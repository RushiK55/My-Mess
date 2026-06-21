package com.example.mymess.presentation.user

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.ImageUploader
import com.example.mymess.core.Resource
import com.example.mymess.core.SessionManager
import com.example.mymess.data.models.User
import com.example.mymess.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val imageUploader: ImageUploader,
) : ViewModel() {

    private val _profileState = MutableStateFlow<Resource<User>>(Resource.Loading)
    val profileState: StateFlow<Resource<User>> = _profileState

    private val _profileUpdateState = MutableStateFlow<Resource<User>?>(null)
    val profileUpdateState: StateFlow<Resource<User>?> = _profileUpdateState

    private val _passwordState = MutableStateFlow<Resource<Unit>?>(null)
    val passwordState: StateFlow<Resource<Unit>?> = _passwordState

    private val _logoutState = MutableStateFlow(false)
    val logoutState: StateFlow<Boolean> = _logoutState

    fun loadProfile() {
        val uid = sessionManager.getUid()
        if (uid.isNullOrBlank()) {
            _profileState.value = Resource.Error("Invalid session. Please login again")
            return
        }

        viewModelScope.launch {
            _profileState.value = Resource.Loading
            _profileState.value = authRepository.getUserByUid(uid)
        }
    }

    fun updateProfile(name: String, phone: String, profilePicUri: Uri?, existingProfilePic: String?) {
        val uid = sessionManager.getUid()
        if (uid.isNullOrBlank()) {
            _profileUpdateState.value = Resource.Error("Invalid session. Please login again")
            return
        }

        viewModelScope.launch {
            _profileUpdateState.value = Resource.Loading
            
            val profilePicUrl = if (profilePicUri != null) {
                when (val result = imageUploader.uploadImage(profilePicUri)) {
                    is Resource.Success -> result.data
                    is Resource.Error -> {
                        _profileUpdateState.value = Resource.Error("Image upload failed: ${result.message}")
                        return@launch
                    }
                    Resource.Loading -> null
                }
            } else {
                existingProfilePic
            }

            val result = authRepository.updateUserProfile(uid, name, phone, profilePicUrl)
            _profileUpdateState.value = result
            if (result is Resource.Success) {
                _profileState.value = Resource.Success(result.data)
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        val uid = sessionManager.getUid()
        if (uid.isNullOrBlank()) {
            _passwordState.value = Resource.Error("Invalid session. Please login again")
            return
        }
        if (newPassword != confirmPassword) {
            _passwordState.value = Resource.Error("New password and confirm password must match")
            return
        }

        viewModelScope.launch {
            _passwordState.value = Resource.Loading
            _passwordState.value = authRepository.changePassword(uid, currentPassword, newPassword)
        }
    }

    fun logout() {
        sessionManager.clearSession()
        _logoutState.value = true
    }

    fun clearProfileUpdateState() {
        _profileUpdateState.value = null
    }

    fun clearPasswordState() {
        _passwordState.value = null
    }
}
