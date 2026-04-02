package com.example.mymess.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.Resource
import com.example.mymess.data.models.AnalyticsSummary
import com.example.mymess.data.models.Banner
import com.example.mymess.data.models.User
import com.example.mymess.data.repository.AdminRepository
import com.example.mymess.data.repository.AnalyticsRepository
import com.example.mymess.data.repository.BannerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AdminViewModel @Inject constructor(
    private val adminRepository: AdminRepository,
    private val bannerRepository: BannerRepository,
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val _pendingOwnersState = MutableStateFlow<Resource<List<User>>>(Resource.Loading)
    val pendingOwnersState: StateFlow<Resource<List<User>>> = _pendingOwnersState

    private val _actionState = MutableStateFlow<Resource<Unit>?>(null)
    val actionState: StateFlow<Resource<Unit>?> = _actionState

    private val _bannerState = MutableStateFlow<Resource<List<Banner>>>(Resource.Loading)
    val bannerState: StateFlow<Resource<List<Banner>>> = _bannerState

    private val _summaryState = MutableStateFlow<Resource<AnalyticsSummary>>(Resource.Loading)
    val summaryState: StateFlow<Resource<AnalyticsSummary>> = _summaryState

    fun loadPendingOwners() {
        viewModelScope.launch {
            _pendingOwnersState.value = Resource.Loading
            _pendingOwnersState.value = adminRepository.getPendingOwners()
        }
    }

    fun approveOwner(ownerUid: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = adminRepository.approveOwner(ownerUid)
            loadPendingOwners()
        }
    }

    fun rejectOwner(ownerUid: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = adminRepository.rejectOwner(ownerUid)
            loadPendingOwners()
        }
    }

    fun loadBanners() {
        viewModelScope.launch {
            _bannerState.value = Resource.Loading
            _bannerState.value = bannerRepository.getActiveBanners("admin")
        }
    }

    fun loadSummary() {
        viewModelScope.launch {
            _summaryState.value = Resource.Loading
            _summaryState.value = analyticsRepository.getAdminAnalytics()
        }
    }
}

