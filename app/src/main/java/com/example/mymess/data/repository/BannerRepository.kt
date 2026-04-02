package com.example.mymess.data.repository

import com.example.mymess.core.Resource
import com.example.mymess.data.models.Banner

interface BannerRepository {
    suspend fun getActiveBanners(targetRole: String): Resource<List<Banner>>
    suspend fun getAllBanners(): Resource<List<Banner>>
    suspend fun getBannersByCreator(creatorUid: String, targetRole: String? = null): Resource<List<Banner>>
    suspend fun saveBanner(banner: Banner): Resource<Unit>
    suspend fun deleteBanner(bannerId: String): Resource<Unit>
}
