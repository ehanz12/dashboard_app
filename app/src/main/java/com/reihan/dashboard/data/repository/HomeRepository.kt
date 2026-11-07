package com.reihan.dashboard.data.repository

import com.reihan.dashboard.data.model.DashboardResponse
import com.reihan.dashboard.data.model.MeResponse
import com.reihan.dashboard.data.network.RetrofitClient

class HomeRepository {
    companion object {
        private var cachedMe: MeResponse? = null
        private var cachedDashboard: DashboardResponse? = null
        private var lastFetchTime = 0L
        private const val CACHE_EXPIRY_MS = 60_000L // 1 minute in-memory cache

        fun invalidateCache() {
            lastFetchTime = 0L
            cachedDashboard = null
        }
    }

    suspend fun getDashboard(forceRefresh: Boolean = false): DashboardResponse {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedDashboard != null && (now - lastFetchTime) < CACHE_EXPIRY_MS) {
            return cachedDashboard!!
        }
        val response = RetrofitClient.apiService.getDashboard()
        cachedDashboard = response
        lastFetchTime = now
        return response
    }

    suspend fun getMe(forceRefresh: Boolean = false): MeResponse {
        if (!forceRefresh && cachedMe != null) {
            return cachedMe!!
        }
        val response = RetrofitClient.apiService.getMe()
        cachedMe = response
        return response
    }
}
