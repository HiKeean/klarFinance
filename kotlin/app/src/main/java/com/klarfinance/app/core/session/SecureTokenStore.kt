package com.klarfinance.app.core.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists the refresh token ONLY when the user opts into fingerprint (see
 * kotlin-nasabah-app knowledge, "Sidik Jari" security-checklist item) - this is the one piece
 * of session state that survives an app restart, and only for devices that unlocked it with a
 * successful [com.klarfinance.app.core.security.BiometricAuthHelper] prompt. Everything else
 * ([SessionManager]) stays in-memory only.
 */
@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "klarfinance_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun saveRefreshToken(token: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun hasRefreshToken(): Boolean = getRefreshToken() != null

    fun clear() {
        prefs.edit().remove(KEY_REFRESH_TOKEN).apply()
    }

    companion object {
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
