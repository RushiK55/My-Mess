package com.example.mymess.presentation.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mymess.core.Resource
import com.example.mymess.core.SessionManager
import com.example.mymess.data.models.Meal
import com.example.mymess.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class OwnerMessMealsViewModel @Inject constructor(
    private val ownerRepository: OwnerRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val _mealsState = MutableStateFlow<Resource<List<Meal>>>(Resource.Loading)
    val mealsState: StateFlow<Resource<List<Meal>>> = _mealsState

    private val _actionState = MutableStateFlow<Resource<Unit>?>(null)
    val actionState: StateFlow<Resource<Unit>?> = _actionState

    fun load() {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _mealsState.value = Resource.Loading
            _mealsState.value = ownerRepository.getOwnerMessMeals(ownerUid)
        }
    }

    fun saveMeal(mealId: String?, name: String, description: String, price: Double, imageUrl: String?) {
        val ownerUid = sessionManager.getUid() ?: return
        if (name.isBlank() || price <= 0.0) {
            _actionState.value = Resource.Error("Enter valid meal name and price")
            return
        }
        val meal = Meal(
            mealId = mealId.orEmpty(),
            name = name.trim(),
            description = description.trim(),
            price = price,
            imageUrl = imageUrl?.trim()?.ifBlank { null },
            type = "mess",
            mealSection = "mess",
            isAvailable = true,
        )
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = ownerRepository.saveMessMeal(ownerUid, meal)
            load()
        }
    }

    fun toggleAvailability(meal: Meal) {
        val ownerUid = sessionManager.getUid() ?: return
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = ownerRepository.saveMessMeal(ownerUid, meal.copy(isAvailable = !meal.isAvailable))
            load()
        }
    }

    fun deleteMeal(mealId: String) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading
            _actionState.value = ownerRepository.deleteMeal(mealId)
            load()
        }
    }
}

