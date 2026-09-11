package com.api.klarfinance.config;

import com.api.klarfinance.auth.repository.ApiParameterRepository;
import com.api.klarfinance.dbo.model.ApiParameter;
import com.api.klarfinance.global.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class HmacSignatureFilter extends OncePerRequestFilter {

    private final HmacService hmacService;
    private final ObjectMapper objectMapper;
    private final ApiParameterRepository apiParameterRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (
                path.startsWith("/api/v1/nasabah/auth")
                        || path.startsWith("/api/v1/internal/auth")
                        || path.startsWith("/api/v1/auth")
                        || path.startsWith("/swagger-ui")
                        || path.startsWith("/v3/api-docs")
                        // Handshake WebSocket dari browser gak bisa kirim custom header (X-Signature/dst) -
                        // otentikasi WS pakai JWT lewat query param sendiri di WebSocketAuthInterceptor.
                        || path.startsWith("/api/v1/ws")
                        // Halaman generator QRIS (qris-generator/) - HTML/JS statis polos, gak bisa
                        // hitung HMAC signature sama sekali.
                        || path.startsWith("/api/v1/qris/merchants")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!(request instanceof CachedBodyHttpServletRequest wrappedRequest)) {
            log.warn("Request tidak dibungkus oleh CachedBodyHttpServletRequest. Path: {}", request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        String clientType = request.getHeader("X-Client-Type");
        String clientSignature = request.getHeader("X-Signature");
        String timestamp = request.getHeader("X-Timestamp");

        if (clientSignature == null || timestamp == null || clientType == null) {
            log.error("Missing HMAC headers or Client-Type: {}", request.getRequestURI());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Missing Required Headers (X-Signature, X-Timestamp, or X-Client-Type)");
            return;
        }

        Optional<ApiParameter> apiParameterOpt = apiParameterRepository.findByClientType(clientType);
        if (apiParameterOpt.isEmpty()) {
            log.error("API Key cannot be found for client type: {}", clientType);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid Client Type or API Key cannot be found");
            return;
        }
        String apiKey = apiParameterOpt.get().getApiKey();

        long currentMilli = Instant.now().toEpochMilli();
        long clientTime;
        try {
            clientTime = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            log.error("Invalid Timestamp format: {}", timestamp);
            sendErrorResponse(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid Timestamp format");
            return;
        }

        if (Math.abs(currentMilli - clientTime) > 300000) {
            log.error("Replay Attack detection. Client Time: {}, Server Time: {}", clientTime, currentMilli);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Request expired (Replay Attack detection)");
            return;
        }

        byte[] contentAsByteArray = wrappedRequest.getCachedBody();
        String requestBody = new String(contentAsByteArray, StandardCharsets.UTF_8);

        String stringToSign = request.getMethod() + request.getRequestURI() + timestamp + requestBody + apiKey;

        // log.info("========== DEBUG HMAC START ==========");
        // log.info("1. Method        : [{}]", request.getMethod());
        // log.info("2. URI           : [{}]", request.getRequestURI());
        // log.info("3. Timestamp     : [{}]", timestamp);
        // log.info("4. Body Request  : [{}]", requestBody);
        // log.info("5. Client Type   : [{}]", clientType);
        // log.info("6. API Key (DB)  : [{}]", apiKey);
        // log.info("7. STRING TO SIGN: [{}]", stringToSign);

        String serverSignature = hmacService.calculateSignature(stringToSign);

        // log.info("8. Client Signature (Postman): {}", clientSignature);
        // log.info("9. Server Signature (Java)   : {}", serverSignature);
        // log.info("=========== DEBUG HMAC END ===========");

        if (!serverSignature.equals(clientSignature)) {
            log.error("Invalid HMAC Signature");
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid HMAC Signature");
            return;
        }

        filterChain.doFilter(wrappedRequest, response);
    }

    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> errorResponse = ApiResponse.error(message);

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);

        response.getWriter().write(jsonResponse);
    }
}