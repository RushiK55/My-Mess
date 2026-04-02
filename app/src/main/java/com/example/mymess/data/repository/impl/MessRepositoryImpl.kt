package com.example.mymess.data.repository.impl

import com.example.mymess.core.Resource
import com.example.mymess.data.models.Meal
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.Order
import com.example.mymess.data.models.PaymentRecord
import com.example.mymess.data.models.SeedData
import com.example.mymess.data.models.User
import com.example.mymess.data.models.UserRequest
import com.example.mymess.data.models.activeEnrolledMessId
import com.example.mymess.data.models.section
import com.example.mymess.data.remote.FirebaseApi
import com.example.mymess.data.repository.MessRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import javax.inject.Inject

class MessRepositoryImpl @Inject constructor(
    private val api: FirebaseApi,
    private val gson: Gson,
) : MessRepository {

    override suspend fun getCloudMeals(): Resource<List<Meal>> {
        return runCatching {
            val response = api.getData("meals")
            if (!response.isSuccessful || response.body() == null) {
                return Resource.Success(SeedData.defaultCloudMeals)
            }
            val type = object : TypeToken<Map<String, Meal>>() {}.type
            val meals: Map<String, Meal> = gson.fromJson(response.body(), type) ?: emptyMap()
            val cloudMeals = meals.values.filter { it.section() == "cloud" && it.isAvailable }
            Resource.Success(if (cloudMeals.isEmpty()) SeedData.defaultCloudMeals else cloudMeals)
        }.getOrElse { Resource.Success(SeedData.defaultCloudMeals) }
    }

    override suspend fun getUserProfile(userId: String): Resource<User> {
        return runCatching {
            val response = api.getObject("users", userId)
            if (!response.isSuccessful || response.body() == null) {
                return Resource.Error("Unable to fetch user profile")
            }
            val user = gson.fromJson(response.body(), User::class.java)
            Resource.Success(user)
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch user profile") }
    }

    override suspend fun getMessDetails(messId: String): Resource<Mess> {
        return runCatching {
            val response = api.getObject("messes", messId)
            if (!response.isSuccessful || response.body() == null) {
                return Resource.Error("Mess details not found")
            }
            val mess = gson.fromJson(response.body(), Mess::class.java)
            Resource.Success(mess)
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch mess details") }
    }

    override suspend fun getMealsForMess(messId: String, includeCloud: Boolean): Resource<List<Meal>> {
        return runCatching {
            val response = api.getData("meals")
            if (!response.isSuccessful || response.body() == null) {
                return Resource.Success(emptyList())
            }
            val type = object : TypeToken<Map<String, Meal>>() {}.type
            val meals: Map<String, Meal> = gson.fromJson(response.body(), type) ?: emptyMap()
            val items = meals.values
                .filter { meal ->
                    meal.messId == messId && meal.isAvailable && (includeCloud || meal.section() == "mess")
                }
                .sortedBy { it.name }
            Resource.Success(items)
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch menu") }
    }

    override suspend fun placeOrder(userId: String, meal: Meal, quantity: Int, specialInstructions: String?): Resource<String> {
        return runCatching {
            val userResponse = api.getObject("users", userId)
            if (!userResponse.isSuccessful || userResponse.body() == null) {
                return Resource.Error("Unable to verify user account")
            }
            val user = gson.fromJson(userResponse.body(), User::class.java)
            val isMessMeal = meal.section() == "mess"
            val activeEnrollment = user.activeEnrolledMessId()
            val isBlockedForMess = user.status == "blocked" && (user.blockedByMessId == meal.messId || activeEnrollment == meal.messId)
            if (isBlockedForMess) {
                return Resource.Error(user.blockReason ?: "You are blocked from ordering in this mess")
            }

            if (isMessMeal && activeEnrollment != meal.messId) {
                return Resource.Error("You can order mess meals only from your enrolled mess")
            }

            val ownerId = runCatching {
                val messResponse = api.getObject("messes", meal.messId)
                if (!messResponse.isSuccessful || messResponse.body() == null) null
                else gson.fromJson(messResponse.body(), Mess::class.java).ownerId
            }.getOrNull()

            val normalizedSource = if (isMessMeal) "mess" else "cloud"

            val orderId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val paymentId = if (normalizedSource == "cloud") UUID.randomUUID().toString() else null
            val order = Order(
                orderId = orderId,
                userId = userId,
                messId = meal.messId,
                mealId = meal.mealId,
                mealName = meal.name,
                quantity = quantity,
                totalPrice = meal.price * quantity,
                status = "pending",
                orderType = normalizedSource,
                orderSource = normalizedSource,
                ownerId = ownerId,
                specialInstructions = specialInstructions?.takeIf { it.isNotBlank() },
                createdAt = now,
                updatedAt = now,
                paymentStatus = if (normalizedSource == "cloud") "pending" else "pending",
                paymentId = paymentId,
            )

            val response = api.putData("orders", orderId, order)
            if (!response.isSuccessful) {
                return Resource.Error("Order placement failed")
            }

            if (normalizedSource == "cloud" && paymentId != null) {
                val payment = PaymentRecord(
                    paymentId = paymentId,
                    userId = userId,
                    messId = meal.messId,
                    amount = meal.price * quantity,
                    status = "pending",
                    dueDate = now,
                    paymentType = "cloud_advance",
                    orderId = orderId,
                    mealName = meal.name,
                    createdAt = now,
                )
                api.putData("payments", paymentId, payment)
            }

            Resource.Success(orderId)
        }.getOrElse { Resource.Error(it.message ?: "Order placement failed") }
    }

    override suspend fun getUserOrders(userId: String): Resource<List<Order>> {
        return runCatching {
            val response = api.getData("orders")
            if (!response.isSuccessful || response.body() == null) {
                return Resource.Success(emptyList())
            }

            val type = object : TypeToken<Map<String, Order>>() {}.type
            val orders: Map<String, Order> = gson.fromJson(response.body(), type) ?: emptyMap()
            val userOrders = orders.values
                .filter { it.userId == userId }
                .sortedByDescending { it.createdAt }

            Resource.Success(userOrders)
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch orders") }
    }

    override suspend fun getApprovedMesses(): Resource<List<Mess>> {
        return runCatching {
            val response = api.getData("messes")
            if (!response.isSuccessful || response.body() == null) {
                return Resource.Success(emptyList())
            }

            val type = object : TypeToken<Map<String, Mess>>() {}.type
            val messes: Map<String, Mess> = gson.fromJson(response.body(), type) ?: emptyMap()
            val approvedMesses = messes.values.filter { it.isApproved }.sortedBy { it.name }
            Resource.Success(approvedMesses)
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch messes") }
    }

    override suspend fun requestJoinMess(userId: String, messId: String): Resource<Unit> {
        return runCatching {
            val requestResponse = runCatching { api.getData("userRequests") }.getOrNull()
            val requests: Map<String, UserRequest> = if (requestResponse?.isSuccessful == true && requestResponse.body() != null) {
                runCatching {
                val type = object : TypeToken<Map<String, UserRequest>>() {}.type
                    gson.fromJson<Map<String, UserRequest>>(requestResponse.body(), type) ?: emptyMap()
                }.getOrElse { emptyMap() }
            } else {
                emptyMap()
            }

            val duplicate = requests.values.any {
                it.userId == userId && it.messId == messId && (it.status == "pending" || it.status == "approved")
            }
            if (duplicate) {
                return Resource.Error("Join request already exists")
            }

            val requestId = UUID.randomUUID().toString()
            val request = UserRequest(
                requestId = requestId,
                userId = userId,
                messId = messId,
                status = "pending",
                createdAt = System.currentTimeMillis(),
            )
            val createResponse = api.putData("userRequests", requestId, request)
            if (createResponse.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to send join request")
        }.getOrElse { Resource.Error(it.message ?: "Failed to send join request") }
    }
}
