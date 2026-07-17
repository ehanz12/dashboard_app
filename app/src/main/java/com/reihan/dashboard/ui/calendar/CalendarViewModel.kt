package com.reihan.dashboard.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.*
import com.reihan.dashboard.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class CalendarState {
    object Loading : CalendarState()
    data class Success(
        val tasks: List<TaskResponse>,
        val habits: List<HabitLogTodayResponse>,
        val timeblocks: List<TimeblockResponse>
    ) : CalendarState()
    data class Error(val message: String) : CalendarState()
}

class CalendarViewModel : ViewModel() {
    private val _state = MutableStateFlow<CalendarState>(CalendarState.Loading)
    val state: StateFlow<CalendarState> = _state

    private val _selectedDate = MutableStateFlow(Calendar.getInstance())
    val selectedDate: StateFlow<Calendar> = _selectedDate

    init {
        loadData()
    }

    fun selectDate(date: Calendar) {
        _selectedDate.value = date
        loadData()
    }

    fun loadData() {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(_selectedDate.value.time)
        viewModelScope.launch {
            _state.value = CalendarState.Loading
            try {
                // Fetch tasks (all for now)
                val tasksRes = RetrofitClient.apiService.getTasks()
                
                // Fetch schedule for specific date
                val scheduleRes = RetrofitClient.apiService.getSchedule(dateStr)
                val schedule = scheduleRes.data

                _state.value = CalendarState.Success(
                    tasks = tasksRes.data ?: emptyList(),
                    habits = schedule?.habits ?: emptyList(),
                    timeblocks = schedule?.timeblocks ?: emptyList()
                )
            } catch (e: Exception) {
                _state.value = CalendarState.Error(e.localizedMessage ?: "Failed to load data")
            }
        }
    }

    fun addTimeblock(
        activityName: String,
        startTime: String,
        endTime: String,
        dayOfWeek: Int?,
        date: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Ensure only one of dayOfWeek or date is sent
                val request = CreateTimeblockRequest(
                    activityName = activityName,
                    startTime = startTime,
                    endTime = endTime,
                    dayOfWeek = if (date != null) null else dayOfWeek,
                    date = date
                )
                RetrofitClient.apiService.createTimeblock(request)
                loadData()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to create schedule")
            }
        }
    }

    fun updateTimeblock(
        id: String,
        activityName: String,
        startTime: String,
        endTime: String,
        dayOfWeek: Int?,
        date: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Ensure only one of dayOfWeek or date is sent
                val request = CreateTimeblockRequest(
                    activityName = activityName,
                    startTime = startTime,
                    endTime = endTime,
                    dayOfWeek = if (date != null) null else dayOfWeek,
                    date = date
                )
                RetrofitClient.apiService.updateTimeblock(id, request)
                loadData()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to update schedule")
            }
        }
    }

    fun deleteTimeblock(id: String, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteTimeblock(id)
                loadData()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to delete schedule")
            }
        }
    }
}
