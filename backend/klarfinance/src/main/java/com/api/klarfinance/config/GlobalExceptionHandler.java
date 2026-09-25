package com.api.klarfinance.config;

import io.jsonwebtoken.security.SignatureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.api.klarfinance.global.ApiResponse;
import com.api.klarfinance.global.TooManyRequestsException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> illegalArgument(IllegalArgumentException e){
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiResponse<Object>> optimisticLockingFailure(OptimisticLockingFailureException e){
        return build(HttpStatus.CONFLICT, "Pembayaran sedang diproses di request lain, silakan coba lagi");
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiResponse<Object>> tooManyRequests(TooManyRequestsException e){
        return build(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> runTimeException(RuntimeException e){
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(HttpClientErrorException.Unauthorized.class)
    public ResponseEntity<ApiResponse<Object>> unauthorized(HttpClientErrorException.Unauthorized e){
        return build(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> badCredentials(BadCredentialsException e){
        return build(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> forbidden(AccessDeniedException e) {
        return build(HttpStatus.FORBIDDEN, "Forbidden: you do not have permission to access this resource");
    }

    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<ApiResponse<Object>> signatureException(SignatureException e){
        return build(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    private ResponseEntity<ApiResponse<Object>> build(HttpStatus status, String message) {
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .statusCode(status.value())
                .message(message)
                .build();
        return ResponseEntity.status(status).body(response);
    }
}
