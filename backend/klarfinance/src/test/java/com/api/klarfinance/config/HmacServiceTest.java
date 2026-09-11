package com.api.klarfinance.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class HmacServiceTest {

    private HmacService hmacService;

    @BeforeEach
    void setUp() {
        hmacService = new HmacService();
        ReflectionTestUtils.setField(hmacService, "hmacSecret", "test-secret-key");
    }

    @Test
    void calculateSignature_isDeterministicForSameInput() {
        String signature1 = hmacService.calculateSignature("payload-data");
        String signature2 = hmacService.calculateSignature("payload-data");

        assertThat(signature1).isEqualTo(signature2);
    }

    @Test
    void calculateSignature_differsForDifferentInput() {
        String signature1 = hmacService.calculateSignature("payload-a");
        String signature2 = hmacService.calculateSignature("payload-b");

        assertThat(signature1).isNotEqualTo(signature2);
    }

    @Test
    void calculateSignature_isBase64Encoded() {
        String signature = hmacService.calculateSignature("payload-data");

        assertThat(signature).matches("^[A-Za-z0-9+/]+=*$");
        // HmacSHA256 produces 32 bytes -> 44 base64 chars (with padding)
        assertThat(java.util.Base64.getDecoder().decode(signature)).hasSize(32);
    }

    @Test
    void calculateSignature_differsForDifferentSecret() {
        String signature1 = hmacService.calculateSignature("payload-data");

        HmacService other = new HmacService();
        ReflectionTestUtils.setField(other, "hmacSecret", "different-secret-key");
        String signature2 = other.calculateSignature("payload-data");

        assertThat(signature1).isNotEqualTo(signature2);
    }
}
