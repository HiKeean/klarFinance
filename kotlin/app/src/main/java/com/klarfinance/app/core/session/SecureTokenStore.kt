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
        try {
            createPrefs(context, masterKey)
        } catch (e: Exception) {
            // The keystore-backed master key can become unusable while the encrypted prefs
            // file on disk survives (lock screen change, device restore, reinstall without
            // clearing data) - decryption then fails with AEADBadTagException and crashes
            // the app on every launch. The file only ever holds a refresh token, so it's safe
            // to drop and start clean; the user just re-authenticates.
            context.deleteSharedPreferences(PREFS_NAME)
            createPrefs(context, masterKey)
        }
    }

    private fun createPrefs(context: Context, masterKey: MasterKey) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveRefreshToken(token: String) {
        prefs.edit().putString(KEY_REFRESH_TOKEN, token).apply()
    }

    fun getRefreshToken(): String? = prefs.getString(KEY_REFRESH_TOKEN, null)

    fun hasRefreshToken(): Boolean = getRefreshToken() != null

    fun clear() {
        prefs.edit().remove(KEY_REFRESH_TOKEN).apply()
    }

    companion object {
        private const val PREFS_NAME = "klarfinance_secure_prefs"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
