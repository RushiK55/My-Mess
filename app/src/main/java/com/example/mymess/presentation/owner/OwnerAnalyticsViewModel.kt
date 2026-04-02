package com.example.mymess.presentation.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.Resource
import com.example.mymess.core.SessionManager
import com.example.mymess.data.models.OwnerAnalyticsInsights
import com.example.mymess.data.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OwnerAnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _state = MutableStateFlow<Resource<OwnerAnalyticsInsights>>(Resource.Loading)
    val state: StateFlow<Resource<OwnerAnalyticsInsights>> = _state

    fun load() {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _state.value = Resource.Loading
            _state.value = analyticsRepository.getOwnerAnalyticsInsights(ownerUid)
        }
    }
}
