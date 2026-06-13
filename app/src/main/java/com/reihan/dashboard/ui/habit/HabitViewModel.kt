
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
        val allHabits: List<HabitResponse>
    ) : HabitState()
    data class Error(val message: String) : HabitState()
}

class HabitViewModel : ViewModel() {

    private val _state = MutableStateFlow<HabitState>(HabitState.Loading)
    val state: StateFlow<HabitState> = _state

    fun loadHabitData() {
        viewModelScope.launch {
            _state.value = HabitState.Loading
            try {
                // 1. Fetch today's habit logs/list
                val logsRes = RetrofitClient.apiService.getHabitLogsToday()
                
                // 2. Fetch all registered habits (page 1, limit 100)
                val habitsRes = RetrofitClient.apiService.getHabits(page = 1, limit = 100)

                _state.value = HabitState.Success(
                    habitsToday = logsRes.data ?: emptyList(),
                    allHabits = habitsRes.data ?: emptyList()
                )
            } catch (e: Exception) {
                _state.value = HabitState.Error(e.localizedMessage ?: "Failed to fetch habits")
            }
        }
    }

    fun addHabit(name: String, frequency: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.createHabit(
                    CreateHabitRequest(name, frequency.lowercase())
                )
                loadHabitData()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to create habit")
            }
        }
    }

    fun editHabit(id: String, name: String, frequency: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.updateHabit(
                    id,
                    CreateHabitRequest(name, frequency.lowercase())
                )
                loadHabitData()
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
                loadHabitData()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to delete habit")
            }
        }
    }

    fun toggleHabitToday(habitId: String, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.toggleHabitLog(ToggleHabitLogRequest(habitId))
                loadHabitData() // refresh logs
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to toggle habit")
            }
        }
    }
}
