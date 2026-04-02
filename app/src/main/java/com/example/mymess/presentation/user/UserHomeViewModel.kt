package com.example.mymess.presentation.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.Resource
import com.example.mymess.core.SessionManager
import com.example.mymess.data.models.Banner
import com.example.mymess.data.models.Meal
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.Order
import com.example.mymess.data.models.PaymentRecord
import com.example.mymess.data.models.User
import com.example.mymess.data.models.activeEnrolledMessId
import com.example.mymess.data.repository.BannerRepository
import com.example.mymess.data.repository.MessRepository
import com.example.mymess.data.repository.PaymentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class UserHomeViewModel @Inject constructor(
    private val messRepository: MessRepository,
    private val bannerRepository: BannerRepository,
    private val paymentRepository: PaymentRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _cloudMealsState = MutableStateFlow<Resource<List<Meal>>>(Resource.Loading)
    val cloudMealsState: StateFlow<Resource<List<Meal>>> = _cloudMealsState

    private val _messMealsState = MutableStateFlow<Resource<List<Meal>>>(Resource.Loading)
    val messMealsState: StateFlow<Resource<List<Meal>>> = _messMealsState

    private val _orderState = MutableStateFlow<Resource<String>?>(null)
    val orderState: StateFlow<Resource<String>?> = _orderState

    private val _historyState = MutableStateFlow<Resource<List<Order>>>(Resource.Loading)
    val historyState: StateFlow<Resource<List<Order>>> = _historyState

    private val _messesState = MutableStateFlow<Resource<List<Mess>>>(Resource.Loading)
    val messesState: StateFlow<Resource<List<Mess>>> = _messesState

    private val _selectedMessMenuState = MutableStateFlow<Resource<List<Meal>>>(Resource.Loading)
    val selectedMessMenuState: StateFlow<Resource<List<Meal>>> = _selectedMessMenuState

    private val _joinState = MutableStateFlow<Resource<Unit>?>(null)
    val joinState: StateFlow<Resource<Unit>?> = _joinState

    private val _bannerState = MutableStateFlow<Resource<List<Banner>>>(Resource.Loading)
    val bannerState: StateFlow<Resource<List<Banner>>> = _bannerState

    private val _userState = MutableStateFlow<Resource<User>>(Resource.Loading)
    val userState: StateFlow<Resource<User>> = _userState

    private val _enrolledMessState = MutableStateFlow<Resource<Mess?>>(Resource.Loading)
    val enrolledMessState: StateFlow<Resource<Mess?>> = _enrolledMessState

    private val _paymentsState = MutableStateFlow<Resource<List<PaymentRecord>>>(Resource.Loading)
    val paymentsState: StateFlow<Resource<List<PaymentRecord>>> = _paymentsState

    fun loadUserHome() {
        loadBanners()
        loadCloudMeals()
        loadEnrolledSection()
    }

    fun loadCloudMeals() {
        viewModelScope.launch {
            _cloudMealsState.value = Resource.Loading
            _cloudMealsState.value = messRepository.getCloudMeals()
        }
    }

    fun loadEnrolledSection() {
        val uid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _userState.value = Resource.Loading
            val userResult = messRepository.getUserProfile(uid)
            _userState.value = userResult

            if (userResult !is Resource.Success) {
                _enrolledMessState.value = Resource.Error("Unable to load mess enrollment")
                _messMealsState.value = Resource.Success(emptyList())
                return@launch
            }

            val messId = userResult.data.activeEnrolledMessId()
            if (messId.isNullOrBlank()) {
                _enrolledMessState.value = Resource.Success(null)
                _messMealsState.value = Resource.Success(emptyList())
                return@launch
            }

            _enrolledMessState.value = when (val messResult = messRepository.getMessDetails(messId)) {
                is Resource.Success -> Resource.Success(messResult.data)
                is Resource.Error -> Resource.Error(messResult.message)
                Resource.Loading -> Resource.Loading
            }

            _messMealsState.value = messRepository.getMealsForMess(messId)

            _paymentsState.value = paymentRepository.getUserPayments(uid)
        }
    }

    fun orderMeal(meal: Meal, quantity: Int, specialInstructions: String?) {
        val uid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _orderState.value = Resource.Loading
            _orderState.value = messRepository.placeOrder(uid, meal, quantity, specialInstructions)
            loadEnrolledSection()
        }
    }

    fun loadOrderHistory() {
        val uid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _historyState.value = Resource.Loading
            _historyState.value = messRepository.getUserOrders(uid)
        }
    }

    fun loadApprovedMesses() {
        viewModelScope.launch {
            _messesState.value = Resource.Loading
            _messesState.value = messRepository.getApprovedMesses()
        }
    }

    fun loadMenuForMess(messId: String) {
        viewModelScope.launch {
            _selectedMessMenuState.value = Resource.Loading
            _selectedMessMenuState.value = messRepository.getMealsForMess(messId)
        }
    }

    fun requestJoinMess(messId: String) {
        val uid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _joinState.value = Resource.Loading
            _joinState.value = messRepository.requestJoinMess(uid, messId)
        }
    }

    fun loadBanners() {
        viewModelScope.launch {
            _bannerState.value = Resource.Loading
            _bannerState.value = bannerRepository.getActiveBanners("user")
        }
    }
}
