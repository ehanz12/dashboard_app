package com.reihan.dashboard.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reihan.dashboard.data.model.ChangePasswordRequest
import com.reihan.dashboard.data.model.EditProfileRequest
import com.reihan.dashboard.data.model.UserResponse
import com.reihan.dashboard.data.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ProfileState {
    object Loading : ProfileState()
    data class Loaded(
        val user: UserResponse,
        val message: String? = null
    ) : ProfileState()
    data class Error(val message: String) : ProfileState()
}

sealed class ProfileActionState {
    object Idle : ProfileActionState()
    object Loading : ProfileActionState()
    data class Success(val message: String) : ProfileActionState()
    data class Error(val message: String) : ProfileActionState()
}

class ProfileViewModel : ViewModel() {

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val profileState: StateFlow<ProfileState> = _profileState

    private val _actionState = MutableStateFlow<ProfileActionState>(ProfileActionState.Idle)
    val actionState: StateFlow<ProfileActionState> = _actionState

    fun loadProfile() {
        viewModelScope.launch {
            _profileState.value = ProfileState.Loading
            _actionState.value = ProfileActionState.Idle
            try {
                val response = RetrofitClient.apiService.getMe()
                val user = response.data
                if (user != null) {
                    _profileState.value = ProfileState.Loaded(user)
                } else {
                    _profileState.value = ProfileState.Error(response.message ?: "Failed to load profile")
                }
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.localizedMessage ?: "Failed to load profile")
            }
        }
    }

    fun editProfile(name: String, email: String, phoneNumber: String, bio: String) {
        viewModelScope.launch {
            _actionState.value = ProfileActionState.Loading
            try {
                val response = RetrofitClient.apiService.editProfile(EditProfileRequest(name, email, phoneNumber, bio))
                val user = response.data
                val message = response.message ?: "Profile updated successfully"
                if (user != null) {
                    _profileState.value = ProfileState.Loaded(user, message)
                }
                _actionState.value = ProfileActionState.Success(message)
            } catch (e: Exception) {
                _actionState.value = ProfileActionState.Error(e.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _actionState.value = ProfileActionState.Loading
            try {
                val response = RetrofitClient.apiService.changePassword(
                    ChangePasswordRequest(
                        oldPassword,
                        newPassword,
                        confirmPassword
                    )
                )
                val message = response.message ?: "Password changed successfully"
                _actionState.value = ProfileActionState.Success(message)
            } catch (e: Exception) {
                _actionState.value = ProfileActionState.Error(e.localizedMessage ?: "Failed to change password")
            }
        }
    }

    fun resetActionState() {
        _actionState.value = ProfileActionState.Idle
    }
}
