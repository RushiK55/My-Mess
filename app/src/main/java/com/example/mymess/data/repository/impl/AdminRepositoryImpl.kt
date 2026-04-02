package com.example.mymess.data.repository.impl

import com.example.mymess.core.Resource
import com.example.mymess.data.models.Mess
import com.example.mymess.data.models.User
import com.example.mymess.data.remote.FirebaseApi
import com.example.mymess.data.repository.AdminRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javax.inject.Inject

class AdminRepositoryImpl @Inject constructor(
    private val api: FirebaseApi,
    private val gson: Gson,
) : AdminRepository {

    override suspend fun getPendingOwners(): Resource<List<User>> {
        return runCatching {
            val usersResponse = api.getData("users")
            if (!usersResponse.isSuccessful || usersResponse.body() == null) {
                return Resource.Success(emptyList())
            }

            val userType = object : TypeToken<Map<String, User>>() {}.type
            val users: Map<String, User> = gson.fromJson(usersResponse.body(), userType) ?: emptyMap()

            val messesResponse = api.getData("messes")
            val messType = object : TypeToken<Map<String, Mess>>() {}.type
            val messes: Map<String, Mess> = if (messesResponse.isSuccessful && messesResponse.body() != null) {
                gson.fromJson(messesResponse.body(), messType) ?: emptyMap()
            } else {
                emptyMap()
            }

            val unapprovedOwnerIds = messes.values
                .filter { !it.isApproved }
                .map { it.ownerId }
                .toSet()

            val pendingOwners = users.values
                .filter {
                    it.role == "owner" && (
                        unapprovedOwnerIds.contains(it.uid) ||
                            it.status == "pending"
                        )
                }
                .sortedByDescending { it.createdAt }
            Resource.Success(pendingOwners)
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch pending owners") }
    }

    override suspend fun approveOwner(ownerUid: String): Resource<Unit> {
        return updateOwnerStatus(ownerUid, "approved")
    }

    override suspend fun rejectOwner(ownerUid: String): Resource<Unit> {
        return updateOwnerStatus(ownerUid, "rejected")
    }

    override suspend fun getAllUsers(): Resource<List<User>> {
        return runCatching {
            val response = api.getData("users")
            if (!response.isSuccessful || response.body() == null) {
                return Resource.Success(emptyList())
            }
            val type = object : TypeToken<Map<String, User>>() {}.type
            val users: Map<String, User> = gson.fromJson(response.body(), type) ?: emptyMap()
            Resource.Success(users.values.sortedBy { it.name.lowercase() })
        }.getOrElse { Resource.Error(it.message ?: "Unable to fetch users") }
    }

    override suspend fun updateUserStatus(userUid: String, status: String): Resource<Unit> {
        return runCatching {
            val userResponse = api.getObject("users", userUid)
            if (!userResponse.isSuccessful || userResponse.body() == null) {
                return Resource.Error("User not found")
            }
            val user = gson.fromJson(userResponse.body(), User::class.java)
            val updated = user.copy(status = status)
            val updateResponse = api.putData("users", userUid, updated)
            if (updateResponse.isSuccessful) Resource.Success(Unit) else Resource.Error("Failed to update user")
        }.getOrElse { Resource.Error(it.message ?: "Failed to update user") }
    }

    override suspend fun deleteUser(userUid: String): Resource<Unit> {
        return runCatching {
            val userResponse = api.getObject("users", userUid)
            if (!userResponse.isSuccessful || userResponse.body() == null) {
                return Resource.Error("User not found")
            }

            val user = gson.fromJson(userResponse.body(), User::class.java)
            val deleteUserResponse = api.deleteData("users", userUid)
            if (!deleteUserResponse.isSuccessful) {
                return Resource.Error("Failed to delete user")
            }

            if (user.role == "owner") {
                val messesResponse = api.getData("messes")
                if (messesResponse.isSuccessful && messesResponse.body() != null) {
                    val messType = object : TypeToken<Map<String, Mess>>() {}.type
                    val messes: Map<String, Mess> = gson.fromJson(messesResponse.body(), messType) ?: emptyMap()
                    messes.values
                        .filter { it.ownerId == userUid }
                        .forEach { api.deleteData("messes", it.messId) }
                }
            }

            Resource.Success(Unit)
        }.getOrElse { Resource.Error(it.message ?: "Failed to delete user") }
    }

    private suspend fun updateOwnerStatus(ownerUid: String, status: String): Resource<Unit> {
        return runCatching {
            val userResponse = api.getObject("users", ownerUid)
            if (!userResponse.isSuccessful || userResponse.body() == null) {
                return Resource.Error("Owner not found")
            }

            val owner = gson.fromJson(userResponse.body(), User::class.java)
            val updatedOwner = owner.copy(status = status)
            val updateUserResponse = api.putData("users", ownerUid, updatedOwner)
            if (!updateUserResponse.isSuccessful) {
                return Resource.Error("Failed to update owner status")
            }

            val messesResponse = api.getData("messes")
            if (messesResponse.isSuccessful && messesResponse.body() != null) {
                val messType = object : TypeToken<Map<String, Mess>>() {}.type
                val messes: Map<String, Mess> = gson.fromJson(messesResponse.body(), messType) ?: emptyMap()
                val shouldApprove = status == "approved"
                messes.values
                    .filter { it.ownerId == ownerUid }
                    .forEach { api.putData("messes", it.messId, it.copy(isApproved = shouldApprove)) }
            }

            Resource.Success(Unit)
        }.getOrElse { Resource.Error(it.message ?: "Failed to update owner") }
    }
}

