package com.reihan.dashboard.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.HabitLogTodayResponse
import com.reihan.dashboard.data.model.JournalResponse
import com.reihan.dashboard.data.model.TaskResponse
import com.reihan.dashboard.data.model.TransactionResponse
import com.reihan.dashboard.data.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

sealed class HomeState {
    object Loading : HomeState()
    data class Success(
        val userName: String,
        val totalIncome: Double,
        val totalExpense: Double,
        val balance: Double,
        val recentTransactions: List<TransactionResponse>,
        // Today's summary widgets
        val todayTasks: List<TaskResponse>,
        val todayCompletedTasks: Int,
        val todayJournal: JournalResponse?,
        val todayHabits: List<HabitLogTodayResponse>,
        val todayCompletedHabits: Int
    ) : HomeState()
    data class Error(val message: String) : HomeState()
}

class HomeViewModel : ViewModel() {

    private val _homeState = MutableStateFlow<HomeState>(HomeState.Loading)
    val homeState: StateFlow<HomeState> = _homeState

    fun loadDashboardData() {
        viewModelScope.launch {
            _homeState.value = HomeState.Loading
            try {
                // Move network calls and data processing to IO/Default context
                val result = withContext(Dispatchers.Default) {
                    val userProfileDeferred = async { RetrofitClient.apiService.getMe() }
                    val transactionsResDeferred = async { RetrofitClient.apiService.getTransactions(page = 1, limit = 100) }
                    val tasksResDeferred = async { RetrofitClient.apiService.getTasks() }
                    val journalResDeferred = async { RetrofitClient.apiService.getJournals() }
                    val habitsResDeferred = async { RetrofitClient.apiService.getHabitLogsToday() }

                    val userProfile = userProfileDeferred.await().data
                    val transactionsRes = transactionsResDeferred.await()
                    val tasksRes = tasksResDeferred.await()
                    val journalRes = journalResDeferred.await()
                    val habitsRes = habitsResDeferred.await()

                    val userName = userProfile?.name ?: "User"
                    val transactionsList = transactionsRes.data

                    var totalIncome = 0.0
                    var totalExpense = 0.0
                    for (t in transactionsList) {
                        if (t.type.lowercase() == "income") totalIncome += t.amount
                        else if (t.type.lowercase() == "expense") totalExpense += t.amount
                    }

                    val allTasks = tasksRes.data ?: emptyList()
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val todayTasks = allTasks.filter { task ->
                        task.dueDate == null || task.dueDate.startsWith(todayStr)
                    }.take(5)
                    val completedTasks = allTasks.count { it.isCompleted }

                    val allJournals = journalRes.data ?: emptyList()
                    val todayJournal = allJournals.find { it.entryDate.startsWith(todayStr) }

                    val todayHabits = habitsRes.data ?: emptyList()
                    val completedHabits = todayHabits.count { it.completed }

                    HomeState.Success(
                        userName = userName,
                        totalIncome = totalIncome,
                        totalExpense = totalExpense,
                        balance = totalIncome - totalExpense,
                        recentTransactions = transactionsList.take(5),
                        todayTasks = todayTasks,
                        todayCompletedTasks = completedTasks,
                        todayJournal = todayJournal,
                        todayHabits = todayHabits,
                        todayCompletedHabits = completedHabits
                    )
                }
                _homeState.value = result
            } catch (e: Exception) {
                _homeState.value = HomeState.Error(e.localizedMessage ?: "Failed to load dashboard data")
            }
        }
    }
}
