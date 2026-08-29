package com.klarfinance.app.data.repository

import com.klarfinance.app.core.network.extractErrorMessage
import com.klarfinance.app.data.remote.AuthApi
import com.klarfinance.app.data.remote.dto.RequestOtpRequestDto
import com.klarfinance.app.data.remote.dto.VerifyOtpRequestDto
import com.klarfinance.app.domain.repository.AuthRepository
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val json: Json,
) : AuthRepository {

    override suspend fun requestOtp(phone: String): Result<Unit> = runCatching {
        val response = api.requestOtp(RequestOtpRequestDto(phone))
        if (!response.isSuccessful) {
            throw IllegalStateException(json.extractErrorMessage(response))
        }
    }.recoverNetworkFailure()

    override suspend fun verifyOtp(phone: String, otp: String): Result<Unit> = runCatching {
        val response = api.verifyOtp(VerifyOtpRequestDto(phone, otp))
        if (!response.isSuccessful) {
            throw IllegalStateException(json.extractErrorMessage(response))
        }
    }.recoverNetworkFailure()

    private fun Result<Unit>.recoverNetworkFailure(): Result<Unit> = recoverCatching { throwable ->
        if (throwable is IOException) {
            throw IllegalStateException("Unable to reach KlarFinance. Check your connection and try again.")
        }
        throw throwable
    }
}
