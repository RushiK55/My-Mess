package com.example.mymess.data.repository.impl

import com.example.mymess.core.Resource
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.User
import com.example.mymess.data.remote.FirebaseApi
import com.example.mymess.data.repository.AuthRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: FirebaseApi,
    private val gson: Gson,
) : AuthRepository {

    override suspend fun register(user: User, ownerMessDraft: Mess?): Resource<User> {
        return runCatching {
            if (user.role == "admin") {
                return Resource.Error("Admin registration is disabled")
            }

            val usersResponse = runCatching { api.getData("users") }.getOrNull()
            val existingUsers: Map<String, User> = if (usersResponse?.isSuccessful == true && usersResponse.body() != null) {
                runCatching {
                    val type = object : TypeToken<Map<String, User>>() {}.type
                    gson.fromJson<Map<String, User>>(usersResponse.body(), type) ?: emptyMap()
                }.getOrElse { emptyMap() }
            } else {
                emptyMap()
            }

            val duplicate = existingUsers.values.any { it.email.equals(user.email, ignoreCase = true) }
            if (duplicate) {
                return Resource.Error("Email already registered")
            }

            val uid = user.uid.ifBlank { UUID.randomUUID().toString() }
            val payload = user.copy(uid = uid)
            val response = api.putData(path = "users", id = uid, data = payload)
            if (!response.isSuccessful) {
                return Resource.Error("Registration failed")
            }

            if (payload.role == "owner") {
                val messDraft = ownerMessDraft ?: return Resource.Error("Mess details are required for owner registration")
                val messId = messDraft.messId.ifBlank { UUID.randomUUID().toString() }
                val messPayload = messDraft.copy(
                    messId = messId,
                    ownerId = uid,
                    isApproved = false,
                    createdAt = if (messDraft.createdAt <= 0L) System.currentTimeMillis() else messDraft.createdAt,
                )
                val messResponse = api.putData(path = "messes", id = messId, data = messPayload)
                if (!messResponse.isSuccessful) {
                    api.deleteData("users", uid)
                    return Resource.Error("Registration failed while creating mess profile")
                }
            }

            Resource.Success(payload)
        }.getOrElse { Resource.Error(it.message ?: "Registration failed") }
    }

    override suspend fun login(email: String, password: String): Resource<User> {
        if (email.isBlank() || password.isBlank()) {
            return Resource.Error("Email and password are required")
        }

        val response = runCatching { api.getData("users") }
            .getOrElse { return Resource.Error(it.message ?: "Login failed") }

        if (!response.isSuccessful || response.body() == null) {
            return Resource.Error("Unable to fetch users")
        }

        val rawMapType = object : TypeToken<Map<String, User>>() {}.type
        val users: Map<String, User> = gson.fromJson(response.body(), rawMapType) ?: emptyMap()

        val user = users.values.firstOrNull { it.email.equals(email, ignoreCase = true) }
            ?: return Resource.Error("User not found")

        if (user.password.isBlank() || user.password != password) {
            return Resource.Error("Invalid credentials")
        }

        return when (user.status.lowercase()) {
            "approved" -> Resource.Success(user)
            "pending" -> Resource.Error("Your account is pending approval")
            "blocked" -> Resource.Error("Your account has been blocked")
            "rejected" -> Resource.Error("Your account was rejected by admin")
            else -> Resource.Error("Invalid account status")
        }
    }

    override suspend fun getUserByUid(uid: String): Resource<User> {
        if (uid.isBlank()) return Resource.Error("Invalid session")
        return runCatching {
            val response = api.getObject("users", uid)
            if (!response.isSuccessful || response.body() == null) {
                return Resource.Error("User not found")
            }
            val user = gson.fromJson(response.body(), User::class.java)
            Resource.Success(user)
        }.getOrElse { Resource.Error(it.message ?: "Unable to verify session") }
    }

    override suspend fun updateUserProfile(uid: String, name: String, phone: String): Resource<User> {
        if (uid.isBlank()) return Resource.Error("Invalid session")
        if (name.isBlank() || phone.isBlank()) return Resource.Error("Name and phone are required")
        return runCatching {
            val current = getUserByUid(uid)
            if (current !is Resource.Success) {
                return current as? Resource.Error ?: Resource.Error("Unable to fetch profile")
            }
            val updated = current.data.copy(name = name.trim(), phone = phone.trim())
            val response = api.putData("users", uid, updated)
            if (response.isSuccessful) Resource.Success(updated) else Resource.Error("Failed to update profile")
        }.getOrElse { Resource.Error(it.message ?: "Failed to update profile") }
    }

    override suspend fun resetPassword(email: String, newPassword: String): Resource<Unit> {
        if (email.isBlank() || newPassword.isBlank()) {
            return Resource.Error("Email and new password are required")
        }
        return runCatching {
            val response = api.getData("users")
            if (!response.isSuccessful || response.body() == null) {
                return Resource.Error("Unable to fetch users")
            }
            val type = object : TypeToken<Map<String, User>>() {}.type
            val users: Map<String, User> = gson.fromJson(response.body(), type) ?: emptyMap()
            val matchedUser = users.values.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }
                ?: return Resource.Error("No account found for this email")

            val updated = matchedUser.copy(password = newPassword)
            val updateResponse = api.putData("users", matchedUser.uid, updated)
            if (updateResponse.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to reset password")
        }.getOrElse { Resource.Error(it.message ?: "Failed to reset password") }
    }

    override suspend fun changePassword(uid: String, currentPassword: String, newPassword: String): Resource<Unit> {
        if (uid.isBlank()) return Resource.Error("Invalid session")
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            return Resource.Error("Current and new password are required")
        }
        return runCatching {
            val current = getUserByUid(uid)
            if (current !is Resource.Success) {
                return current as? Resource.Error ?: Resource.Error("Unable to fetch profile")
            }
            if (current.data.password != currentPassword) {
                return Resource.Error("Current password is incorrect")
            }
            val updated = current.data.copy(password = newPassword)
            val response = api.putData("users", uid, updated)
            if (response.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to update password")
        }.getOrElse { Resource.Error(it.message ?: "Failed to update password") }
    }
}


