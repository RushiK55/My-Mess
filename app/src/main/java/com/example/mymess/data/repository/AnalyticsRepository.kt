package com.example.mymess.data.repository

import com.example.mymess.core.Resource
import com.example.mymess.data.models.AdminAnalyticsInsights
import com.example.mymess.data.models.AnalyticsSummary
import com.example.mymess.data.models.OwnerAnalyticsInsights

interface AnalyticsRepository {
    suspend fun getOwnerAnalytics(ownerUid: String): Resource<AnalyticsSummary>
    suspend fun getOwnerAnalyticsInsights(ownerUid: String): Resource<OwnerAnalyticsInsights>
    suspend fun getAdminAnalytics(): Resource<AnalyticsSummary>
    suspend fun getAdminAnalyticsInsights(): Resource<AdminAnalyticsInsights>
}
