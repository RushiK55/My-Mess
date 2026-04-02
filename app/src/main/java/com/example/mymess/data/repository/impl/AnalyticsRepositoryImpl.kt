package com.example.mymess.data.repository.impl

import com.example.mymess.core.Resource
import com.example.mymess.data.models.AdminAnalyticsInsights
import com.example.mymess.data.models.AnalyticsSummary
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.MetricPoint
import com.example.mymess.data.models.Order
import com.example.mymess.data.models.OwnerAnalyticsInsights
import com.example.mymess.data.models.TopMealMetric
import com.example.mymess.data.models.User
import com.example.mymess.data.models.activeEnrolledMessId
import com.example.mymess.data.models.source
import com.example.mymess.data.remote.FirebaseApi
import com.example.mymess.data.repository.AnalyticsRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class AnalyticsRepositoryImpl @Inject constructor(
    private val api: FirebaseApi,
    private val gson: Gson,
) : AnalyticsRepository {

    override suspend fun getOwnerAnalytics(ownerUid: String): Resource<AnalyticsSummary> {
        return when (val insights = getOwnerAnalyticsInsights(ownerUid)) {
            is Resource.Success -> Resource.Success(insights.data.summary)
            is Resource.Error -> Resource.Error(insights.message)
            Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getOwnerAnalyticsInsights(ownerUid: String): Resource<OwnerAnalyticsInsights> {
        return runCatching {
            val messesResponse = api.getData("messes")
            if (!messesResponse.isSuccessful || messesResponse.body() == null) {
                return Resource.Success(emptyInsights())
            }

            val messType = object : TypeToken<Map<String, Mess>>() {}.type
            val messes: Map<String, Mess> = gson.fromJson(messesResponse.body(), messType) ?: emptyMap()
            val ownerMessIds = messes.values.filter { it.ownerId == ownerUid }.map { it.messId }.toSet()
            if (ownerMessIds.isEmpty()) {
                return Resource.Success(emptyInsights())
            }

            val ordersResponse = api.getData("orders")
            val orderMap: Map<String, Order> = if (ordersResponse.isSuccessful && ordersResponse.body() != null) {
                val orderType = object : TypeToken<Map<String, Order>>() {}.type
                gson.fromJson(ordersResponse.body(), orderType) ?: emptyMap()
            } else {
                emptyMap()
            }
            val ownerOrders = orderMap.values.filter { it.messId != null && ownerMessIds.contains(it.messId) }

            val usersResponse = api.getData("users")
            val users: Map<String, User> = if (usersResponse.isSuccessful && usersResponse.body() != null) {
                val userType = object : TypeToken<Map<String, User>>() {}.type
                gson.fromJson(usersResponse.body(), userType) ?: emptyMap()
            } else {
                emptyMap()
            }
            val enrolledUsers = users.values.filter { it.activeEnrolledMessId()?.let(ownerMessIds::contains) == true }

            val summary = buildSummary(ownerOrders)
            val insights = OwnerAnalyticsInsights(
                summary = summary,
                ordersPerDay = buildOrdersPerDay(ownerOrders, 7),
                revenueTrend = buildRevenueTrend(ownerOrders, 7),
                topMeals = buildTopMeals(ownerOrders, 5),
                userGrowth = buildUserGrowth(enrolledUsers, 6),
            )
            Resource.Success(insights)
        }.getOrElse { Resource.Error(it.message ?: "Unable to load analytics") }
    }

    override suspend fun getAdminAnalytics(): Resource<AnalyticsSummary> {
        return when (val insights = getAdminAnalyticsInsights()) {
            is Resource.Success -> Resource.Success(insights.data.summary)
            is Resource.Error -> Resource.Error(insights.message)
            Resource.Loading -> Resource.Loading
        }
    }

    override suspend fun getAdminAnalyticsInsights(): Resource<AdminAnalyticsInsights> {
        return runCatching {
            val ordersResponse = api.getData("orders")
            val orders: List<Order> = if (ordersResponse.isSuccessful && ordersResponse.body() != null) {
                val orderType = object : TypeToken<Map<String, Order>>() {}.type
                val orderMap: Map<String, Order> = gson.fromJson(ordersResponse.body(), orderType) ?: emptyMap()
                orderMap.values.toList()
            } else {
                emptyList()
            }

            val usersResponse = api.getData("users")
            val users: List<User> = if (usersResponse.isSuccessful && usersResponse.body() != null) {
                val userType = object : TypeToken<Map<String, User>>() {}.type
                val userMap: Map<String, User> = gson.fromJson(usersResponse.body(), userType) ?: emptyMap()
                userMap.values.toList()
            } else {
                emptyList()
            }

            val now = System.currentTimeMillis()
            val last30Days = now - (30L * 24L * 60L * 60L * 1000L)
            val monthStart = monthStartMillis()

            val base = buildSummary(orders)
            val deliveredOrders = orders.filter { it.status == "delivered" }
            val messRevenue = deliveredOrders.filter { it.source() == "mess" }.sumOf { it.totalPrice }
            val cloudRevenue = deliveredOrders.filter { it.source() == "cloud" }.sumOf { it.totalPrice }
            val summary =
                base.copy(
                    totalUsers = users.count { it.role == "user" },
                    totalOwners = users.count { it.role == "owner" },
                    userGrowthCount = users.count { it.role == "user" && it.createdAt >= last30Days },
                    ownerRegistrationsCount = users.count { it.role == "owner" && it.createdAt >= last30Days },
                    orderVolumeThisMonth = orders.count { it.createdAt >= monthStart },
                    messRevenue = messRevenue,
                    cloudRevenue = cloudRevenue,
                )

            Resource.Success(
                AdminAnalyticsInsights(
                    summary = summary,
                    ordersPerDay = buildOrdersPerDay(orders, 7),
                    revenueTrend = buildRevenueTrend(orders, 7),
                    sourceRevenue = listOf(
                        MetricPoint("Mess", messRevenue),
                        MetricPoint("Cloud", cloudRevenue),
                    ),
                    userGrowth = buildUserGrowth(users.filter { it.role == "user" }, 6),
                ),
            )
        }.getOrElse { Resource.Error(it.message ?: "Unable to load analytics") }
    }

    private fun monthStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun buildSummary(orders: List<Order>): AnalyticsSummary {
        val total = orders.size
        val revenue = orders.filter { it.status == "delivered" }.sumOf { it.totalPrice }
        val pending = orders.count { it.status == "pending" || it.status == "accepted" || it.status == "preparing" || it.status == "ready" }
        val delivered = orders.count { it.status == "delivered" }
        return AnalyticsSummary(total, revenue, pending, delivered)
    }

    private fun buildOrdersPerDay(orders: List<Order>, days: Int): List<MetricPoint> {
        val dayBuckets = mutableMapOf<String, Double>()
        val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
        repeat(days) { index ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -(days - index - 1))
            val key = formatter.format(cal.time)
            dayBuckets[key] = 0.0
        }

        orders.forEach { order ->
            val key = formatter.format(Date(order.createdAt))
            if (dayBuckets.containsKey(key)) {
                dayBuckets[key] = (dayBuckets[key] ?: 0.0) + 1.0
            }
        }

        return dayBuckets.map { MetricPoint(it.key, it.value) }
    }

    private fun buildRevenueTrend(orders: List<Order>, days: Int): List<MetricPoint> {
        val dayBuckets = mutableMapOf<String, Double>()
        val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
        repeat(days) { index ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -(days - index - 1))
            val key = formatter.format(cal.time)
            dayBuckets[key] = 0.0
        }

        orders.filter { it.status == "delivered" }.forEach { order ->
            val key = formatter.format(Date(order.updatedAt))
            if (dayBuckets.containsKey(key)) {
                dayBuckets[key] = (dayBuckets[key] ?: 0.0) + order.totalPrice
            }
        }

        return dayBuckets.map { MetricPoint(it.key, it.value) }
    }

    private fun buildTopMeals(orders: List<Order>, limit: Int): List<TopMealMetric> {
        return orders
            .groupBy { it.mealName.ifBlank { "Unknown" } }
            .map { TopMealMetric(it.key, it.value.sumOf { order -> order.quantity }) }
            .sortedByDescending { it.orders }
            .take(limit)
    }

    private fun buildUserGrowth(users: List<User>, months: Int): List<MetricPoint> {
        val buckets = linkedMapOf<String, Double>()
        val formatter = SimpleDateFormat("MMM", Locale.getDefault())
        repeat(months) { index ->
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -(months - index - 1))
            cal.set(Calendar.DAY_OF_MONTH, 1)
            val key = formatter.format(cal.time)
            buckets[key] = 0.0
        }

        users.forEach { user ->
            val key = formatter.format(Date(user.createdAt))
            if (buckets.containsKey(key)) {
                buckets[key] = (buckets[key] ?: 0.0) + 1.0
            }
        }

        return buckets.map { MetricPoint(it.key, it.value) }
    }

    private fun emptyInsights(): OwnerAnalyticsInsights {
        return OwnerAnalyticsInsights(
            summary = AnalyticsSummary(),
            ordersPerDay = emptyList(),
            revenueTrend = emptyList(),
            topMeals = emptyList(),
            userGrowth = emptyList(),
        )
    }
}
