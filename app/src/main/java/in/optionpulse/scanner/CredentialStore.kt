package com.optionpulse.scanner

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class CredentialStore(context: Context) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "optionpulse_secure",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var upstoxToken: String
        get() = prefs.getString("upstox_token", "") ?: ""
        set(value) = prefs.edit().putString("upstox_token", value.trim()).apply()
    var telegramToken: String
        get() = prefs.getString("telegram_token", "") ?: ""
        set(value) = prefs.edit().putString("telegram_token", value.trim()).apply()
    var telegramChatId: String
        get() = prefs.getString("telegram_chat_id", "") ?: ""
        set(value) = prefs.edit().putString("telegram_chat_id", value.trim()).apply()

    fun configured() = upstoxToken.isNotBlank() && telegramToken.isNotBlank() && telegramChatId.isNotBlank()
}
