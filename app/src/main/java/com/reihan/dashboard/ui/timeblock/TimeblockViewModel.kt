package com.reihan.dashboard.ui.timeblock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.*
import com.reihan.dashboard.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

sealed class TimeblockState {
    object Loading : TimeblockState()
    data class Success(
        val timeblocks: List<TimeblockResponse>,
        val selectedDay: Int  // 1=Monday ... 7=Sunday
    ) : TimeblockState()
    data class Error(val message: String) : TimeblockState()
}

class TimeblockViewModel : ViewModel() {

    private val _state = MutableStateFlow<TimeblockState>(TimeblockState.Loading)
    val state: StateFlow<TimeblockState> = _state

    private var currentDay: Int = run {
        val cal = Calendar.getInstance()
        val d = cal.get(Calendar.DAY_OF_WEEK)
        // Calendar.DAY_OF_WEEK: 1=Sun, 2=Mon ... 7=Sat
        // We want: 1=Mon ... 7=Sun
        if (d == Calendar.SUNDAY) 7 else d - 1
    }

    fun loadTimeblocks(dayOfWeek: Int? = null) {
        val day = dayOfWeek ?: currentDay
        currentDay = day
        viewModelScope.launch {
            _state.value = TimeblockState.Loading
            try {
                val res = RetrofitClient.apiService.getTimeblocks(dayOfWeek = day)
                val sorted = (res.data ?: emptyList()).sortedBy { it.startTime }
                _state.value = TimeblockState.Success(timeblocks = sorted, selectedDay = day)
            } catch (e: Exception) {
                _state.value = TimeblockState.Error(e.localizedMessage ?: "Failed to load schedule")
            }
        }
    }

    fun addTimeblock(
        activityName: String,
        startTime: String,
        endTime: String,
        colorCode: String,
        dayOfWeek: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.createTimeblock(
                    CreateTimeblockRequest(
                        activityName = activityName,
                        startTime = startTime,
                        endTime = endTime,
                        colorCode = colorCode,
                        dayOfWeek = dayOfWeek
                    )
                )
                loadTimeblocks(dayOfWeek)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to add schedule block")
            }
        }
    }

    fun editTimeblock(
        id: String,
        activityName: String,
        startTime: String,
        endTime: String,
        colorCode: String,
        dayOfWeek: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.updateTimeblock(
                    id,
                    CreateTimeblockRequest(
                        activityName = activityName,
                        startTime = startTime,
                        endTime = endTime,
                        colorCode = colorCode,
                        dayOfWeek = dayOfWeek
                    )
                )
                loadTimeblocks(dayOfWeek)
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to edit schedule block")
            }
        }
    }

    fun deleteTimeblock(id: String, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteTimeblock(id)
                loadTimeblocks(currentDay)
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to delete schedule block")
            }
        }
    }
}
