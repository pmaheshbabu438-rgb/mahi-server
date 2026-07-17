package com.example.api

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val email: String,
    val name: String,
    val password: String,
    val isAdmin: Boolean = false
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    val id: String,
    val email: String,
    val name: String,
    val token: String,
    val isAdmin: Boolean
)

@JsonClass(generateAdapter = true)
data class RouterNetworkDto(
    val bssid: String,
    val ssid: String,
    val signalStrengthDbm: Int,
    val downloadSpeedMbps: Float,
    val uploadSpeedMbps: Float,
    val latencyMs: Int,
    val distanceMeters: Float,
    val securityType: String,
    val isRegistered: Boolean = true,
    val capacityMax: Int = 100,
    val capacityActive: Int = 0,
    val billingRate: String = "Free",
    val description: String = ""
)

@JsonClass(generateAdapter = true)
data class CreateRouterRequest(
    val bssid: String,
    val ssid: String,
    val downloadSpeedMbps: Float,
    val uploadSpeedMbps: Float,
    val securityType: String,
    val billingRate: String = "Free",
    val description: String = ""
)

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String?,
    val data: T?
)
