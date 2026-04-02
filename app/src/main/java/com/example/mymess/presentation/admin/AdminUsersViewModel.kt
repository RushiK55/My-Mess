package com.example.mymess.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.Resource
import com.example.mymess.data.models.User
import com.example.mymess.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
) : ViewModel() {

    private val _usersState = MutableStateFlow<Resource<List<User>>>(Resource.Loading)
    val usersState: StateFlow<Resource<List<User>>> = _usersState

    private val _actionState = MutableStateFlow<Resource<String>?>(null)
    val actionState: StateFlow<Resource<String>?> = _actionState

    fun loadUsers() {
        viewModelScope.launch {
            _usersState.value = Resource.Loading
            _usersState.value = adminRepository.getAllUsers()
        }
    }

    fun toggleBlock(user: User) {
        val next = if (user.status == "blocked") "approved" else "blocked"
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = when (val result = adminRepository.updateUserStatus(user.uid, next)) {
                is Resource.Success -> Resource.Success(if (next == "blocked") "User blocked" else "User unblocked")
                is Resource.Error -> Resource.Error(result.message)
                Resource.Loading -> Resource.Loading
            }
            loadUsers()
        }
    }

    fun deleteUser(user: User) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = when (val result = adminRepository.deleteUser(user.uid)) {
                is Resource.Success -> Resource.Success("User deleted")
                is Resource.Error -> Resource.Error(result.message)
                Resource.Loading -> Resource.Loading
            }
            loadUsers()
        }
    }
}

