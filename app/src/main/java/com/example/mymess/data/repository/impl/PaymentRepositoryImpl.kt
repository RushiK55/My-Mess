package com.example.mymess.data.repository.impl

import com.example.mymess.core.Resource
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.Order
import com.example.mymess.data.models.PaymentRecord
import com.example.mymess.data.models.User
import com.example.mymess.data.models.activeEnrolledMessId
import com.example.mymess.data.models.category
import com.example.mymess.data.remote.FirebaseApi
import com.example.mymess.data.repository.PaymentRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

class PaymentRepositoryImpl @Inject constructor(
    private val api: FirebaseApi,
    private val gson: Gson,
) : PaymentRepository {

    override suspend fun getUserPayments(userId: String): Resource<List<PaymentRecord>> {
        return runCatching {
            val payments = fetchPayments().values
                .filter { it.userId == userId }
                .sortedByDescending { it.dueDate }
            Resource.Success(payments)
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch payments") }
    }

    override suspend fun getOwnerPayments(ownerUid: String): Resource<List<PaymentRecord>> {
        return runCatching {
            val messResponse = api.getData("messes")
            if (!messResponse.isSuccessful || messResponse.body() == null) {
                return Resource.Success(emptyList())
            }
            val messType = object : TypeToken<Map<String, Mess>>() {}.type
            val messes: Map<String, Mess> = gson.fromJson(messResponse.body(), messType) ?: emptyMap()
            val ownerMessIds = messes.values.filter { it.ownerId == ownerUid }.map { it.messId }.toSet()

            val usersResponse = api.getData("users")
            val users: Map<String, User> = if (usersResponse.isSuccessful && usersResponse.body() != null) {
                val userType = object : TypeToken<Map<String, User>>() {}.type
                gson.fromJson(usersResponse.body(), userType) ?: emptyMap()
            } else {
                emptyMap()
            }

            val ownerPayments = fetchPayments().values
                .filter { ownerMessIds.contains(it.messId) }
                .map { payment -> payment.copy(userName = users[payment.userId]?.name) }
                .sortedByDescending { it.dueDate }
            Resource.Success(ownerPayments)
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch owner payments") }
    }

    override suspend fun submitPayment(paymentId: String, method: String): Resource<Unit> {
        return runCatching {
            val currentResponse = api.getObject("payments", paymentId)
            if (!currentResponse.isSuccessful || currentResponse.body() == null) {
                return Resource.Error("Payment record not found")
            }
            val current = gson.fromJson(currentResponse.body(), PaymentRecord::class.java)
            if (current.category() == "cloud_advance") {
                return Resource.Error("Cloud meal advance payment is confirmed by owner")
            }
            if (current.status == "paid") {
                return Resource.Error("Payment is already marked as paid")
            }
            val updated = current.copy(status = "payment_submitted", paymentMethod = method)
            val updateResponse = api.putData("payments", paymentId, updated)
            if (updateResponse.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to submit payment")
        }.getOrElse { Resource.Error(it.message ?: "Failed to submit payment") }
    }

    override suspend fun markPaymentPaid(paymentId: String): Resource<Unit> {
        return runCatching {
            val currentResponse = api.getObject("payments", paymentId)
            if (!currentResponse.isSuccessful || currentResponse.body() == null) {
                return Resource.Error("Payment record not found")
            }
            val current = gson.fromJson(currentResponse.body(), PaymentRecord::class.java)
            val normalizedPaymentId = if (current.paymentId.isBlank()) paymentId else current.paymentId
            val now = System.currentTimeMillis()
            val updated = current.copy(paymentId = normalizedPaymentId, status = "paid", paidAt = now)
            val updateResponse = api.putData("payments", paymentId, updated)
            if (!updateResponse.isSuccessful) {
                return Resource.Error("Failed to update payment")
            }

            val linkedOrderId = updated.orderId
            if (!linkedOrderId.isNullOrBlank()) {
                val orderResponse = api.getObject("orders", linkedOrderId)
                if (orderResponse.isSuccessful && orderResponse.body() != null) {
                    val order = gson.fromJson(orderResponse.body(), Order::class.java)
                    val updatedOrder = order.copy(paymentStatus = "paid", paymentId = normalizedPaymentId, updatedAt = now)
                    api.putData("orders", linkedOrderId, updatedOrder)
                }
            }

            Resource.Success(Unit)
        }.getOrElse { Resource.Error(it.message ?: "Failed to update payment") }
    }

    override suspend fun generateMonthlyBills(ownerUid: String): Resource<Int> {
        return runCatching {
            val messResponse = api.getData("messes")
            if (!messResponse.isSuccessful || messResponse.body() == null) {
                return Resource.Error("Mess not found")
            }
            val messType = object : TypeToken<Map<String, Mess>>() {}.type
            val messes: Map<String, Mess> = gson.fromJson(messResponse.body(), messType) ?: emptyMap()
            val ownerMess = messes.values.firstOrNull { it.ownerId == ownerUid }
                ?: return Resource.Error("Owner mess not found")

            val usersResponse = api.getData("users")
            if (!usersResponse.isSuccessful || usersResponse.body() == null) {
                return Resource.Success(0)
            }
            val userType = object : TypeToken<Map<String, User>>() {}.type
            val users: Map<String, User> = gson.fromJson(usersResponse.body(), userType) ?: emptyMap()
            val enrolledUsers = users.values.filter { it.activeEnrolledMessId() == ownerMess.messId && it.status == "approved" }

            val paymentMap = fetchPayments()

            val monthStart = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val monthStartMillis = monthStart.timeInMillis
            val monthEnd = monthStart.clone() as Calendar
            monthEnd.add(Calendar.MONTH, 1)
            val monthEndMillis = monthEnd.timeInMillis

            var createdCount = 0
            enrolledUsers.forEach { user ->
                val alreadyExists = paymentMap.values.any {
                    it.category() == "mess_bill" &&
                        it.userId == user.uid &&
                        it.messId == ownerMess.messId &&
                        it.dueDate in monthStartMillis until monthEndMillis
                }
                if (!alreadyExists) {
                    val paymentId = UUID.randomUUID().toString()
                    val record = PaymentRecord(
                        paymentId = paymentId,
                        userId = user.uid,
                        userName = user.name,
                        messId = ownerMess.messId,
                        amount = 3000.0,
                        status = "pending",
                        dueDate = monthEndMillis - 1,
                        paymentType = "mess_bill",
                        createdAt = System.currentTimeMillis(),
                    )
                    val saveResponse = api.putData("payments", paymentId, record)
                    if (saveResponse.isSuccessful) createdCount++
                }
            }
            Resource.Success(createdCount)
        }.getOrElse { Resource.Error(it.message ?: "Failed to generate monthly bills") }
    }

    private suspend fun fetchPayments(): Map<String, PaymentRecord> {
        val response = runCatching { api.getData("payments") }.getOrNull() ?: return emptyMap()
        if (!response.isSuccessful || response.body() == null) return emptyMap()
        val type = object : TypeToken<Map<String, PaymentRecord>>() {}.type
        val raw: Map<String, PaymentRecord> = runCatching { gson.fromJson<Map<String, PaymentRecord>>(response.body(), type) }
            .getOrElse { emptyMap() }

        // Normalize paymentId from Firebase key for older records that don't persist it.
        return raw.mapValues { (key, value) -> if (value.paymentId.isBlank()) value.copy(paymentId = key) else value }
    }
}
