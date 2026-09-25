package com.api.klarfinance.auth.service;

import com.api.klarfinance.global.TooManyRequestsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceOtpRateLimitTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private KirimiWhatsappService kirimiWhatsappService;
    @Mock
    private FirebasePhoneTokenVerifier firebasePhoneTokenVerifier;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        authService = new AuthService(redisTemplate, kirimiWhatsappService, firebasePhoneTokenVerifier,
                null, null, null, null, null, null, null, null);
    }

    @Test
    void requestOtp_firstRequest_sendsOtp() {
        when(valueOps.setIfAbsent(eq("otp:cooldown:628123456789"), eq("1"), any(Duration.class))).thenReturn(true);
        when(valueOps.increment("otp:count:628123456789")).thenReturn(1L);

        assertThat(authService.requestOtp("08123456789", null)).isEqualTo("WHATSAPP");

        verify(redisTemplate).expire("otp:count:628123456789", Duration.ofHours(1));
        verify(kirimiWhatsappService).sendOtpMessage(eq("628123456789"), anyString());
    }

    @Test
    void requestOtp_withinCooldown_rejectedWithoutSending() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        when(redisTemplate.getExpire("otp:cooldown:628123456789")).thenReturn(42L);

        assertThatThrownBy(() -> authService.requestOtp("628123456789", null))
                .isInstanceOf(TooManyRequestsException.class)
                .hasMessageContaining("42 detik");
        verifyNoInteractions(kirimiWhatsappService);
    }

    @Test
    void requestOtp_overHourlyLimit_rejectedWithoutSending() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(valueOps.increment("otp:count:628123456789")).thenReturn(6L);

        assertThatThrownBy(() -> authService.requestOtp("628123456789", null))
                .isInstanceOf(TooManyRequestsException.class);
        verifyNoInteractions(kirimiWhatsappService);
    }

    @Test
    void requestOtp_overIpLimit_rejectedWithoutSending() {
        when(valueOps.increment("otp:ip:1.2.3.4")).thenReturn(31L);

        assertThatThrownBy(() -> authService.requestOtp("628123456789", "1.2.3.4"))
                .isInstanceOf(TooManyRequestsException.class);
        verifyNoInteractions(kirimiWhatsappService);
    }

    @Test
    void requestOtp_whatsappFails_fallsBackToFirebaseSmsAndDropsOtp() {
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(valueOps.increment("otp:count:628123456789")).thenReturn(1L);
        doThrow(new IllegalStateException("Failed to send OTP WhatsApp"))
                .when(kirimiWhatsappService).sendOtpMessage(anyString(), anyString());

        assertThat(authService.requestOtp("628123456789", null)).isEqualTo("FIREBASE_SMS");
        verify(redisTemplate).delete("otp:628123456789");
    }

    @Test
    void verifyOtp_fifthWrongAttempt_invalidatesOtp() {
        when(valueOps.get("otp:628123456789")).thenReturn("111111");
        when(valueOps.increment("otp:attempt:628123456789")).thenReturn(5L);

        assertThatThrownBy(() -> authService.verifyOtp("628123456789", "222222"))
                .hasMessageContaining("minta OTP baru");
        verify(redisTemplate).delete("otp:628123456789");
    }

    @Test
    void verifyOtp_wrongAttemptBelowLimit_keepsOtp() {
        when(valueOps.get("otp:628123456789")).thenReturn("111111");
        when(valueOps.increment("otp:attempt:628123456789")).thenReturn(2L);

        assertThatThrownBy(() -> authService.verifyOtp("628123456789", "222222"))
                .hasMessage("Invalid OTP");
        verify(redisTemplate, never()).delete("otp:628123456789");
    }

    @Test
    void verifyFirebasePhone_matchingPhone_marksVerified() {
        when(firebasePhoneTokenVerifier.verifyAndGetPhone("tok")).thenReturn("+628123456789");

        authService.verifyFirebasePhone("08123456789", "tok");

        verify(valueOps).set(eq("otp:verified:628123456789"), eq("1"), any(Duration.class));
    }

    @Test
    void verifyFirebasePhone_differentPhone_rejected() {
        when(firebasePhoneTokenVerifier.verifyAndGetPhone("tok")).thenReturn("+628999999999");

        assertThatThrownBy(() -> authService.verifyFirebasePhone("628123456789", "tok"))
                .isInstanceOf(IllegalArgumentException.class);
        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }
}
