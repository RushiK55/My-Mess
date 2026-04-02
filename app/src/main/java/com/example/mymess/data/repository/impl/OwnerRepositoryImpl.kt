package com.example.mymess.data.repository.impl

import com.example.mymess.core.Resource
import com.example.mymess.data.models.JoinRequestWithUser
import com.example.mymess.data.models.Meal
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.Order
import com.example.mymess.data.models.OwnerUserBillingDetails
import com.example.mymess.data.models.OwnerDashboardSummary
import com.example.mymess.data.models.PaymentRecord
import com.example.mymess.data.models.User
import com.example.mymess.data.models.UserRequest
import com.example.mymess.data.models.activeEnrolledMessId
import com.example.mymess.data.models.category
import com.example.mymess.data.models.source
import com.example.mymess.data.models.section
import com.example.mymess.data.models.BillLineItem
import com.example.mymess.data.models.UserBillPreview
import com.example.mymess.data.remote.FirebaseApi
import com.example.mymess.data.repository.OwnerRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

class OwnerRepositoryImpl @Inject constructor(
    private val api: FirebaseApi,
    private val gson: Gson,
) : OwnerRepository {

    override suspend fun getOwnerDashboardSummary(ownerUid: String): Resource<OwnerDashboardSummary> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Success(OwnerDashboardSummary(0, 0, 0.0))
            val users = fetchUsers().values
            val orders = fetchOrders().values
            val enrolled = users.count { it.activeEnrolledMessId() == mess.messId && it.status == "approved" }
            val pendingOrders = orders.count { it.messId == mess.messId && it.status == "pending" }

            val startOfDay = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

            val todaysEarnings = orders
                .filter { it.messId == mess.messId && it.status == "delivered" && it.updatedAt >= startOfDay }
                .sumOf { it.totalPrice }

            Resource.Success(OwnerDashboardSummary(enrolled, pendingOrders, todaysEarnings))
        }.getOrElse { Resource.Error(it.message ?: "Unable to load dashboard") }
    }

    override suspend fun getOwnerOrderRequests(ownerUid: String): Resource<List<Order>> {
        return getOrdersByStatuses(ownerUid, setOf("pending"))
    }

    override suspend fun getOwnerPendingOrders(ownerUid: String): Resource<List<Order>> {
        return getOrdersByStatuses(ownerUid, setOf("accepted", "preparing", "ready"))
    }

    override suspend fun updateOrderStatus(orderId: String, status: String): Resource<Unit> {
        return runCatching {
            val orderResponse = api.getObject("orders", orderId)
            if (!orderResponse.isSuccessful || orderResponse.body() == null) {
                return Resource.Error("Order not found")
            }

            val currentOrder = gson.fromJson(orderResponse.body(), Order::class.java)
            if (status == "accepted" && currentOrder.source() == "cloud" && currentOrder.paymentStatus != "paid") {
                return Resource.Error("Cloud order requires advance payment before acceptance")
            }
            val updatedOrder = currentOrder.copy(status = status, updatedAt = System.currentTimeMillis())
            val updateResponse = api.putData("orders", orderId, updatedOrder)
            if (updateResponse.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to update status")
        }.getOrElse { Resource.Error(it.message ?: "Failed to update order") }
    }

    override suspend fun rejectOrder(orderId: String): Resource<Unit> = updateOrderStatus(orderId, "cancelled")

    override suspend fun getPendingJoinRequests(ownerUid: String): Resource<List<JoinRequestWithUser>> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Success(emptyList())
            val requests = fetchRequests().values
                .filter { it.status == "pending" && it.messId == mess.messId }
                .sortedByDescending { it.createdAt }
            val users = fetchUsers()
            Resource.Success(requests.map { JoinRequestWithUser(it, users[it.userId]) })
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch join requests") }
    }

    override suspend fun approveJoinRequest(requestId: String): Resource<Unit> {
        return runCatching {
            val requestResponse = api.getObject("userRequests", requestId)
            if (!requestResponse.isSuccessful || requestResponse.body() == null) {
                return Resource.Error("Request not found")
            }

            val request = gson.fromJson(requestResponse.body(), UserRequest::class.java)
            val updatedRequest = request.copy(status = "approved")
            val requestUpdate = api.putData("userRequests", requestId, updatedRequest)
            if (!requestUpdate.isSuccessful) {
                return Resource.Error("Failed to update request")
            }

            val userResponse = api.getObject("users", request.userId)
            if (!userResponse.isSuccessful || userResponse.body() == null) {
                return Resource.Error("User not found")
            }
            val user = gson.fromJson(userResponse.body(), User::class.java)
            val updatedUser = user.copy(
                messId = request.messId,
                enrolledMessId = request.messId,
                status = if (user.status == "blocked") "approved" else user.status,
                approvedAt = System.currentTimeMillis(),
                blockedByMessId = null,
                blockReason = null,
            )
            val userUpdate = api.putData("users", request.userId, updatedUser)
            if (userUpdate.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to approve user")
        }.getOrElse { Resource.Error(it.message ?: "Failed to process request") }
    }

    override suspend fun rejectJoinRequest(requestId: String): Resource<Unit> {
        return runCatching {
            val response = api.deleteData("userRequests", requestId)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to reject request")
        }.getOrElse { Resource.Error(it.message ?: "Failed to reject request") }
    }

    override suspend fun getEnrolledUsers(ownerUid: String): Resource<List<User>> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Success(emptyList())
            val users = fetchUsers().values
                .filter { it.activeEnrolledMessId() == mess.messId && (it.status == "approved" || it.status == "blocked") }
                .sortedBy { it.name.lowercase() }
            Resource.Success(users)
        }.getOrElse { Resource.Error(it.message ?: "Unable to load enrolled users") }
    }

    override suspend fun getOwnerMessMeals(ownerUid: String): Resource<List<Meal>> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Success(emptyList())
            val meals = fetchMeals().values
                .filter { it.messId == mess.messId && it.section() == "mess" }
                .sortedBy { it.name.lowercase() }
            Resource.Success(meals)
        }.getOrElse { Resource.Error(it.message ?: "Unable to load mess meals") }
    }

    override suspend fun saveMessMeal(ownerUid: String, meal: Meal): Resource<Unit> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Error("Mess not found")
            val mealId = meal.mealId.ifBlank { UUID.randomUUID().toString() }
            val payload = meal.copy(mealId = mealId, messId = mess.messId, type = "mess", mealSection = "mess")
            val response = api.putData("meals", mealId, payload)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to save mess meal")
        }.getOrElse { Resource.Error(it.message ?: "Failed to save mess meal") }
    }

    override suspend fun getUserPaymentHistory(ownerUid: String, userId: String): Resource<List<PaymentRecord>> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Success(emptyList())
            val payments = fetchPayments().values
                .filter { it.userId == userId && it.messId == mess.messId }
                .sortedByDescending { it.dueDate }
            Resource.Success(payments)
        }.getOrElse { Resource.Error(it.message ?: "Unable to load payment history") }
    }

    override suspend fun getOwnerUserBillingDetails(ownerUid: String, userId: String): Resource<OwnerUserBillingDetails> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Error("Owner mess not found")
            val (monthStart, monthEnd) = currentMonthWindow()

            val orders = fetchOrders().values
                .filter {
                    it.userId == userId &&
                        it.messId == mess.messId &&
                        it.source() == "mess" &&
                        it.status == "accepted" &&
                        it.updatedAt in monthStart until monthEnd
                }

            val lineItems = orders
                .groupBy { it.mealName.ifBlank { "Meal" } }
                .map { (mealName, mealOrders) ->
                    BillLineItem(
                        mealName = mealName,
                        quantity = mealOrders.sumOf { it.quantity },
                        amount = mealOrders.sumOf { it.totalPrice },
                    )
                }
                .sortedByDescending { it.amount }

            val payments = fetchPayments().values
                .filter { it.userId == userId && it.messId == mess.messId && it.category() == "mess_bill" }
                .sortedByDescending { it.dueDate }

            val alreadyGenerated = payments.any { it.dueDate in monthStart until monthEnd }
            val monthName = java.text.SimpleDateFormat("MMMM yyyy").format(java.util.Date(monthStart))
            val totalAmount = lineItems.sumOf { it.amount }
            val canGenerate = lineItems.isNotEmpty() && !alreadyGenerated && totalAmount > 0
            val preview = UserBillPreview(
                items = lineItems,
                totalAmount = totalAmount,
                periodLabel = monthName,
                alreadyGenerated = alreadyGenerated,
                canGenerate = canGenerate,
            )

            Resource.Success(OwnerUserBillingDetails(paymentHistory = payments, billPreview = preview))
        }.getOrElse { Resource.Error(it.message ?: "Unable to load billing details") }
    }

    override suspend fun generateUserBill(ownerUid: String, userId: String): Resource<String> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Error("Owner mess not found")
            val user = fetchUsers()[userId] ?: return Resource.Error("User not found")
            val details = when (val result = getOwnerUserBillingDetails(ownerUid, userId)) {
                is Resource.Success -> result.data
                is Resource.Error -> return Resource.Error(result.message)
                Resource.Loading -> return Resource.Error("Unable to prepare bill")
            }
            val preview = details.billPreview
            if (preview.alreadyGenerated) {
                return Resource.Error("Bill already generated for ${preview.periodLabel}")
            }
            if (preview.items.isEmpty()) {
                return Resource.Error("No accepted mess meals found for ${preview.periodLabel}")
            }
            if (preview.totalAmount <= 0) {
                return Resource.Error("Bill total is invalid for ${preview.periodLabel}")
            }

            val (_, monthEnd) = currentMonthWindow()
            val billId = UUID.randomUUID().toString()
            val detailsText = preview.items.joinToString("\n") {
                "${it.mealName} x${it.quantity} = Rs ${it.amount}"
            }
            val record = PaymentRecord(
                paymentId = billId,
                userId = userId,
                userName = user.name,
                messId = mess.messId,
                amount = preview.totalAmount,
                status = "pending",
                dueDate = monthEnd - 1,
                paymentType = "mess_bill",
                billDetails = detailsText,
                createdAt = System.currentTimeMillis(),
            )
            val response = api.putData("payments", billId, record)
            if (response.isSuccessful) Resource.Success(billId) else Resource.Error("Failed to generate bill")
        }.getOrElse { Resource.Error(it.message ?: "Failed to generate bill") }
    }

    override suspend fun blockUser(ownerUid: String, userId: String, reason: String): Resource<Unit> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Error("Owner mess not found")
            val userResponse = api.getObject("users", userId)
            if (!userResponse.isSuccessful || userResponse.body() == null) {
                return Resource.Error("User not found")
            }
            val user = gson.fromJson(userResponse.body(), User::class.java)
            val updated = user.copy(
                status = "blocked",
                blockedByMessId = mess.messId,
                blockReason = reason.trim().ifBlank { "Blocked by owner" },
            )
            val response = api.putData("users", userId, updated)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to block user")
        }.getOrElse { Resource.Error(it.message ?: "Failed to block user") }
    }

    override suspend fun getOwnerMess(ownerUid: String): Resource<Mess> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Error("Mess not found")
            Resource.Success(mess)
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch mess") }
    }

    override suspend fun updateOwnerMess(ownerUid: String, mess: Mess): Resource<Unit> {
        return runCatching {
            val existing = getOwnerMessInternal(ownerUid) ?: return Resource.Error("Mess not found")
            val payload = mess.copy(messId = existing.messId, ownerId = ownerUid, createdAt = existing.createdAt)
            val response = api.putData("messes", existing.messId, payload)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to update mess")
        }.getOrElse { Resource.Error(it.message ?: "Failed to update mess") }
    }

    override suspend fun getOwnerCloudMeals(ownerUid: String): Resource<List<Meal>> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Success(emptyList())
            val meals = fetchMeals().values
                .filter { it.messId == mess.messId && it.section() == "cloud" }
                .sortedBy { it.name.lowercase() }
            Resource.Success(meals)
        }.getOrElse { Resource.Error(it.message ?: "Unable to load cloud meals") }
    }

    override suspend fun saveCloudMeal(ownerUid: String, meal: Meal): Resource<Unit> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Error("Mess not found")
            val mealId = meal.mealId.ifBlank { UUID.randomUUID().toString() }
            val payload = meal.copy(mealId = mealId, messId = mess.messId, type = "cloud", mealSection = "cloud")
            val response = api.putData("meals", mealId, payload)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to save cloud meal")
        }.getOrElse { Resource.Error(it.message ?: "Failed to save cloud meal") }
    }

    override suspend fun deleteMeal(mealId: String): Resource<Unit> {
        return runCatching {
            val response = api.deleteData("meals", mealId)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to delete meal")
        }.getOrElse { Resource.Error(it.message ?: "Failed to delete meal") }
    }

    private suspend fun getOrdersByStatuses(ownerUid: String, statuses: Set<String>): Resource<List<Order>> {
        return runCatching {
            val mess = getOwnerMessInternal(ownerUid) ?: return Resource.Success(emptyList())
            val orders = fetchOrders().values
                .filter { it.messId == mess.messId && statuses.contains(it.status) }
                .sortedByDescending { it.createdAt }
            Resource.Success(orders)
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch owner orders") }
    }

    private suspend fun getOwnerMessInternal(ownerUid: String): Mess? {
        val messes = fetchMesses().values
        return messes.firstOrNull { it.ownerId == ownerUid }
    }

    private suspend fun fetchMesses(): Map<String, Mess> {
        val response = api.getData("messes")
        if (!response.isSuccessful || response.body() == null) return emptyMap()
        val type = object : TypeToken<Map<String, Mess>>() {}.type
        return gson.fromJson(response.body(), type) ?: emptyMap()
    }

    private suspend fun fetchUsers(): Map<String, User> {
        val response = api.getData("users")
        if (!response.isSuccessful || response.body() == null) return emptyMap()
        val type = object : TypeToken<Map<String, User>>() {}.type
        return gson.fromJson(response.body(), type) ?: emptyMap()
    }

    private suspend fun fetchOrders(): Map<String, Order> {
        val response = api.getData("orders")
        if (!response.isSuccessful || response.body() == null) return emptyMap()
        val type = object : TypeToken<Map<String, Order>>() {}.type
        return gson.fromJson(response.body(), type) ?: emptyMap()
    }

    private suspend fun fetchRequests(): Map<String, UserRequest> {
        val response = api.getData("userRequests")
        if (!response.isSuccessful || response.body() == null) return emptyMap()
        val type = object : TypeToken<Map<String, UserRequest>>() {}.type
        return gson.fromJson(response.body(), type) ?: emptyMap()
    }

    private suspend fun fetchPayments(): Map<String, PaymentRecord> {
        val response = api.getData("payments")
        if (!response.isSuccessful || response.body() == null) return emptyMap()
        val type = object : TypeToken<Map<String, PaymentRecord>>() {}.type
        val raw: Map<String, PaymentRecord> = gson.fromJson(response.body(), type) ?: emptyMap()
        return raw.mapValues { (key, value) -> if (value.paymentId.isBlank()) value.copy(paymentId = key) else value }
    }

    private suspend fun fetchMeals(): Map<String, Meal> {
        val response = api.getData("meals")
        if (!response.isSuccessful || response.body() == null) return emptyMap()
        val type = object : TypeToken<Map<String, Meal>>() {}.type
        return gson.fromJson(response.body(), type) ?: emptyMap()
    }

    private fun currentMonthWindow(): Pair<Long, Long> {
        val monthStart = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthEnd = monthStart.clone() as Calendar
        monthEnd.add(Calendar.MONTH, 1)
        return monthStart.timeInMillis to monthEnd.timeInMillis
    }
}
