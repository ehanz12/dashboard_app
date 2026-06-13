package com.reihan.dashboard.data.network

import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        // Append token if available in SessionManager
        val hasAuthHeader = SessionManager.token?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
            true
        } ?: false

        val response = chain.proceed(requestBuilder.build())

        // Check if token has expired or is unauthorized (HTTP 401)
        if (response.code == 401 && hasAuthHeader) {
            SessionManager.triggerSessionExpired()
        }

        return response
    }
}
