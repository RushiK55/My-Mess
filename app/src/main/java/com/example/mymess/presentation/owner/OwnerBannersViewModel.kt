package com.example.mymess.presentation.owner

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.ImageUploader
import com.example.mymess.core.Resource
import com.example.mymess.core.SessionManager
import com.example.mymess.data.models.Banner
import com.example.mymess.data.repository.BannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OwnerBannersViewModel @Inject constructor(
    private val bannerRepository: BannerRepository,
    private val sessionManager: SessionManager,
    private val imageUploader: ImageUploader,
) : ViewModel() {

    private val _bannersState = MutableStateFlow<Resource<List<Banner>>>(Resource.Loading)
    val bannersState: StateFlow<Resource<List<Banner>>> = _bannersState

    private val _actionState = MutableStateFlow<Resource<Unit>?>(null)
    val actionState: StateFlow<Resource<Unit>?> = _actionState

    fun load() {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _bannersState.value = Resource.Loading
            _bannersState.value = bannerRepository.getBannersByCreator(ownerUid, targetRole = "user")
        }
    }

    fun save(title: String, imageUri: Uri) {
        val ownerUid = sessionManager.getUid() ?: return
        if (title.isBlank()) {
            _actionState.value = Resource.Error("Title is required")
            return
        }
        
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            
            val uploadResult = imageUploader.uploadImage(imageUri)
            if (uploadResult is Resource.Error) {
                _actionState.value = Resource.Error("Upload failed: ${uploadResult.message}")
                return@launch
            }
            
            val imageUrl = (uploadResult as Resource.Success).data

            _actionState.value = bannerRepository.saveBanner(
                Banner(
                    title = title.trim(),
                    imageUrl = imageUrl,
                    targetRole = "user",
                    isActive = true,
                    createdBy = ownerUid,
                ),
            )
            load()
        }
    }

    fun delete(bannerId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = bannerRepository.deleteBanner(bannerId)
            load()
        }
    }
}
