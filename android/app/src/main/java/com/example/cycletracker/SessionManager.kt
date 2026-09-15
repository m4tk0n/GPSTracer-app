package com.example.cycletracker

import android.content.Context

/**
 * Jednoduche ulozeni prihlasovaciho tokenu. Pro produkci by bylo lepsi
 * pouzit EncryptedSharedPreferences (androidx.security:security-crypto),
 * ale pro osobni pouziti staci tohle.
 */
object SessionManager {
    private const val PREFS = "cycletracker_session"
    private const val KEY_TOKEN = "api_token"
    private const val KEY_USERNAME = "username"

    fun saveSession(context: Context, token: String, username: String) {
        prefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    fun getToken(context: Context): String? = prefs(context).getString(KEY_TOKEN, null)

    fun getUsername(context: Context): String? = prefs(context).getString(KEY_USERNAME, null)

    fun isLoggedIn(context: Context): Boolean = !getToken(context).isNullOrEmpty()

    fun logout(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
