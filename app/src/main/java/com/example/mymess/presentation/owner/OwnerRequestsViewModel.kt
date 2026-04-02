package com.example.mymess.presentation.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.Resource
import com.example.mymess.core.SessionManager
import com.example.mymess.data.models.JoinRequestWithUser
import com.example.mymess.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OwnerRequestsViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _requestsState = MutableStateFlow<Resource<List<JoinRequestWithUser>>>(Resource.Loading)
    val requestsState: StateFlow<Resource<List<JoinRequestWithUser>>> = _requestsState

    private val _actionState = MutableStateFlow<Resource<Unit>?>(null)
    val actionState: StateFlow<Resource<Unit>?> = _actionState

    fun loadRequests() {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _requestsState.value = Resource.Loading
            _requestsState.value = ownerRepository.getPendingJoinRequests(ownerUid)
        }
    }

    fun approve(requestId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = ownerRepository.approveJoinRequest(requestId)
            loadRequests()
        }
    }

    fun reject(requestId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = ownerRepository.rejectJoinRequest(requestId)
            loadRequests()
        }
    }
}

