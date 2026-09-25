package com.api.klarfinance.global;

/** Dilempar kalau request dibatasi (rate limit) - GlobalExceptionHandler memetakan ke HTTP 429. */
public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message) {
        super(message);
    }
}
