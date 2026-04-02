package com.example.mymess.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.SessionManager
import com.example.mymess.core.Resource
import com.example.mymess.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _destinationState = MutableStateFlow<String?>(null)
    val destinationState: StateFlow<String?> = _destinationState

    fun resolveDestination() {
        val uid = sessionManager.getUid()
        if (uid.isNullOrBlank()) {
            _destinationState.value = "login"
            return
        }

        viewModelScope.launch {
            when (val result = authRepository.getUserByUid(uid)) {
                is Resource.Success -> {
                    if (result.data.status != "approved") {
                        sessionManager.clearSession()
                        _destinationState.value = "login"
                        return@launch
                    }
                    sessionManager.saveSession(result.data.uid, result.data.role)
                    _destinationState.value = when (result.data.role) {
                        "owner" -> "owner"
                        "admin" -> "admin"
                        else -> "user"
                    }
                }

                is Resource.Error -> {
                    sessionManager.clearSession()
                    _destinationState.value = "login"
                }

                Resource.Loading -> Unit
            }
        }
    }

    fun clearDestination() {
        _destinationState.value = null
    }
}

