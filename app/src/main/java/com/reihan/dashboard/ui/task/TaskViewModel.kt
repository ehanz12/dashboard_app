package com.reihan.dashboard.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.*
import com.reihan.dashboard.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class TaskState {
    object Loading : TaskState()
    data class Success(
        val q1: List<TaskResponse>, // Urgent + Important
        val q2: List<TaskResponse>, // Not Urgent + Important
        val q3: List<TaskResponse>, // Urgent + Not Important
        val q4: List<TaskResponse>  // Not Urgent + Not Important
    ) : TaskState()
    data class Error(val message: String) : TaskState()
}

class TaskViewModel : ViewModel() {

    private val _state = MutableStateFlow<TaskState>(TaskState.Loading)
    val state: StateFlow<TaskState> = _state

    fun loadTasks() {
        viewModelScope.launch {
            _state.value = TaskState.Loading
            try {
                val res = RetrofitClient.apiService.getTasks()
                val all = res.data ?: emptyList()
                _state.value = TaskState.Success(
                    q1 = all.filter { it.quadrant == 1 },
                    q2 = all.filter { it.quadrant == 2 },
                    q3 = all.filter { it.quadrant == 3 },
                    q4 = all.filter { it.quadrant == 4 }
                )
            } catch (e: Exception) {
                _state.value = TaskState.Error(e.localizedMessage ?: "Failed to load tasks")
            }
        }
    }

    fun addTask(
        title: String,
        description: String,
        quadrant: Int,
        dueDate: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.createTask(
                    CreateTaskRequest(
                        title = title,
                        description = description,
                        quadrant = quadrant,
                        dueDate = if (dueDate.isNullOrBlank()) null else dueDate
                    )
                )
                loadTasks()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to create task")
            }
        }
    }

    fun editTask(
        id: String,
        title: String,
        description: String,
        quadrant: Int,
        isCompleted: Boolean,
        dueDate: String?,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.updateTask(
                    id,
                    UpdateTaskRequest(
                        title = title,
                        description = description,
                        quadrant = quadrant,
                        isCompleted = isCompleted,
                        dueDate = if (dueDate.isNullOrBlank()) null else dueDate
                    )
                )
                loadTasks()
                onSuccess()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to edit task")
            }
        }
    }

    fun toggleTask(id: String, onFailure: (String) -> Unit = {}) {
        val currentState = _state.value
        // Optimistic toggle: find task across quadrants and flip isCompleted
        if (currentState is TaskState.Success) {
            val allLists = listOf(currentState.q1, currentState.q2, currentState.q3, currentState.q4)
            val updated = allLists.map { list ->
                list.map { task ->
                    if (task.id == id) task.copy(isCompleted = !task.isCompleted) else task
                }
            }
            _state.value = TaskState.Success(
                q1 = updated[0],
                q2 = updated[1],
                q3 = updated[2],
                q4 = updated[3]
            )
        }

        viewModelScope.launch {
            try {
                RetrofitClient.apiService.toggleTaskStatus(id)
            } catch (e: Exception) {
                // Revert on failure
                loadTasks()
                onFailure(e.localizedMessage ?: "Failed to toggle task status")
            }
        }
    }

    fun deleteTask(id: String, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteTask(id)
                loadTasks()
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Failed to delete task")
            }
        }
    }
}
