package com.example.mymess.data.models

data class User(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "user",
    val status: String = "pending",
    val approvedBy: String? = null,
    val password: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val messId: String? = null,
    val enrolledMessId: String? = null,
    val approvedAt: Long? = null,
    val blockedByMessId: String? = null,
    val blockReason: String? = null,
)

data class Mess(
    val messId: String = "",
    val ownerId: String = "",
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val contact: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val isApproved: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

data class Meal(
    val mealId: String = "",
    val messId: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String? = null,
    val isAvailable: Boolean = true,
    val type: String = "mess",
    val mealSection: String = "",
)

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val messId: String? = null,
    val mealId: String = "",
    val mealName: String = "",
    val quantity: Int = 1,
    val totalPrice: Double = 0.0,
    val status: String = "pending",
    val orderType: String = "mess",
    val orderSource: String = "",
    val ownerId: String? = null,
    val specialInstructions: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val paymentStatus: String = "pending",
    val paymentMethod: String? = null,
    val paymentId: String? = null,
)

data class UserRequest(
    val requestId: String = "",
    val userId: String = "",
    val messId: String = "",
    val status: String = "pending",
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

data class Banner(
    val bannerId: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val targetRole: String = "all",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String? = null,
)

data class PaymentRecord(
    val paymentId: String = "",
    val userId: String = "",
    val messId: String = "",
    val amount: Double = 0.0,
    val status: String = "pending",
    val dueDate: Long = System.currentTimeMillis(),
    val paidAt: Long? = null,
    val paymentMethod: String? = null,
    val userName: String? = null,
    val paymentType: String = "", // "mess_bill" or "cloud_advance"
    val orderId: String? = null,
    val mealName: String? = null,
    val billDetails: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

data class BillLineItem(
    val mealName: String,
    val quantity: Int,
    val amount: Double,
)

data class UserBillPreview(
    val items: List<BillLineItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val periodLabel: String = "",
    val alreadyGenerated: Boolean = false,
    val canGenerate: Boolean = false,
)

data class OwnerUserBillingDetails(
    val paymentHistory: List<PaymentRecord> = emptyList(),
    val billPreview: UserBillPreview = UserBillPreview(),
)

data class JoinRequestWithUser(
    val request: UserRequest,
    val user: User?,
)

data class PushResponse(
    val name: String,
)

data class OwnerDashboardSummary(
    val enrolledUsers: Int,
    val pendingOrders: Int,
    val todaysEarnings: Double,
)

data class MetricPoint(
    val label: String,
    val value: Double,
)

data class TopMealMetric(
    val mealName: String,
    val orders: Int,
)

data class OwnerAnalyticsInsights(
    val summary: AnalyticsSummary,
    val ordersPerDay: List<MetricPoint>,
    val revenueTrend: List<MetricPoint>,
    val topMeals: List<TopMealMetric>,
    val userGrowth: List<MetricPoint>,
)

data class AdminAnalyticsInsights(
    val summary: AnalyticsSummary,
    val ordersPerDay: List<MetricPoint>,
    val revenueTrend: List<MetricPoint>,
    val sourceRevenue: List<MetricPoint>,
    val userGrowth: List<MetricPoint>,
)

fun User.activeEnrolledMessId(): String? = enrolledMessId ?: messId

fun Meal.section(): String {
    val sectionValue = mealSection.ifBlank { type }
    return if (sectionValue.equals("cloud", ignoreCase = true)) "cloud" else "mess"
}

fun Order.source(): String {
    val sourceValue = orderSource.ifBlank { orderType }
    return if (sourceValue.equals("cloud", ignoreCase = true)) "cloud" else "mess"
}

fun PaymentRecord.category(): String {
    if (paymentType.equals("cloud_advance", ignoreCase = true)) return "cloud_advance"
    if (paymentType.equals("mess_bill", ignoreCase = true)) return "mess_bill"
    return if (!orderId.isNullOrBlank()) "cloud_advance" else "mess_bill"
}

