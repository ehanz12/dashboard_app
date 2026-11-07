package com.reihan.dashboard.data.repository

import com.reihan.dashboard.data.model.*
import com.reihan.dashboard.data.network.RetrofitClient

class AnalyticsRepository {
    suspend fun getSummary(type: String, month: Int?, year: Int): AnalyticsSummaryResponse {
        return RetrofitClient.apiService.getAnalyticsSummary(type, month, year)
    }

    suspend fun getFinance(type: String, month: Int?, year: Int): FinanceAnalyticsResponse {
        return RetrofitClient.apiService.getFinanceAnalytics(type, month, year)
    }

    suspend fun getCategories(type: String, month: Int?, year: Int): CategoryAnalyticsResponse {
        return RetrofitClient.apiService.getCategoryAnalytics(type, month, year)
    }

    suspend fun getHabits(type: String, month: Int?, year: Int): HabitAnalyticsResponse {
        return RetrofitClient.apiService.getHabitAnalytics(type, month, year)
    }

    suspend fun getTasks(type: String, month: Int?, year: Int): TaskAnalyticsResponse {
        return RetrofitClient.apiService.getTaskAnalytics(type, month, year)
    }

    suspend fun getStreak(): StreakAnalyticsResponse {
        return RetrofitClient.apiService.getStreakAnalytics()
    }
}
