package com.example.api

import retrofit2.Response
import retrofit2.http.*

interface SignalConnectApi {
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<AuthResponse>>

    @GET("api/routers")
    suspend fun getRouters(
        @Header("Authorization") token: String
    ): Response<ApiResponse<List<RouterNetworkDto>>>

    @POST("api/routers")
    suspend fun registerRouter(
        @Header("Authorization") token: String,
        @Body request: CreateRouterRequest
    ): Response<ApiResponse<RouterNetworkDto>>

    @DELETE("api/routers/{bssid}")
    suspend fun deleteRouter(
        @Header("Authorization") token: String,
        @Path("bssid") bssid: String
    ): Response<ApiResponse<Unit>>
}
