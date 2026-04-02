package com.example.mymess.data.repository

import com.example.mymess.core.Resource
import com.example.mymess.data.models.Meal
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.Order
import com.example.mymess.data.models.User

interface MessRepository {
    suspend fun getCloudMeals(): Resource<List<Meal>>
    suspend fun getUserProfile(userId: String): Resource<User>
    suspend fun getMessDetails(messId: String): Resource<Mess>
    suspend fun getMealsForMess(messId: String, includeCloud: Boolean = false): Resource<List<Meal>>
    suspend fun placeOrder(userId: String, meal: Meal, quantity: Int, specialInstructions: String?): Resource<String>
    suspend fun getUserOrders(userId: String): Resource<List<Order>>
    suspend fun getApprovedMesses(): Resource<List<Mess>>
    suspend fun requestJoinMess(userId: String, messId: String): Resource<Unit>
}
