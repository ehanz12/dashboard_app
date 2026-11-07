package com.reihan.dashboard.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.DashboardData
import com.reihan.dashboard.data.repository.HomeRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HomeState {
    object Loading : HomeState()
    data class Success(
        val userName: String,
        val dashboardData: DashboardData
    ) : HomeState()
    data class Error(val message: String) : HomeState()
}

class HomeViewModel : ViewModel() {
    private val repository = HomeRepository()

    private val _homeState = MutableStateFlow<HomeState>(HomeState.Loading)
    val homeState: StateFlow<HomeState> = _homeState

    fun loadDashboardData(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            // Only set to Loading if we don't already have data to prevent screen flickering
            if (_homeState.value !is HomeState.Success) {
                _homeState.value = HomeState.Loading
            }
            try {
                // Fetch user info and dashboard data concurrently
                val userDeferred = async { repository.getMe(forceRefresh) }
                val dashboardDeferred = async { repository.getDashboard(forceRefresh) }

                val userRes = userDeferred.await()
                val dashboardRes = dashboardDeferred.await()

                _homeState.value = HomeState.Success(
                    userName = userRes.data?.name ?: "User",
                    dashboardData = dashboardRes.data
                )
            } catch (e: Exception) {
                if (_homeState.value !is HomeState.Success) {
                    _homeState.value = HomeState.Error(e.localizedMessage ?: "Failed to load dashboard data")
                }
            }
        }
    }
}
