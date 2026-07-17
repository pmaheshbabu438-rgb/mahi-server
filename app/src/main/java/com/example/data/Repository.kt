package com.example.data

import com.example.api.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class SignalConnectRepository(
    private val userDao: UserDao,
    private val routerDao: RouterDao,
    private val usageDao: UsageDao
) {
    // Endpoints base URL can be customized by the user. Default to localhost or standard development address
    private var serverUrl = "https://ais-dev-s4ge53o7ymc4mo6ile7w6y-956448708501.asia-southeast1.run.app/"
    private var isDemoMode = true // Standard default is demo mode so it works instantly out of the box in the visual emulator

    private val api: SignalConnectApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(serverUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .client(client)
            .build()
            .create(SignalConnectApi::class.java)
    }

    fun setServerUrl(url: String) {
        serverUrl = if (url.endsWith("/")) url else "$url/"
        isDemoMode = false
    }

    fun getDemoMode(): Boolean = isDemoMode
    fun setDemoMode(demo: Boolean) {
        this.isDemoMode = demo
    }

    // User session flow
    val currentUserFlow: Flow<UserEntity?> = userDao.getCurrentUserFlow()

    suspend fun login(email: String, password: String): Result<UserEntity> {
        if (isDemoMode) {
            // Simulated login. Special password 'admin' makes user an admin!
            val isAdmin = email.contains("admin") || password == "admin"
            val mockUser = UserEntity(
                id = "mock_user_123",
                email = email,
                name = if (isAdmin) "Admin Commander" else "Standard User",
                token = "mock_jwt_token_456_demo",
                isAdmin = isAdmin,
                isLocalUser = true
            )
            userDao.insertUser(mockUser)
            // Pre-populate routers list for demo if empty
            prepopulateRoutersIfEmpty()
            return Result.success(mockUser)
        }

        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                val user = UserEntity(
                    id = data.id,
                    email = data.email,
                    name = data.name,
                    token = data.token,
                    isAdmin = data.isAdmin,
                    isLocalUser = false
                )
                userDao.insertUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, name: String, password: String, isAdmin: Boolean): Result<UserEntity> {
        if (isDemoMode) {
            val mockUser = UserEntity(
                id = "mock_user_${System.currentTimeMillis()}",
                email = email,
                name = name,
                token = "mock_jwt_token_${System.currentTimeMillis()}",
                isAdmin = isAdmin,
                isLocalUser = true
            )
            userDao.insertUser(mockUser)
            prepopulateRoutersIfEmpty()
            return Result.success(mockUser)
        }

        return try {
            val response = api.register(RegisterRequest(email, name, password, isAdmin))
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                val user = UserEntity(
                    id = data.id,
                    email = data.email,
                    name = data.name,
                    token = data.token,
                    isAdmin = data.isAdmin,
                    isLocalUser = false
                )
                userDao.insertUser(user)
                Result.success(user)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        userDao.clearUser()
        // Keep logs and routers for local visualization
    }

    // Routers access
    val allRoutersFlow: Flow<List<RouterEntity>> = routerDao.getAllRoutersFlow()

    suspend fun syncRouters(): Result<List<RouterEntity>> {
        val currentUser = userDao.getCurrentUser()
        if (isDemoMode || currentUser == null) {
            prepopulateRoutersIfEmpty()
            val localRouters = routerDao.getAllRoutersFlow().firstOrNull() ?: emptyList()
            return Result.success(localRouters)
        }

        return try {
            val response = api.getRouters("Bearer ${currentUser.token}")
            if (response.isSuccessful && response.body()?.success == true) {
                val list = response.body()!!.data!!.map { dto ->
                    RouterEntity(
                        bssid = dto.bssid,
                        ssid = dto.ssid,
                        signalStrengthDbm = dto.signalStrengthDbm,
                        downloadSpeedMbps = dto.downloadSpeedMbps,
                        uploadSpeedMbps = dto.uploadSpeedMbps,
                        latencyMs = dto.latencyMs,
                        distanceMeters = dto.distanceMeters,
                        securityType = dto.securityType,
                        isRegistered = dto.isRegistered,
                        capacityMax = dto.capacityMax,
                        capacityActive = dto.capacityActive,
                        billingRate = dto.billingRate,
                        description = dto.description
                    )
                }
                routerDao.clearAllRouters()
                routerDao.insertRouters(list)
                Result.success(list)
            } else {
                // Return cache if fetch fails
                val cacheList = routerDao.getAllRoutersFlow().firstOrNull() ?: emptyList()
                if (cacheList.isNotEmpty()) {
                    Result.success(cacheList)
                } else {
                    Result.failure(Exception(response.body()?.message ?: "Failed to sync routers"))
                }
            }
        } catch (e: Exception) {
            val cacheList = routerDao.getAllRoutersFlow().firstOrNull() ?: emptyList()
            if (cacheList.isNotEmpty()) {
                Result.success(cacheList)
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun addRouter(
        bssid: String,
        ssid: String,
        downloadSpeed: Float,
        uploadSpeed: Float,
        securityType: String,
        billingRate: String,
        description: String
    ): Result<RouterEntity> {
        val newRouter = RouterEntity(
            bssid = bssid,
            ssid = ssid,
            signalStrengthDbm = -55,
            downloadSpeedMbps = downloadSpeed,
            uploadSpeedMbps = uploadSpeed,
            latencyMs = 24,
            distanceMeters = 8.5f,
            securityType = securityType,
            isRegistered = true,
            capacityMax = 120,
            capacityActive = 0,
            billingRate = billingRate,
            description = description
        )

        val currentUser = userDao.getCurrentUser()
        if (isDemoMode || currentUser == null) {
            routerDao.insertRouter(newRouter)
            return Result.success(newRouter)
        }

        return try {
            val req = CreateRouterRequest(
                bssid = bssid,
                ssid = ssid,
                downloadSpeedMbps = downloadSpeed,
                uploadSpeedMbps = uploadSpeed,
                securityType = securityType,
                billingRate = billingRate,
                description = description
            )
            val response = api.registerRouter("Bearer ${currentUser.token}", req)
            if (response.isSuccessful && response.body()?.success == true) {
                routerDao.insertRouter(newRouter)
                Result.success(newRouter)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Failed to register router on server"))
            }
        } catch (e: Exception) {
            // local backup anyway
            routerDao.insertRouter(newRouter)
            Result.success(newRouter)
        }
    }

    suspend fun deleteRouter(bssid: String): Result<Unit> {
        val currentUser = userDao.getCurrentUser()
        routerDao.deleteRouterByBssid(bssid)
        if (isDemoMode || currentUser == null) {
            return Result.success(Unit)
        }

        return try {
            val response = api.deleteRouter("Bearer ${currentUser.token}", bssid)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.body()?.message ?: "Server deletion failed"))
            }
        } catch (e: Exception) {
            Result.success(Unit) // local success
        }
    }

    // Usage tracking
    val allLogsFlow: Flow<List<UsageLogEntity>> = usageDao.getAllLogsFlow()

    suspend fun addUsageLog(log: UsageLogEntity) {
        usageDao.insertLog(log)
    }

    suspend fun clearUsageLogs() {
        usageDao.clearLogs()
    }

    suspend fun prepopulateRoutersIfEmpty() {
        val curr = routerDao.getAllRoutersFlow().firstOrNull() ?: emptyList()
        if (curr.isEmpty()) {
            val defaultRouters = listOf(
                RouterEntity(
                    bssid = "00:1A:2B:3C:4D:5E",
                    ssid = "SignalConnect_Alpha-7",
                    signalStrengthDbm = -42,
                    downloadSpeedMbps = 85.5f,
                    uploadSpeedMbps = 35.2f,
                    latencyMs = 12,
                    distanceMeters = 3.2f,
                    securityType = "WPA3 Secure",
                    isRegistered = true,
                    capacityMax = 150,
                    capacityActive = 18,
                    billingRate = "Free Community Link",
                    description = "Ultra-speed node deployed in Central Transit Hub."
                ),
                RouterEntity(
                    bssid = "24:F5:A2:8B:10:9C",
                    ssid = "Downtown_Greenway_Node",
                    signalStrengthDbm = -65,
                    downloadSpeedMbps = 38.0f,
                    uploadSpeedMbps = 12.5f,
                    latencyMs = 28,
                    distanceMeters = 14.8f,
                    securityType = "WPA2 Enterprise",
                    isRegistered = true,
                    capacityMax = 80,
                    capacityActive = 47,
                    billingRate = "Free Public",
                    description = "Outdoor greenway coverage zone."
                ),
                RouterEntity(
                    bssid = "E2:81:B4:9C:FD:0F",
                    ssid = "RescueNet_Gateway_Base",
                    signalStrengthDbm = -81,
                    downloadSpeedMbps = 15.2f,
                    uploadSpeedMbps = 4.0f,
                    latencyMs = 45,
                    distanceMeters = 24.5f,
                    securityType = "Open Gateway",
                    isRegistered = true,
                    capacityMax = 200,
                    capacityActive = 104,
                    billingRate = "Free Emergency",
                    description = "Weak signal cellular extender provided during local outages."
                ),
                RouterEntity(
                    bssid = "50:D4:F7:61:A0:C3",
                    ssid = "Unregistered_LocalNet",
                    signalStrengthDbm = -75,
                    downloadSpeedMbps = 4.2f,
                    uploadSpeedMbps = 1.0f,
                    latencyMs = 90,
                    distanceMeters = 19.3f,
                    securityType = "WPA2 Personal",
                    isRegistered = false,
                    capacityMax = 10,
                    capacityActive = 3,
                    billingRate = "Restricted",
                    description = "Standard private router (Not SignalConnect registered)."
                )
            )
            routerDao.insertRouters(defaultRouters)
        }
    }
}
