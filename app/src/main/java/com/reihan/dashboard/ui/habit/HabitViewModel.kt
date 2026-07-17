package com.reihan.dashboard.ui.habit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.*
import com.reihan.dashboard.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HabitState {
    object Loading : HabitState()
    data class Success(
        val habitsToday: List<HabitLogTodayResponse>,
        val allHabits: List<HabitMini>,
        val summary: HabitSummary? = null,
        val hasMore: Boolean = false,
        val isNextPageLoading: Boolean = false
    ) : HabitState()
    data class Error(val message: String) : HabitState()
}

class HabitViewModel : ViewModel() {

    private val _state = MutableStateFlow<HabitState>(HabitState.Loading)
    val state: StateFlow<HabitState> = _state

    private var currentPage = 1
    private var totalPages = 1
    private val allLoadedHabits = mutableListOf<HabitMini>()
    private var cachedTodayLogs = listOf<HabitLogTodayResponse>()
    private var cachedSummary: HabitSummary? = null

    fun loadHabitData(isRefresh: Boolean = true) {
        if (isRefresh) {
            currentPage = 1
            allLoadedHabits.clear()
        } else {
            if (currentPage > totalPages) return
            val current = _state.value
            if (current is HabitState.Success) {
                _state.value = current.copy(isNextPageLoading = true)
            }
        }

        viewModelScope.launch {
            if (isRefresh) _state.value = HabitState.Loading
            
            try {
                // 1. Fetch today's logs and Dashboard summary on refresh
                if (isRefresh) {
                    val logsRes = RetrofitClient.apiService.getHabitLogsToday()
                    cachedTodayLogs = logsRes.data ?: emptyList()
                    
                    try {
                        val dashRes = RetrofitClient.apiService.getDashboard()
                        cachedSummary = dashRes.data.habits
                    } catch (e: Exception) {
                        // Summary is optional, don't fail the whole screen
                    }
                }

                // 2. Fetch habits with pagination
                val habitsRes = RetrofitClient.apiService.getHabits(page = currentPage, limit = 10)
                val newHabits = habitsRes.data ?: emptyList()
                totalPages = habitsRes.totalPages

                // Deduplicate and add
                val existingIds = allLoadedHabits.map { it.id }.toSet()
                newHabits.forEach { 
                    if (it.id !in existingIds) allLoadedHabits.add(it)
                }

                _state.value = HabitState.Success(
                    habitsToday = cachedTodayLogs,
                    allHabits = allLoadedHabits.toList(),
                    summary = cachedSummary,
                    hasMore = currentPage < totalPages,
                    isNextPageLoading = false
                )
                
                currentPage++
            } catch (e: Exception) {
                if (isRefresh) {
                    _state.value = HabitState.Error(e.localizedMessage ?: "Failed to fetch habits")
                } else {
                    val current = _state.value
                    if (current is HabitState.Success) {
                        _state.value = current.copy(isNextPageLoading = false)
                    }
                }
            }
        }
    }

    fun addHabit(
        name: String,
        frequency: String,
        days: List<String>? = null,
        reminderTime: String? = null,
        reminderEnabled: Boolean? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val freq = frequency.lowercase()
                val finalDays = if (freq == "weekly") days else null
                val isReminderEnabled = reminderEnabled ?: false
                val finalReminderTime = if (isReminderEnabled) {
                    if (reminderTime?.isNotEmpty() == true) reminderTime else null
                } else {
                    "" 
                }

                RetrofitClient.apiService.createHabit(
                    CreateHabitRequest(
                        name = name,
                        frequency = freq,
                        days = finalDays,
                        reminderTime = finalReminderTime,
                        reminderEnabled = isReminderEnabled
                    )
                )
                loadHabitData(isRefresh = true)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to create habit")
            }
        }
    }

    fun editHabit(
        id: String,
        name: String,
        frequency: String,
        days: List<String>? = null,
        reminderTime: String? = null,
        reminderEnabled: Boolean? = null,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val freq = frequency.lowercase()
                val finalDays = if (freq == "weekly") days else null
                val isReminderEnabled = reminderEnabled ?: false
                val finalReminderTime = if (isReminderEnabled) {
                    if (reminderTime?.isNotEmpty() == true) reminderTime else null
                } else {
                    "" 
                }

                RetrofitClient.apiService.updateHabit(
                    id,
                    CreateHabitRequest(
                        name = name,
                        frequency = freq,
                        days = finalDays,
                        reminderTime = finalReminderTime,
                        reminderEnabled = isReminderEnabled
                    )
                )
                loadHabitData(isRefresh = true)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to edit habit")
            }
        }
    }

    fun removeHabit(id: String, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteHabit(id)
                loadHabitData(isRefresh = true)
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to delete habit")
            }
        }
    }

    fun toggleHabitToday(habitId: String, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.toggleHabitLog(ToggleHabitLogRequest(habitId))
                loadHabitData(isRefresh = true)
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to toggle habit")
            }
        }
    }
}
