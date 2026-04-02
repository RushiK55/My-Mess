package com.example.mymess.presentation.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.Resource
import com.example.mymess.core.SessionManager
import com.example.mymess.data.models.PaymentRecord
import com.example.mymess.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class UserPaymentsViewModel @Inject constructor(
    private val paymentRepository: PaymentRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _paymentsState = MutableStateFlow<Resource<List<PaymentRecord>>>(Resource.Loading)
    val paymentsState: StateFlow<Resource<List<PaymentRecord>>> = _paymentsState

    private val _actionState = MutableStateFlow<Resource<Unit>?>(null)
    val actionState: StateFlow<Resource<Unit>?> = _actionState

    fun loadPayments() {
        val uid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _paymentsState.value = Resource.Loading
            _paymentsState.value = paymentRepository.getUserPayments(uid)
        }
    }

    fun submitPayment(paymentId: String, method: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = paymentRepository.submitPayment(paymentId, method)
            loadPayments()
        }
    }
}
