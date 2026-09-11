package com.api.klarfinance.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class HmacRequestCachingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Multipart bodies must be left untouched: draining request.getInputStream()
        // here consumes the servlet container's raw stream before Spring's multipart
        // resolver can parse the parts, breaking every file-upload endpoint. HMAC
        // signing was never meaningful over binary multipart bytes anyway (see
        // HmacSignatureFilter's string-to-sign, which assumes a UTF-8 JSON body).
        String contentType = request.getContentType();
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Ganti menggunakan Custom Wrapper kita
        CachedBodyHttpServletRequest wrappedRequest = new CachedBodyHttpServletRequest(request);
        filterChain.doFilter(wrappedRequest, response);
    }
}