package com.example.mymess.data.repository

import com.example.mymess.core.Resource
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.User

interface AuthRepository {
    suspend fun register(user: User, ownerMessDraft: Mess? = null): Resource<User>
    suspend fun login(email: String, password: String): Resource<User>
    suspend fun getUserByUid(uid: String): Resource<User>
    suspend fun updateUserProfile(uid: String, name: String, phone: String): Resource<User>
    suspend fun resetPassword(email: String, newPassword: String): Resource<Unit>
    suspend fun changePassword(uid: String, currentPassword: String, newPassword: String): Resource<Unit>
}

