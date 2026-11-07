package com.reihan.dashboard.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.*
import com.reihan.dashboard.data.network.RetrofitClient
import com.reihan.dashboard.data.network.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String, onSuccess: () -> Unit, onNotVerified: (String) -> Unit = {}) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password are required")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.apiService.login(LoginRequest(email, password))
                val token = response.token ?: response.data?.token
                if (!token.isNullOrBlank()) {
                    SessionManager.saveToken(token)
                    syncFmcToken() // Sync FMC token immediately
                    _authState.value = AuthState.Success(response.message ?: "Successfully logged in")
                    onSuccess()
                } else {
                    _authState.value = AuthState.Error("Token not received. Check server response.")
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorObj = try {
                    Gson().fromJson(errorBody, Map::class.java)
                } catch (ex: Exception) {
                    null
                }

                if (e.code() == 403 && (errorObj?.get("error") == "email_not_verified" || errorObj?.get("email_verified") == false)) {
                    val targetEmail = errorObj["email"]?.toString() ?: email
                    _authState.value = AuthState.Idle
                    onNotVerified(targetEmail)
                } else {
                    val errorMessage = errorObj?.get("Message")?.toString() 
                        ?: errorObj?.get("message")?.toString() 
                        ?: errorObj?.get("error")?.toString() 
                        ?: "Login failed"
                    _authState.value = AuthState.Error(errorMessage)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to log in")
            }
        }
    }

    fun register(name: String, email: String, password: String, phoneNumber: String, bio: String, onRegistered: (String) -> Unit) {
        if (name.isBlank() || email.isBlank() || password.isBlank() || phoneNumber.isBlank() || bio.isBlank()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.apiService.register(RegisterRequest(name, email, password, phoneNumber, bio))
                val token = response.token ?: response.data?.token
                if (!token.isNullOrBlank()) {
                    SessionManager.saveToken(token)
                    syncFmcToken()
                }
                _authState.value = AuthState.Success(response.message ?: "Register successful. Please check your email for verification code.")
                onRegistered(email)
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = try {
                    val errorObj = Gson().fromJson(errorBody, Map::class.java)
                    errorObj["Message"]?.toString() ?: errorObj["message"]?.toString() ?: "Registration failed"
                } catch (ex: Exception) {
                    "Error ${e.code()}: ${e.message()}"
                }
                _authState.value = AuthState.Error(errorMessage)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to register")
            }
        }
    }

    fun verifyEmail(email: String, code: String, onSuccess: () -> Unit) {
        if (email.isBlank() || code.isBlank()) {
            _authState.value = AuthState.Error("Email and code are required")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.apiService.verifyEmail(VerifyEmailRequest(email, code))
                val token = response.token ?: response.data?.token
                
                if (!token.isNullOrBlank()) {
                    SessionManager.saveToken(token)
                    syncFmcToken()
                    _authState.value = AuthState.Success(response.message ?: "Email verified successfully")
                } else {
                    _authState.value = AuthState.Success(response.message ?: "Email verified! Redirecting...")
                }
                onSuccess()
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = try {
                    val errorObj = Gson().fromJson(errorBody, Map::class.java)
                    errorObj["Message"]?.toString() ?: errorObj["message"]?.toString() ?: "Verification failed"
                } catch (ex: Exception) {
                    "Error ${e.code()}: ${e.message()}"
                }
                _authState.value = AuthState.Error(errorMessage)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to verify email")
            }
        }
    }

    fun forgotPassword(email: String, onSuccess: (String) -> Unit) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Email is required")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.apiService.forgotPassword(ForgotPasswordRequest(email))
                _authState.value = AuthState.Success(response.message ?: "Reset code sent to your email")
                onSuccess(email)
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = try {
                    val errorObj = Gson().fromJson(errorBody, Map::class.java)
                    errorObj["Message"]?.toString() ?: errorObj["message"]?.toString() ?: "Failed to send reset code"
                } catch (ex: Exception) {
                    "Error ${e.code()}: ${e.message()}"
                }
                _authState.value = AuthState.Error(errorMessage)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to process request")
            }
        }
    }

    fun resetPassword(request: ResetPasswordRequest, onSuccess: () -> Unit) {
        if (request.email.isBlank() || request.code.isBlank() || request.newPassword.isBlank()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.apiService.resetPassword(request)
                _authState.value = AuthState.Success(response.message ?: "Password reset successfully")
                onSuccess()
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = try {
                    val errorObj = Gson().fromJson(errorBody, Map::class.java)
                    errorObj["Message"]?.toString() ?: errorObj["message"]?.toString() ?: "Reset failed"
                } catch (ex: Exception) {
                    "Error ${e.code()}: ${e.message()}"
                }
                _authState.value = AuthState.Error(errorMessage)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to reset password")
            }
        }
    }

    fun setError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun syncFmcToken() {
        val hasJwt = SessionManager.hasToken()
        android.util.Log.d("FMC_SYNC", "1. JWT Token status: " + if (hasJwt) "Stored" else "Not Found")
        
        if (!hasJwt) {
            android.util.Log.d("FMC_SYNC", "Sync skipped: No valid JWT token found in SessionManager")
            return
        }
        
        android.util.Log.d("FMC_SYNC", "2. Obtaining FMC token from Firebase...")
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                android.util.Log.d("FMC_SYNC", "3. FMC Token successfully obtained: $token")
                
                viewModelScope.launch {
                    try {
                        val request = UserDeviceRequest(fmcToken = token, deviceType = "android")
                        android.util.Log.d("FMC_SYNC", "4. Sending Retrofit request to POST /api/v1/devices")
                        
                        val response = RetrofitClient.apiService.saveDevice(request)
                        
                        android.util.Log.d("FMC_SYNC", "5. Response Received - HTTP ${response.code()}")
                        
                        if (response.isSuccessful) {
                            android.util.Log.d("FMC_SYNC", "6. Device information synchronized successfully.")
                        } else {
                            android.util.Log.e("FMC_SYNC", "6. Device sync failed with status ${response.code()}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("FMC_SYNC", "ERR: Unexpected Retrofit Exception: ${e.localizedMessage}")
                    }
                }
            } else {
                android.util.Log.e("FMC_SYNC", "ERR: Firebase failed to provide registration token", task.exception)
            }
        }
    }

    fun googleLogin(idToken: String, onSuccess: () -> Unit) {
        if (idToken.isBlank()) {
            _authState.value = AuthState.Error("ID token is required")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.apiService.googleAuth(
                    GoogleSignUpRequest(idToken = idToken)
                )
                val token = response.token ?: response.data?.token
                if (!token.isNullOrBlank()) {
                    SessionManager.saveToken(token)
                    syncFmcToken()
                    _authState.value = AuthState.Success(response.message ?: "Successfully logged in with Google")
                    onSuccess()
                } else {
                    _authState.value = AuthState.Error("Token not received from Google login response.")
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = try {
                    val errorObj = Gson().fromJson(errorBody, Map::class.java)
                    errorObj["Message"]?.toString() ?: errorObj["message"]?.toString() ?: "Google login failed"
                } catch (ex: Exception) {
                    "Error ${e.code()}: ${e.message()}"
                }
                _authState.value = AuthState.Error(errorMessage)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to login with Google")
            }
        }
    }

    fun googleRegister(
        idToken: String,
        name: String?,
        phoneNumber: String? = null,
        bio: String? = null,
        onRegistered: () -> Unit
    ) {
        if (idToken.isBlank()) {
            _authState.value = AuthState.Error("ID token is required")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitClient.apiService.googleAuth(
                    GoogleSignUpRequest(
                        idToken = idToken,
                        name = name,
                        phoneNumber = phoneNumber,
                        bio = bio
                    )
                )
                val token = response.token ?: response.data?.token
                if (!token.isNullOrBlank()) {
                    SessionManager.saveToken(token)
                    syncFmcToken()
                    _authState.value = AuthState.Success(response.message ?: "Successfully registered with Google")
                    onRegistered()
                } else {
                    _authState.value = AuthState.Error("Token not received from Google registration.")
                }
            } catch (e: HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                val errorMessage = try {
                    val errorObj = Gson().fromJson(errorBody, Map::class.java)
                    errorObj["Message"]?.toString() ?: errorObj["message"]?.toString() ?: "Google registration failed"
                } catch (ex: Exception) {
                    "Error ${e.code()}: ${e.message()}"
                }
                _authState.value = AuthState.Error(errorMessage)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Failed to register with Google")
            }
        }
    }
}
