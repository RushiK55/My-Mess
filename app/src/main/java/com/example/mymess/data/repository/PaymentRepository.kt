package com.example.mymess.data.repository

import com.example.mymess.core.Resource
import com.example.mymess.data.models.PaymentRecord

interface PaymentRepository {
    suspend fun getUserPayments(userId: String): Resource<List<PaymentRecord>>
    suspend fun getOwnerPayments(ownerUid: String): Resource<List<PaymentRecord>>
    suspend fun submitPayment(paymentId: String, method: String): Resource<Unit>
    suspend fun markPaymentPaid(paymentId: String): Resource<Unit>
    suspend fun generateMonthlyBills(ownerUid: String): Resource<Int>
}
