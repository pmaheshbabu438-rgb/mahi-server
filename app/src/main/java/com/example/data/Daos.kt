package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUser()
}

@Dao
interface RouterDao {
    @Query("SELECT * FROM routers")
    fun getAllRoutersFlow(): Flow<List<RouterEntity>>

    @Query("SELECT * FROM routers WHERE bssid = :bssid LIMIT 1")
    suspend fun getRouterByBssid(bssid: String): RouterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouters(routers: List<RouterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRouter(router: RouterEntity)

    @Delete
    suspend fun deleteRouter(router: RouterEntity)

    @Query("DELETE FROM routers WHERE bssid = :bssid")
    suspend fun deleteRouterByBssid(bssid: String)

    @Query("DELETE FROM routers")
    suspend fun clearAllRouters()
}

@Dao
interface UsageDao {
    @Query("SELECT * FROM usage_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<UsageLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: UsageLogEntity)

    @Query("DELETE FROM usage_logs")
    suspend fun clearLogs()
}
