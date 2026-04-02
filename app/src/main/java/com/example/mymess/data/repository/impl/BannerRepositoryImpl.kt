package com.example.mymess.data.repository.impl

import com.example.mymess.core.Resource
import com.example.mymess.data.models.Banner
import com.example.mymess.data.remote.FirebaseApi
import com.example.mymess.data.repository.BannerRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import javax.inject.Inject

class BannerRepositoryImpl @Inject constructor(
    private val api: FirebaseApi,
    private val gson: Gson,
) : BannerRepository {

    override suspend fun getAllBanners(): Resource<List<Banner>> {
        return runCatching {
            val response = api.getData("banners")
            if (!response.isSuccessful || response.body() == null) {
                return Resource.Success(emptyList())
            }

            val type = object : TypeToken<Map<String, Banner>>() {}.type
            val banners: Map<String, Banner> = gson.fromJson(response.body(), type) ?: emptyMap()
            Resource.Success(banners.values.sortedByDescending { it.createdAt })
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch banners") }
    }

    override suspend fun getActiveBanners(targetRole: String): Resource<List<Banner>> {
        return runCatching {
            val response = api.getData("banners")
            if (!response.isSuccessful || response.body() == null) {
                return Resource.Success(emptyList())
            }

            val type = object : TypeToken<Map<String, Banner>>() {}.type
            val banners: Map<String, Banner> = gson.fromJson(response.body(), type) ?: emptyMap()
            val filtered = banners.values.filter {
                it.isActive && (it.targetRole == "all" || it.targetRole == targetRole)
            }
            Resource.Success(filtered.sortedByDescending { it.createdAt })
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch banners") }
    }

    override suspend fun getBannersByCreator(creatorUid: String, targetRole: String?): Resource<List<Banner>> {
        return runCatching {
            val response = api.getData("banners")
            if (!response.isSuccessful || response.body() == null) {
                return Resource.Success(emptyList())
            }

            val type = object : TypeToken<Map<String, Banner>>() {}.type
            val banners: Map<String, Banner> = gson.fromJson(response.body(), type) ?: emptyMap()
            val filtered = banners.values.filter {
                it.createdBy == creatorUid && (targetRole == null || it.targetRole == targetRole)
            }
            Resource.Success(filtered.sortedByDescending { it.createdAt })
        }.getOrElse { Resource.Error(it.message ?: "Unable to load banners") }
    }

    override suspend fun saveBanner(banner: Banner): Resource<Unit> {
        return runCatching {
            val bannerId = banner.bannerId.ifBlank { UUID.randomUUID().toString() }
            val payload = banner.copy(bannerId = bannerId)
            val response = api.putData("banners", bannerId, payload)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to save banner")
        }.getOrElse { Resource.Error(it.message ?: "Failed to save banner") }
    }

    override suspend fun deleteBanner(bannerId: String): Resource<Unit> {
        return runCatching {
            val response = api.deleteData("banners", bannerId)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to delete banner")
        }.getOrElse { Resource.Error(it.message ?: "Failed to delete banner") }
    }
}
