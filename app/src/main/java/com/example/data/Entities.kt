package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val email: String,
    val name: String,
    val token: String,
    val isAdmin: Boolean = false,
    val isLocalUser: Boolean = false
)

@Entity(tableName = "routers")
data class RouterEntity(
    @PrimaryKey val bssid: String, // MAC Address / Unique Identifier
    val ssid: String,
    val signalStrengthDbm: Int, // e.g. -50 to -90
    val downloadSpeedMbps: Float, // e.g. 45.0f
    val uploadSpeedMbps: Float, // e.g. 20.0f
    val latencyMs: Int, // e.g. 15
    val distanceMeters: Float, // e.g. 12.5f
    val securityType: String, // e.g. "WPA3", "Open"
    val isRegistered: Boolean = true,
    val capacityMax: Int = 100,
    val capacityActive: Int = 14,
    val billingRate: String = "Free",
    val description: String = "High Performance Community Node"
)

@Entity(tableName = "usage_logs")
data class UsageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val routerBssid: String,
    val routerSsid: String,
    val durationSeconds: Long,
    val megabytesConsumed: Float,
    val avgDownloadSpeedMbps: Float
)
