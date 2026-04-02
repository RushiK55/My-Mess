package com.example.mymess.presentation.admin

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
class AdminBannersViewModel @Inject constructor(
    private val bannerRepository: BannerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _bannersState = MutableStateFlow<Resource<List<Banner>>>(Resource.Loading)
    val bannersState: StateFlow<Resource<List<Banner>>> = _bannersState

    private val _actionState = MutableStateFlow<Resource<String>?>(null)
    val actionState: StateFlow<Resource<String>?> = _actionState

    fun load() {
        viewModelScope.launch {
            _bannersState.value = Resource.Loading
            _bannersState.value = bannerRepository.getAllBanners()
        }
    }

    fun save(
        editingBannerId: String?,
        title: String,
        imageUrl: String,
        targetRole: String,
        isActive: Boolean,
    ) {
        if (title.isBlank() || imageUrl.isBlank()) {
            _actionState.value = Resource.Error("Title and image URL are required")
            return
        }

        val role = targetRole.trim().lowercase()
        if (role !in setOf("user", "owner", "all", "admin")) {
            _actionState.value = Resource.Error("Invalid target role")
            return
        }

        viewModelScope.launch {
            _actionState.value = Resource.Loading
            val result = bannerRepository.saveBanner(
                Banner(
                    bannerId = editingBannerId.orEmpty(),
                    title = title.trim(),
                    imageUrl = imageUrl.trim(),
                    targetRole = role,
                    isActive = isActive,
                    createdBy = sessionManager.getUid(),
                ),
            )
            _actionState.value = when (result) {
                is Resource.Success -> Resource.Success(if (editingBannerId.isNullOrBlank()) "Banner created" else "Banner updated")
                is Resource.Error -> Resource.Error(result.message)
                Resource.Loading -> Resource.Loading
            }
            load()
        }
    }

    fun delete(bannerId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = when (val result = bannerRepository.deleteBanner(bannerId)) {
                is Resource.Success -> Resource.Success("Banner deleted")
                is Resource.Error -> Resource.Error(result.message)
                Resource.Loading -> Resource.Loading
            }
            load()
        }
    }
}

