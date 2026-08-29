package com.klarfinance.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RequestOtpRequestDto(
    val phone: String,
)

@Serializable
data class VerifyOtpRequestDto(
    val phone: String,
    val otp: String,
)

/**
 * Mirrors the backend's ApiResponse<T> envelope. `data` is omitted whenever
 * both auth endpoints succeed or fail, so it is intentionally left out here.
 */
@Serializable
data class ApiResponseDto(
    val success: Boolean,
    val statusCode: Int? = null,
    val message: String? = null,
)
