package com.example.mymess.presentation.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.Resource
import com.example.mymess.core.SessionManager
import com.example.mymess.data.models.Order
import com.example.mymess.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OwnerPendingOrdersViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _ordersState = MutableStateFlow<Resource<List<Order>>>(Resource.Loading)
    val ordersState: StateFlow<Resource<List<Order>>> = _ordersState

    private val _actionState = MutableStateFlow<Resource<Unit>?>(null)
    val actionState: StateFlow<Resource<Unit>?> = _actionState

    fun load() {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _ordersState.value = Resource.Loading
            _ordersState.value = ownerRepository.getOwnerPendingOrders(ownerUid)
        }
    }

    fun advance(order: Order) {
        val nextStatus = when (order.status) {
            "accepted" -> "preparing"
            "preparing" -> "ready"
            "ready" -> "delivered"
            else -> "delivered"
        }
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = ownerRepository.updateOrderStatus(order.orderId, nextStatus)
            load()
        }
    }
}

