package com.api.klarfinance.config;

import com.api.klarfinance.global.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    private final HmacRequestCachingFilter hmacRequestCachingFilter;
    private final HmacSignatureFilter hmacSignatureFilter;
    private final ObjectMapper objectMapper;
    private final AppConfigProperties appConfigProperties;

    @SuppressWarnings("null")
@Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) 
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/nasabah/auth/login", "/api/v1/nasabah/auth/register",
                                "/api/v1/internal/auth/login", "/api/v1/internal/auth/register",
                                "/api/v1/internal/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/request-otp", "/api/v1/auth/verify-otp", "/api/v1/auth/register",
                                "/api/v1/auth/check-phone", "/api/v1/auth/password-reset-requests").permitAll()
                        // Public reference data (province/regency/district/village) - needed by the
                        // nasabah register flow before the user has a JWT (pre-account, post-OTP).
                        .requestMatchers("/api/v1/dbo/location/**").permitAll()
                        // Halaman generator QRIS (qris-generator/) - HTML/JS statis tanpa login,
                        // konfirmasi user "mockup, simple aja, terbuka tanpa login".
                        .requestMatchers("/api/v1/qris/merchants").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // WS handshake diotentikasi manual (JWT lewat query param) di WebSocketAuthInterceptor,
                        // bukan lewat header Authorization biasa - browser gak bisa attach header custom ke WS handshake.
                        .requestMatchers("/api/v1/ws/**").permitAll()
                        // User#getAuthorities() exposes the role name from the database
                        // directly (for example, "SUPERADMIN"), without Spring's
                        // automatic ROLE_ prefix.
                        .requestMatchers("/api/v1/admin/**").hasAuthority("SUPERADMIN")
                        .requestMatchers("api/v1/nasabah/**").hasAnyAuthority("NASABAH")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler((request, response, denied) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            objectMapper.writeValue(response.getWriter(), ApiResponse.builder()
                                    .success(false)
                                    .statusCode(HttpStatus.FORBIDDEN.value())
                                    .message("Forbidden: you do not have permission to access this resource")
                                    .build());
                        }))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(hmacRequestCachingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(hmacSignatureFilter, HmacRequestCachingFilter.class)
                .addFilterAfter(jwtAuthFilter, HmacSignatureFilter.class);

        return http.build();
    }
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Origin yang di-allow diambil dari app.security.cors-allowed-origins
        // (application.properties / env var APP_SECURITY_CORS_ALLOWED_ORIGINS).
        configuration.setAllowedOriginPatterns(appConfigProperties.getSecurity().getCorsAllowedOrigins());

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With", 
            "Accept",
            "X-Signature", 
            "X-Timestamp" , 
            "X-Client-Type"
        ));
        
        configuration.setAllowCredentials(true); 

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); 
        return source;
    }
}
