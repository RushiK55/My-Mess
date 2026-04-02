package com.example.mymess.data.models

data class AnalyticsSummary(
    val totalOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val pendingOrders: Int = 0,
    val deliveredOrders: Int = 0,
    val totalUsers: Int = 0,
    val totalOwners: Int = 0,
    val userGrowthCount: Int = 0,
    val ownerRegistrationsCount: Int = 0,
    val orderVolumeThisMonth: Int = 0,
    val messRevenue: Double = 0.0,
    val cloudRevenue: Double = 0.0,
)

