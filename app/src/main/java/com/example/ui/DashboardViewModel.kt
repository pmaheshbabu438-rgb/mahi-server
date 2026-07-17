package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.RouterEntity
import com.example.data.SignalConnectRepository
import com.example.data.UsageLogEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class ConnectionStatus {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED
}

class DashboardViewModel(private val repository: SignalConnectRepository) : ViewModel() {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _activeRouter = MutableStateFlow<RouterEntity?>(null)
    val activeRouter: StateFlow<RouterEntity?> = _activeRouter.asStateFlow()

    private val _autoSwitchEnabled = MutableStateFlow(true)
    val autoSwitchEnabled: StateFlow<Boolean> = _autoSwitchEnabled.asStateFlow()

    private val _totalDataUsedMb = MutableStateFlow(124.5f) // starting demo data consumption
    val totalDataUsedMb: StateFlow<Float> = _totalDataUsedMb.asStateFlow()

    // Realtime indicators (fluctuating while connected)
    private val _currentDownloadSpeedMbps = MutableStateFlow(0f)
    val currentDownloadSpeedMbps: StateFlow<Float> = _currentDownloadSpeedMbps.asStateFlow()

    private val _currentUploadSpeedMbps = MutableStateFlow(0f)
    val currentUploadSpeedMbps: StateFlow<Float> = _currentUploadSpeedMbps.asStateFlow()

    private val _currentLatencyMs = MutableStateFlow(0)
    val currentLatencyMs: StateFlow<Int> = _currentLatencyMs.asStateFlow()

    private val _currentSignalStrengthDbm = MutableStateFlow(-100)
    val currentSignalStrengthDbm: StateFlow<Int> = _currentSignalStrengthDbm.asStateFlow()

    private val _networkError = MutableStateFlow<String?>(null)
    val networkError: StateFlow<String?> = _networkError.asStateFlow()

    val allRouters = repository.allRoutersFlow
    val usageLogs = repository.allLogsFlow

    private var connectionJob: Job? = null
    private var sessionStartTime: Long = 0
    private var sessionMegabytes: Float = 0f

    init {
        viewModelScope.launch {
            repository.prepopulateRoutersIfEmpty()
        }
    }

    fun isDemoMode(): Boolean = repository.getDemoMode()
    fun setDemoMode(demo: Boolean) {
        repository.setDemoMode(demo)
        viewModelScope.launch {
            repository.syncRouters()
        }
    }

    fun updateServerUrl(url: String) {
        repository.setServerUrl(url)
        viewModelScope.launch {
            repository.syncRouters()
        }
    }

    fun startScan() {
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.SCANNING
            _networkError.value = null
            delay(1800) // Aesthetic radar scanning delay
            val res = repository.syncRouters()
            if (res.isFailure) {
                _networkError.value = "Failed to sync router databases. Using local network memory."
            }
            _connectionStatus.value = ConnectionStatus.DISCONNECTED
        }
    }

    fun connectToRouter(router: RouterEntity) {
        if (!router.isRegistered) {
            _networkError.value = "Unable to connect cleanly: SSID ${router.ssid} is not certified on the SignalConnect Network."
            return
        }

        connectionJob?.cancel()
        viewModelScope.launch {
            _connectionStatus.value = ConnectionStatus.CONNECTING
            _networkError.value = null
            delay(1500) // Aesthetic authentic encryption handshake

            _activeRouter.value = router
            _connectionStatus.value = ConnectionStatus.CONNECTED

            // Initialize active states matching the connected node
            _currentDownloadSpeedMbps.value = router.downloadSpeedMbps
            _currentUploadSpeedMbps.value = router.uploadSpeedMbps
            _currentLatencyMs.value = router.latencyMs
            _currentSignalStrengthDbm.value = router.signalStrengthDbm

            sessionStartTime = System.currentTimeMillis()
            sessionMegabytes = 0f

            startConnectionTickTracker()
        }
    }

    fun disconnectCurrent() {
        val currRouter = _activeRouter.value
        if (currRouter != null && _connectionStatus.value == ConnectionStatus.CONNECTED) {
            storeSessionLog(currRouter)
        }
        connectionJob?.cancel()
        _activeRouter.value = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        _currentDownloadSpeedMbps.value = 0f
        _currentUploadSpeedMbps.value = 0f
        _currentLatencyMs.value = 0
        _currentSignalStrengthDbm.value = -100
    }

    fun toggleAutoSwitch() {
        _autoSwitchEnabled.value = !_autoSwitchEnabled.value
    }

    private fun startConnectionTickTracker() {
        connectionJob = viewModelScope.launch {
            while (isActive) {
                delay(1200) // poll stats
                val router = _activeRouter.value ?: break

                // Introduce high-stability tactile fluctuations (+/- 3 Dbm, speeds +- 10%)
                val strengthFluctuation = (-2..2).random()
                val speedFluctuation = (-8..8).random() / 100f
                val latencyFluctuation = (-3..3).random()

                val newStrength = (router.signalStrengthDbm + strengthFluctuation).coerceIn(-95, -30)
                _currentSignalStrengthDbm.value = newStrength

                val finalDl = (router.downloadSpeedMbps * (1f + speedFluctuation)).coerceAtLeast(1f)
                _currentDownloadSpeedMbps.value = String.format("%.1f", finalDl).toFloat()

                val finalUl = (router.uploadSpeedMbps * (1f + speedFluctuation)).coerceAtLeast(0.5f)
                _currentUploadSpeedMbps.value = String.format("%.1f", finalUl).toFloat()

                _currentLatencyMs.value = (router.latencyMs + latencyFluctuation).coerceIn(4, 150)

                // Accumulate megabytes consumed based on speed
                val addedMb = (finalDl / 8f) * 0.15f // simulated consumption multiplier
                sessionMegabytes += addedMb
                _totalDataUsedMb.value += addedMb

                // Check condition for Auto-Switching
                if (_autoSwitchEnabled.value && newStrength < -78) {
                    // Current connection is weak! Trigger auto-switch search
                    triggerAutoSwitchLookup()
                }
            }
        }
    }

    private fun triggerAutoSwitchLookup() {
        viewModelScope.launch {
            val routersList = repository.allRoutersFlow.firstOrNull() ?: emptyList()
            // Find a registered router with higher signal strength within range
            val alternative = routersList
                .filter { it.isRegistered && it.bssid != _activeRouter.value?.bssid }
                .maxByOrNull { it.signalStrengthDbm }

            if (alternative != null && alternative.signalStrengthDbm > -70) {
                // Perform a hot-swap! Save current session log then connect to stronger node automatically.
                val prevRouter = _activeRouter.value
                if (prevRouter != null) {
                    storeSessionLog(prevRouter)
                }

                _connectionStatus.value = ConnectionStatus.CONNECTING
                _networkError.value = "Current signal weak (${_currentSignalStrengthDbm.value} dBm). Swapping to stronger router ${alternative.ssid}..."
                delay(1300)

                _activeRouter.value = alternative
                _connectionStatus.value = ConnectionStatus.CONNECTED

                _currentDownloadSpeedMbps.value = alternative.downloadSpeedMbps
                _currentUploadSpeedMbps.value = alternative.uploadSpeedMbps
                _currentLatencyMs.value = alternative.latencyMs
                _currentSignalStrengthDbm.value = alternative.signalStrengthDbm

                sessionStartTime = System.currentTimeMillis()
                sessionMegabytes = 0f
            }
        }
    }

    // Force signal drop to test auto-switching directly in the UI!
    fun testForceSignalDrop() {
        if (_connectionStatus.value == ConnectionStatus.CONNECTED) {
            _currentSignalStrengthDbm.value = -85
            _networkError.value = "Manual test trigger: Signal dropped to -85 dBm. Checking auto-reconnect..."
        }
    }

    private fun storeSessionLog(router: RouterEntity) {
        val duration = (System.currentTimeMillis() - sessionStartTime) / 1000
        if (duration > 0 || sessionMegabytes > 0.01f) {
            val log = UsageLogEntity(
                routerBssid = router.bssid,
                routerSsid = router.ssid,
                durationSeconds = duration.coerceAtLeast(1),
                megabytesConsumed = String.format("%.2f", sessionMegabytes).toFloat(),
                avgDownloadSpeedMbps = router.downloadSpeedMbps
            )
            viewModelScope.launch {
                repository.addUsageLog(log)
            }
        }
    }

    fun addCustomRouter(
        bssid: String,
        ssid: String,
        downloadSpeed: Float,
        uploadSpeed: Float,
        securityType: String,
        billingRate: String,
        description: String
    ) {
        viewModelScope.launch {
            repository.addRouter(
                bssid = bssid,
                ssid = ssid,
                downloadSpeed = downloadSpeed,
                uploadSpeed = uploadSpeed,
                securityType = securityType,
                billingRate = billingRate,
                description = description
            )
        }
    }

    fun deleteRouter(bssid: String) {
        viewModelScope.launch {
            if (_activeRouter.value?.bssid == bssid) {
                disconnectCurrent()
            }
            repository.deleteRouter(bssid)
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearUsageLogs()
        }
    }

    override fun onCleared() {
        disconnectCurrent()
        super.onCleared()
    }
}

class DashboardViewModelFactory(private val repository: SignalConnectRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
