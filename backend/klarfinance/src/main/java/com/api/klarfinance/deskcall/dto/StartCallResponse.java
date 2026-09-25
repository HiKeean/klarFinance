package com.api.klarfinance.deskcall.dto;

/** Hasil tombol Call di NPL Report (webadmin). pushSent=false artinya panggilan sudah dibuat di
 * deskcall tapi FCM gagal dikirim - deskcall akan menandainya tidak diangkat (NSTD) setelah batas dering. */
public record StartCallResponse(
        String callId,
        Integer loanId,
        String customerName,
        Integer ringTimeoutSeconds,
        boolean pushSent) {
}
