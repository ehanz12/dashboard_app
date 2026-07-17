package com.reihan.dashboard.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.DashboardData
import com.reihan.dashboard.data.repository.HomeRepository
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

    fun loadDashboardData() {
        viewModelScope.launch {
            _homeState.value = HomeState.Loading
            try {
                val userRes = repository.getMe()
                val dashboardRes = repository.getDashboard()
                
                _homeState.value = HomeState.Success(
                    userName = userRes.data?.name ?: "User",
                    dashboardData = dashboardRes.data
                )
            } catch (e: Exception) {
                _homeState.value = HomeState.Error(e.localizedMessage ?: "Failed to load dashboard data")
            }
        }
    }
}
