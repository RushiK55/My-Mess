package com.example.mymess.presentation.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.Resource
import com.example.mymess.core.SessionManager
import com.example.mymess.data.models.Banner
import com.example.mymess.data.models.JoinRequestWithUser
import com.example.mymess.data.models.Order
import com.example.mymess.data.models.OwnerDashboardSummary
import com.example.mymess.data.repository.BannerRepository
import com.example.mymess.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OwnerViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val bannerRepository: BannerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _orderRequestsState = MutableStateFlow<Resource<List<Order>>>(Resource.Loading)
    val orderRequestsState: StateFlow<Resource<List<Order>>> = _orderRequestsState

    private val _joinRequestsState = MutableStateFlow<Resource<List<JoinRequestWithUser>>>(Resource.Loading)
    val joinRequestsState: StateFlow<Resource<List<JoinRequestWithUser>>> = _joinRequestsState

    private val _summaryState = MutableStateFlow<Resource<OwnerDashboardSummary>>(Resource.Loading)
    val summaryState: StateFlow<Resource<OwnerDashboardSummary>> = _summaryState

    private val _updateState = MutableStateFlow<Resource<Unit>?>(null)
    val updateState: StateFlow<Resource<Unit>?> = _updateState

    private val _joinUpdateState = MutableStateFlow<Resource<Unit>?>(null)
    val joinUpdateState: StateFlow<Resource<Unit>?> = _joinUpdateState

    private val _bannerState = MutableStateFlow<Resource<List<Banner>>>(Resource.Loading)
    val bannerState: StateFlow<Resource<List<Banner>>> = _bannerState

    fun loadHome() {
        loadOrderRequests()
        loadJoinRequests()
        loadSummary()
        loadBanners()
    }

    fun loadOrderRequests() {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _orderRequestsState.value = Resource.Loading
            _orderRequestsState.value = ownerRepository.getOwnerOrderRequests(ownerUid)
        }
    }

    fun loadJoinRequests() {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _joinRequestsState.value = Resource.Loading
            _joinRequestsState.value = ownerRepository.getPendingJoinRequests(ownerUid)
        }
    }

    fun loadSummary() {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _summaryState.value = Resource.Loading
            _summaryState.value = ownerRepository.getOwnerDashboardSummary(ownerUid)
        }
    }

    fun acceptOrder(order: Order) {
        updateStatus(order.orderId, "accepted")
    }

    fun rejectOrder(order: Order) {
        viewModelScope.launch {
            _updateState.value = Resource.Loading
            _updateState.value = ownerRepository.rejectOrder(order.orderId)
            loadOrderRequests()
            loadSummary()
        }
    }

    fun approveJoinRequest(requestId: String) {
        viewModelScope.launch {
            _joinUpdateState.value = Resource.Loading
            _joinUpdateState.value = ownerRepository.approveJoinRequest(requestId)
            loadJoinRequests()
            loadSummary()
        }
    }

    fun rejectJoinRequest(requestId: String) {
        viewModelScope.launch {
            _joinUpdateState.value = Resource.Loading
            _joinUpdateState.value = ownerRepository.rejectJoinRequest(requestId)
            loadJoinRequests()
            loadSummary()
        }
    }

    private fun updateStatus(orderId: String, status: String) {
        viewModelScope.launch {
            _updateState.value = Resource.Loading
            _updateState.value = ownerRepository.updateOrderStatus(orderId, status)
            loadOrderRequests()
            loadSummary()
        }
    }

    fun loadBanners() {
        viewModelScope.launch {
            _bannerState.value = Resource.Loading
            _bannerState.value = bannerRepository.getActiveBanners("owner")
        }
    }
}
