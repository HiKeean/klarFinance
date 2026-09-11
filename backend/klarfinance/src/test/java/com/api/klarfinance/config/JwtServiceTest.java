package com.api.klarfinance.config;

import com.api.klarfinance.auth.model.Role;
import com.api.klarfinance.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        String secret = Base64.getEncoder().encodeToString("this-is-a-very-long-test-secret-key-for-jwt-hs256".getBytes());
        ReflectionTestUtils.setField(jwtService, "secretKey", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600_000L);
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 7200_000L);
    }

    private User user(String identity) {
        return User.builder().id(1).identity(identity).role(Role.builder().name("NASABAH").build()).build();
    }

    @Test
    void generateToken_extractUsername_roundTrips() {
        User user = user("628111111111");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("628111111111");
    }

    @Test
    void isTokenValid_trueForMatchingUserAndUnexpiredToken() {
        User user = user("628111111111");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_falseForDifferentUser() {
        User user = user("628111111111");
        User otherUser = user("628222222222");
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_falseForExpiredToken() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L); // 1ms expiration
        User user = user("628111111111");
        String token = jwtService.generateToken(user);

        Thread.sleep(50);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, user))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void generateRefreshToken_isValidForSameUser() {
        User user = user("628111111111");

        String refreshToken = jwtService.generateRefreshToken(user);

        assertThat(jwtService.isTokenValid(refreshToken, user)).isTrue();
        assertThat(jwtService.extractUsername(refreshToken)).isEqualTo("628111111111");
    }

    @Test
    void generateToken_includesRolesClaim() {
        User user = user("628111111111");

        String token = jwtService.generateToken(user);

        @SuppressWarnings("unchecked")
        java.util.List<String> roles = jwtService.extractClaim(token, claims -> (java.util.List<String>) claims.get("roles"));
        assertThat(roles).containsExactly("NASABAH");
    }
}
