package com.example.mymess.presentation.owner

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.ImageUploader
import com.example.mymess.core.Resource
import com.example.mymess.core.SessionManager
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.User
import com.example.mymess.data.repository.AuthRepository
import com.example.mymess.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OwnerProfileViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val imageUploader: ImageUploader,
) : ViewModel() {

    private val _messState = MutableStateFlow<Resource<Mess>>(Resource.Loading)
    val messState: StateFlow<Resource<Mess>> = _messState

    private val _ownerState = MutableStateFlow<Resource<User>>(Resource.Loading)
    val ownerState: StateFlow<Resource<User>> = _ownerState

    private val _saveState = MutableStateFlow<Resource<Unit>?>(null)
    val saveState: StateFlow<Resource<Unit>?> = _saveState

    private val _profileSaveState = MutableStateFlow<Resource<User>?>(null)
    val profileSaveState: StateFlow<Resource<User>?> = _profileSaveState

    private val _passwordState = MutableStateFlow<Resource<Unit>?>(null)
    val passwordState: StateFlow<Resource<Unit>?> = _passwordState

    private val _logoutState = MutableStateFlow(false)
    val logoutState: StateFlow<Boolean> = _logoutState

    fun loadMess() {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _messState.value = Resource.Loading
            _messState.value = ownerRepository.getOwnerMess(ownerUid)

            _ownerState.value = Resource.Loading
            _ownerState.value = authRepository.getUserByUid(ownerUid)
        }
    }

    // --- Mess Flow ---

    fun saveMessDetails(mess: Mess) {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _saveState.value = Resource.Loading
            _saveState.value = ownerRepository.updateOwnerMess(ownerUid, mess)
            loadMess()
        }
    }

    fun updateMessImage(imageUri: Uri) {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _saveState.value = Resource.Loading
            
            when (val uploadResult = imageUploader.uploadImage(imageUri)) {
                is Resource.Success -> {
                    val imageUrl = uploadResult.data
                    val currentMessRes = _messState.value
                    if (currentMessRes is Resource.Success) {
                        val updatedMess = currentMessRes.data.copy(imageUrl = imageUrl)
                        _saveState.value = ownerRepository.updateOwnerMess(ownerUid, updatedMess)
                        loadMess()
                    } else {
                        _saveState.value = Resource.Error("Cannot update image: Mess details not loaded")
                    }
                }
                is Resource.Error -> {
                    _saveState.value = Resource.Error("Mess image upload failed: ${uploadResult.message}")
                }
                Resource.Loading -> Unit
            }
        }
    }

    // --- Owner Flow ---

    fun saveOwnerDetails(name: String, phone: String) {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _profileSaveState.value = Resource.Loading
            
            val currentUser = (_ownerState.value as? Resource.Success)?.data
            val existingPic = currentUser?.profilePic
            
            _profileSaveState.value = authRepository.updateUserProfile(ownerUid, name, phone, existingPic)
            _ownerState.value = authRepository.getUserByUid(ownerUid)
        }
    }

    fun updateOwnerProfilePic(profilePicUri: Uri) {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _profileSaveState.value = Resource.Loading

            when (val uploadResult = imageUploader.uploadImage(profilePicUri)) {
                is Resource.Success -> {
                    val profilePicUrl = uploadResult.data
                    val currentUser = (_ownerState.value as? Resource.Success)?.data
                    if (currentUser != null) {
                        _profileSaveState.value = authRepository.updateUserProfile(
                            ownerUid, 
                            currentUser.name, 
                            currentUser.phone, 
                            profilePicUrl
                        )
                        _ownerState.value = authRepository.getUserByUid(ownerUid)
                    } else {
                        _profileSaveState.value = Resource.Error("User profile not loaded")
                    }
                }
                is Resource.Error -> {
                    _profileSaveState.value = Resource.Error("Profile pic upload failed: ${uploadResult.message}")
                }
                Resource.Loading -> Unit
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        val ownerUid = sessionManager.getUid() ?: return
        if (newPassword != confirmPassword) {
            _passwordState.value = Resource.Error("New password and confirm password must match")
            return
        }

        viewModelScope.launch {
            _passwordState.value = Resource.Loading
            _passwordState.value = authRepository.changePassword(ownerUid, currentPassword, newPassword)
        }
    }

    fun clearSaveState() {
        _saveState.value = null
    }

    fun clearProfileSaveState() {
        _profileSaveState.value = null
    }

    fun clearPasswordState() {
        _passwordState.value = null
    }

    fun logout() {
        sessionManager.clearSession()
        _logoutState.value = true
    }
}
