package com.example.mymess.presentation.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun save(title: String, image: String) {
        val ownerUid = sessionManager.getUid() ?: return
        if (title.isBlank() || image.isBlank()) {
            _actionState.value = Resource.Error("Title and image URL are required")
            return
        }
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = bannerRepository.saveBanner(
                Banner(
                    title = title.trim(),
                    imageUrl = image.trim(),
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

