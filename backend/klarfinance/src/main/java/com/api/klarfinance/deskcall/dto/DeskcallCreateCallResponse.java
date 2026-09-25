package com.api.klarfinance.deskcall.dto;

/** Response POST /api/v1/calls dari deskcall (field yang dipakai saja; sisanya diabaikan). */
public record DeskcallCreateCallResponse(
        String callId,
        String status,
        String roomName,
        String livekitUrl,
        String customerToken,
        Integer ringTimeoutSeconds) {
}
