package com.example.mymess.presentation.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.Resource
import com.example.mymess.data.models.AdminAnalyticsInsights
import com.example.mymess.data.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AdminAnalyticsViewModel @Inject constructor(
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<Resource<AdminAnalyticsInsights>>(Resource.Loading)
    val state: StateFlow<Resource<AdminAnalyticsInsights>> = _state

    fun load() {
        viewModelScope.launch {
            _state.value = Resource.Loading
            _state.value = analyticsRepository.getAdminAnalyticsInsights()
        }
    }
}

