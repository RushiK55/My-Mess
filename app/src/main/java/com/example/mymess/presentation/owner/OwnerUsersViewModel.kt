package com.example.mymess.presentation.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.Resource
import com.example.mymess.core.SessionManager
import com.example.mymess.data.models.OwnerUserBillingDetails
import com.example.mymess.data.models.User
import com.example.mymess.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OwnerUsersViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _usersState = MutableStateFlow<Resource<List<User>>>(Resource.Loading)
    val usersState: StateFlow<Resource<List<User>>> = _usersState

    private val _billingDetailsState = MutableStateFlow<Resource<OwnerUserBillingDetails>?>(null)
    val billingDetailsState: StateFlow<Resource<OwnerUserBillingDetails>?> = _billingDetailsState

    private val _actionState = MutableStateFlow<Resource<Unit>?>(null)
    val actionState: StateFlow<Resource<Unit>?> = _actionState

    private val _billGenerationState = MutableStateFlow<Resource<String>?>(null)
    val billGenerationState: StateFlow<Resource<String>?> = _billGenerationState

    fun loadUsers() {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _usersState.value = Resource.Loading
            _usersState.value = ownerRepository.getEnrolledUsers(ownerUid)
        }
    }

    fun loadBillingDetails(userId: String) {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _billingDetailsState.value = Resource.Loading
            _billingDetailsState.value = ownerRepository.getOwnerUserBillingDetails(ownerUid, userId)
        }
    }

    fun generateBillForUser(userId: String) {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _billGenerationState.value = Resource.Loading
            _billGenerationState.value = ownerRepository.generateUserBill(ownerUid, userId)
            _billingDetailsState.value = ownerRepository.getOwnerUserBillingDetails(ownerUid, userId)
        }
    }

    fun blockUser(userId: String, reason: String) {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = ownerRepository.blockUser(ownerUid, userId, reason)
            loadUsers()
        }
    }
}

