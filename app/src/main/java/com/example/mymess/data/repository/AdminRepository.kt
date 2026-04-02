package com.example.mymess.data.repository

import com.example.mymess.core.Resource
import com.example.mymess.data.models.User

interface AdminRepository {
    suspend fun getPendingOwners(): Resource<List<User>>
    suspend fun approveOwner(ownerUid: String): Resource<Unit>
    suspend fun rejectOwner(ownerUid: String): Resource<Unit>
    suspend fun getAllUsers(): Resource<List<User>>
    suspend fun updateUserStatus(userUid: String, status: String): Resource<Unit>
    suspend fun deleteUser(userUid: String): Resource<Unit>
}

