package com.reihan.dashboard.data.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object SessionManager {
    private const val PREF_NAME = "dashboard_prefs"
    private const val KEY_TOKEN = "auth_token"
    private var prefs: SharedPreferences? = null

    private val _sessionExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val sessionExpiredEvent: SharedFlow<Unit> = _sessionExpiredEvent.asSharedFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        }
    }

    var token: String?
        get() = prefs?.getString(KEY_TOKEN, null)
        private set(value) {
            prefs?.edit()?.putString(KEY_TOKEN, value)?.apply()
        }

    fun saveToken(newToken: String) {
        token = newToken
    }

    fun clearToken() {
        token = null
    }

    fun hasToken(): Boolean {
        val currentToken = token
        if (currentToken.isNullOrBlank()) {
            return false
        }
        if (isTokenExpired(currentToken)) {
            clearToken()
            return false
        }
        return true
    }

    fun triggerSessionExpired() {
        clearToken()
        _sessionExpiredEvent.tryEmit(Unit)
    }

    private fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size < 2) return true
            val payloadEncoded = parts[1]
            val payloadDecoded = Base64.decode(payloadEncoded, Base64.DEFAULT)
            val payloadString = String(payloadDecoded, StandardCharsets.UTF_8)
            val jsonObject = JSONObject(payloadString)
            if (jsonObject.has("exp")) {
                val exp = jsonObject.getLong("exp")
                val currentTimeSeconds = System.currentTimeMillis() / 1000
                currentTimeSeconds >= (exp - 5) // buffer of 5 seconds
            } else {
                false
            }
        } catch (e: Exception) {
            true
        }
    }
}
