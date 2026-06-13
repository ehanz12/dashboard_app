package com.reihan.dashboard.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.LoginRequest
import com.reihan.dashboard.data.model.RegisterRequest
import com.reihan.dashboard.data.network.RetrofitClient
import com.reihan.dashboard.data.network.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password are required")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(email, password))
                val token = response.token
                if (!token.isNullOrBlank()) {
                    SessionManager.saveToken(token)
                    _authState.value = AuthState.Success(response.message ?: "Successfully logged in")
                    onSuccess()
                } else {
                    _authState.value = AuthState.Error("Token not received")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to log in")
            }
        }
    }

    fun register(name: String, email: String, password: String, onRegistered: () -> Unit) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.apiService.register(RegisterRequest(name, email, password))
                _authState.value = AuthState.Success(response.message ?: "Register successful")
                onRegistered()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to register")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
