package com.example.mymess.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.Resource
import com.example.mymess.core.SessionManager
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.User
import com.example.mymess.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _authState = MutableStateFlow<Resource<User>?>(null)
    val authState: StateFlow<Resource<User>?> = _authState

    private val _forgotPasswordState = MutableStateFlow<Resource<Unit>?>(null)
    val forgotPasswordState: StateFlow<Resource<Unit>?> = _forgotPasswordState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = Resource.Loading
            val result = authRepository.login(email, password)
            if (result is Resource.Success) {
                sessionManager.saveSession(result.data.uid, result.data.role)
            }
            _authState.value = result
        }
    }

    fun register(
        name: String,
        email: String,
        phone: String,
        password: String,
        role: String,
        messName: String,
        messAddress: String,
        messCity: String,
        messContact: String,
        messDescription: String,
    ) {
        if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
            _authState.value = Resource.Error("Name, phone, email, and password are required")
            return
        }

        viewModelScope.launch {
            _authState.value = Resource.Loading
            val normalizedRole = if (role == "owner") "owner" else "user"
            if (
                normalizedRole == "owner" &&
                (messName.isBlank() || messAddress.isBlank() || messCity.isBlank() || messContact.isBlank())
            ) {
                _authState.value = Resource.Error("All owner mess details are required")
                return@launch
            }
            val user = User(
                uid = "",
                name = name,
                email = email,
                phone = phone,
                password = password,
                role = normalizedRole,
                status = if (normalizedRole == "owner") "pending" else "approved",
            )
            val ownerMessDraft = if (normalizedRole == "owner") {
                Mess(
                    name = messName.trim(),
                    address = messAddress.trim(),
                    city = messCity.trim(),
                    contact = messContact.trim(),
                    description = messDescription.trim(),
                    isApproved = false,
                )
            } else {
                null
            }
            val result = authRepository.register(user, ownerMessDraft)
            if (result is Resource.Success && result.data.status == "approved") {
                sessionManager.saveSession(result.data.uid, result.data.role)
            }
            _authState.value = result
        }
    }

    fun resetPassword(email: String, newPassword: String, confirmPassword: String) {
        if (email.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            _forgotPasswordState.value = Resource.Error("Email and both password fields are required")
            return
        }
        if (newPassword != confirmPassword) {
            _forgotPasswordState.value = Resource.Error("Passwords do not match")
            return
        }

        viewModelScope.launch {
            _forgotPasswordState.value = Resource.Loading
            _forgotPasswordState.value = authRepository.resetPassword(email.trim(), newPassword.trim())
        }
    }

    fun clearAuthState() {
        _authState.value = null
    }

    fun clearForgotPasswordState() {
        _forgotPasswordState.value = null
    }
}


