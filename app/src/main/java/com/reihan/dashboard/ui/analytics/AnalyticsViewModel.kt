package com.reihan.dashboard.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.*
import com.reihan.dashboard.data.repository.AnalyticsRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

sealed class AnalyticsState {
    object Loading : AnalyticsState()
    data class Success(
        val summary: AnalyticsSummaryData,
        val finance: List<FinanceAnalyticsItem>,
        val categories: List<CategoryAnalyticsItem>,
        val habits: List<HabitAnalyticsItem>,
        val tasks: List<TaskAnalyticsItem>,
        val streak: StreakAnalyticsData
    ) : AnalyticsState()
    object Empty : AnalyticsState()
    data class Error(val message: String) : AnalyticsState()
}

class AnalyticsViewModel : ViewModel() {
    private val repository = AnalyticsRepository()

    private val _state = MutableStateFlow<AnalyticsState>(AnalyticsState.Loading)
    val state: StateFlow<AnalyticsState> = _state

    private val _filterType = MutableStateFlow("monthly")
    val filterType: StateFlow<String> = _filterType

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear

    init {
        loadAnalytics()
    }

    fun setFilterType(type: String) {
        _filterType.value = type
        loadAnalytics()
    }

    fun setMonth(month: Int) {
        _selectedMonth.value = month
        loadAnalytics()
    }

    fun setYear(year: Int) {
        _selectedYear.value = year
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _state.value = AnalyticsState.Loading
            try {
                val type = _filterType.value
                val month = if (type == "monthly") _selectedMonth.value else null
                val year = _selectedYear.value

                val summaryDeferred = async { repository.getSummary(type, month, year) }
                val financeDeferred = async { repository.getFinance(type, month, year) }
                val categoriesDeferred = async { repository.getCategories(type, month, year) }
                val habitsDeferred = async { repository.getHabits(type, month, year) }
                val tasksDeferred = async { repository.getTasks(type, month, year) }
                val streakDeferred = async { repository.getStreak() }

                val summary = summaryDeferred.await().data
                val finance = financeDeferred.await().data
                val categories = categoriesDeferred.await().data
                val habits = habitsDeferred.await().data
                val tasks = tasksDeferred.await().data
                val streak = streakDeferred.await().data

                if (finance.isEmpty() && categories.isEmpty() && habits.isEmpty() && tasks.isEmpty()) {
                    _state.value = AnalyticsState.Empty
                } else {
                    _state.value = AnalyticsState.Success(
                        summary = summary,
                        finance = finance,
                        categories = categories,
                        habits = habits,
                        tasks = tasks,
                        streak = streak
                    )
                }
            } catch (e: Exception) {
                _state.value = AnalyticsState.Error(e.localizedMessage ?: "Unknown Error")
            }
        }
    }
}
