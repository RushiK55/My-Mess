package com.example.mymess.data.repository

import com.example.mymess.core.Resource
import com.example.mymess.data.models.JoinRequestWithUser
import com.example.mymess.data.models.Meal
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.Order
import com.example.mymess.data.models.OwnerUserBillingDetails
import com.example.mymess.data.models.OwnerDashboardSummary
import com.example.mymess.data.models.PaymentRecord
import com.example.mymess.data.models.User

interface OwnerRepository {
    suspend fun getOwnerDashboardSummary(ownerUid: String): Resource<OwnerDashboardSummary>
    suspend fun getOwnerOrderRequests(ownerUid: String): Resource<List<Order>>
    suspend fun getOwnerPendingOrders(ownerUid: String): Resource<List<Order>>
    suspend fun updateOrderStatus(orderId: String, status: String): Resource<Unit>
    suspend fun rejectOrder(orderId: String): Resource<Unit>

    suspend fun getPendingJoinRequests(ownerUid: String): Resource<List<JoinRequestWithUser>>
    suspend fun approveJoinRequest(requestId: String): Resource<Unit>
    suspend fun rejectJoinRequest(requestId: String): Resource<Unit>

    suspend fun getEnrolledUsers(ownerUid: String): Resource<List<User>>
    suspend fun getUserPaymentHistory(ownerUid: String, userId: String): Resource<List<PaymentRecord>>
    suspend fun getOwnerUserBillingDetails(ownerUid: String, userId: String): Resource<OwnerUserBillingDetails>
    suspend fun generateUserBill(ownerUid: String, userId: String): Resource<String>
    suspend fun blockUser(ownerUid: String, userId: String, reason: String): Resource<Unit>

    suspend fun getOwnerMess(ownerUid: String): Resource<Mess>
    suspend fun updateOwnerMess(ownerUid: String, mess: Mess): Resource<Unit>

    suspend fun getOwnerMessMeals(ownerUid: String): Resource<List<Meal>>
    suspend fun saveMessMeal(ownerUid: String, meal: Meal): Resource<Unit>
    suspend fun getOwnerCloudMeals(ownerUid: String): Resource<List<Meal>>
    suspend fun saveCloudMeal(ownerUid: String, meal: Meal): Resource<Unit>
    suspend fun deleteMeal(mealId: String): Resource<Unit>
}
