package com.klarfinance.app.data.remote

import com.klarfinance.app.data.remote.dto.ApiResponseDto
import com.klarfinance.app.data.remote.dto.RequestOtpRequestDto
import com.klarfinance.app.data.remote.dto.VerifyOtpRequestDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/v1/auth/request-otp")
    suspend fun requestOtp(@Body body: RequestOtpRequestDto): Response<ApiResponseDto>

    @POST("api/v1/auth/verify-otp")
    suspend fun verifyOtp(@Body body: VerifyOtpRequestDto): Response<ApiResponseDto>
}
