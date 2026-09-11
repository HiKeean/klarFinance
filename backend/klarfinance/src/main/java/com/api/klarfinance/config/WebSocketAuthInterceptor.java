package com.api.klarfinance.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import com.api.klarfinance.token.TokenRepository;
import com.api.klarfinance.token.TokenType;

import java.util.List;
import java.util.Map;

/**
 * Browser WebSocket handshake gak bisa attach header custom (Authorization/HMAC) - JWT dikirim
 * lewat query param ?token=... di URL handshake, divalidasi manual di sini pakai JwtService yang
 * sama dengan filter HTTP biasa (JwtAuthenticationFilter).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements HandshakeInterceptor {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest)) return false;

        List<String> tokenParam = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .get("token");
        String token = tokenParam == null || tokenParam.isEmpty() ? null : tokenParam.get(0);
        if (token == null) {
            log.warn("WS handshake ditolak: token query param gak ada");
            return false;
        }

        try {
            String identity = jwtService.extractUsername(token);
            if (identity == null) return false;

            UserDetails userDetails = userDetailsService.loadUserByUsername(identity);
            boolean tokenValid = tokenRepository.findByToken(token)
                    .map(t -> t.getTokenType() == TokenType.ACCESS && !t.isExpired() && !t.isRevoked())
                    .orElse(false);

            if (!jwtService.isTokenValid(token, userDetails) || !tokenValid) {
                log.warn("WS handshake ditolak: token invalid buat identity {}", identity);
                return false;
            }

            attributes.put("identity", identity);
            return true;
        } catch (Exception e) {
            log.warn("WS handshake ditolak: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
    }
}
