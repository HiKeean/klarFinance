package com.klarfinance.app.core.network

import com.klarfinance.app.data.remote.dto.ApiResponseDto
import kotlinx.serialization.json.Json
import retrofit2.Response

/**
 * Extracts the backend's `message` field from a non-2xx response body
 * (as produced by GlobalExceptionHandler), falling back to a generic
 * message when the body is missing or not in the expected shape.
 */
fun Json.extractErrorMessage(response: Response<*>): String {
    val rawBody = response.errorBody()?.string()
    val parsedMessage = rawBody?.let {
        runCatching { decodeFromString(ApiResponseDto.serializer(), it).message }.getOrNull()
    }
    return parsedMessage ?: "Something went wrong (${response.code()})"
}
