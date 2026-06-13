package com.reihan.dashboard.ui.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.*
import com.reihan.dashboard.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class JournalState {
    object Loading : JournalState()
    data class Success(
        val journals: List<JournalResponse>,
        val todayJournal: JournalResponse?
    ) : JournalState()
    data class Error(val message: String) : JournalState()
}

class JournalViewModel : ViewModel() {

    private val _state = MutableStateFlow<JournalState>(JournalState.Loading)
    val state: StateFlow<JournalState> = _state

    fun loadJournals() {
        viewModelScope.launch {
            _state.value = JournalState.Loading
            try {
                val res = RetrofitClient.apiService.getJournals()
                val all = res.data ?: emptyList()
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val todayEntry = all.find { it.entryDate.startsWith(today) }
                _state.value = JournalState.Success(journals = all, todayJournal = todayEntry)
            } catch (e: Exception) {
                _state.value = JournalState.Error(e.localizedMessage ?: "Failed to load journals")
            }
        }
    }

    fun saveJournal(mood: Int, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                RetrofitClient.apiService.createOrUpdateJournal(
                    CreateJournalRequest(mood = mood, content = content, entryDate = today)
                )
                loadJournals()
                onSuccess()
            } catch (e: Exception) {
                // handle silently
            }
        }
    }

    fun deleteJournal(id: String) {
        viewModelScope.launch {
            try {
                RetrofitClient.apiService.deleteJournal(id)
                loadJournals()
            } catch (e: Exception) {
                // handle silently
            }
        }
    }
}
