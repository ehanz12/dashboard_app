package com.reihan.dashboard.data.repository

import com.reihan.dashboard.data.model.DashboardResponse
import com.reihan.dashboard.data.model.MeResponse
import com.reihan.dashboard.data.network.RetrofitClient

class HomeRepository {
    suspend fun getDashboard(): DashboardResponse {
        return RetrofitClient.apiService.getDashboard()
    }

    suspend fun getMe(): MeResponse {
        return RetrofitClient.apiService.getMe()
    }
}
