package com.example.mymess.data.remote

import com.example.mymess.data.models.PushResponse
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface FirebaseApi {
    @GET("{path}.json")
    suspend fun getData(
        @Path("path") path: String,
        @Query("orderBy") orderBy: String? = null,
        @Query("equalTo") equalTo: String? = null,
    ): Response<JsonObject>

    @GET("{path}/{id}.json")
    suspend fun getObject(
        @Path("path") path: String,
        @Path("id") id: String,
    ): Response<JsonObject>

    @POST("{path}.json")
    suspend fun postData(
        @Path("path") path: String,
        @Body data: Any,
    ): Response<PushResponse>

    @PUT("{path}/{id}.json")
    suspend fun putData(
        @Path("path") path: String,
        @Path("id") id: String,
        @Body data: Any,
    ): Response<JsonObject>

    @DELETE("{path}/{id}.json")
    suspend fun deleteData(
        @Path("path") path: String,
        @Path("id") id: String,
    ): Response<JsonObject>
}

