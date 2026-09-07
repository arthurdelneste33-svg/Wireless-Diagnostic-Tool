package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NetworkHost
import com.example.data.model.WifiConnectionDetails
import com.example.wifi.WifiInspector
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WifiUiState(
    val wifiDetails: WifiConnectionDetails = WifiConnectionDetails(),
    val isScanningSubnet: Boolean = false,
    val scanProgress: Float = 0f,
    val scannedHostsCount: Int = 0,
    val totalHostsCount: Int = 254,
    val discoveredHosts: List<NetworkHost> = emptyList(),
    val mdnsHosts: List<NetworkHost> = emptyList(),
    val isMdnsSearching: Boolean = false,
    val statusMessage: String = "Prêt à inspecter le réseau local",
    val pingTarget: String = "1.1.1.1",
    val isPinging: Boolean = false,
    val pingResults: List<com.example.data.model.NetworkPingResult> = emptyList(),
    val speedBenchmark: com.example.data.model.NetworkSpeedBenchmark = com.example.data.model.NetworkSpeedBenchmark()
)

class WifiViewModel(application: Application) : AndroidViewModel(application) {

    private val wifiInspector = WifiInspector(application)

    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    private var subnetScanJob: Job? = null
    private var mdnsJob: Job? = null
    private var pingJob: Job? = null
    private var speedTestJob: Job? = null

    init {
        refreshWifiInfo()
    }

    fun refreshWifiInfo() {
        viewModelScope.launch {
            val details = wifiInspector.getWifiConnectionDetails()
            _uiState.update {
                it.copy(
                    wifiDetails = details,
                    statusMessage = if (details.isConnected) {
                        "Connecté à ${details.ssid} (${details.ipAddress})"
                    } else {
                        "Non connecté au Wi-Fi. Veuillez vous connecter pour analyser le réseau."
                    }
                )
            }
        }
    }

    fun startSubnetScan() {
        if (_uiState.value.isScanningSubnet) return
        val currentDetails = _uiState.value.wifiDetails
        if (!currentDetails.isConnected || currentDetails.ipAddress == "0.0.0.0") {
            _uiState.update { it.copy(statusMessage = "Impossible de scanner: Wi-Fi non connecté") }
            return
        }

        subnetScanJob?.cancel()
        subnetScanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isScanningSubnet = true,
                    scanProgress = 0f,
                    scannedHostsCount = 0,
                    discoveredHosts = emptyList(),
                    statusMessage = "Balayage du sous-réseau en cours..."
                )
            }

            // Always add the current device host info
            val selfHost = NetworkHost(
                ip = currentDetails.ipAddress,
                mac = "Cet appareil",
                hostname = "Mon smartphone (${android.os.Build.MODEL})",
                responseTimeMs = 1,
                isGateway = false
            )

            val hosts = wifiInspector.scanSubnet(
                localIp = currentDetails.ipAddress,
                gateway = currentDetails.gateway,
                onProgress = { scanned, total ->
                    _uiState.update {
                        it.copy(
                            scannedHostsCount = scanned,
                            totalHostsCount = total,
                            scanProgress = scanned.toFloat() / total.toFloat()
                        )
                    }
                }
            )

            val fullList = (listOf(selfHost) + hosts.filter { it.ip != currentDetails.ipAddress })
                .sortedBy { host ->
                    host.ip.substringAfterLast(".").toIntOrNull() ?: 0
                }

            _uiState.update {
                it.copy(
                    isScanningSubnet = false,
                    scanProgress = 1f,
                    discoveredHosts = fullList,
                    statusMessage = "${fullList.size} hôtes actifs découverts sur le sous-réseau."
                )
            }
        }
    }

    fun stopSubnetScan() {
        subnetScanJob?.cancel()
        _uiState.update { it.copy(isScanningSubnet = false, statusMessage = "Scan interrompu") }
    }

    fun toggleMdnsDiscovery() {
        if (_uiState.value.isMdnsSearching) {
            mdnsJob?.cancel()
            _uiState.update { it.copy(isMdnsSearching = false) }
        } else {
            mdnsJob?.cancel()
            mdnsJob = viewModelScope.launch {
                _uiState.update { it.copy(isMdnsSearching = true, mdnsHosts = emptyList()) }
                wifiInspector.startMdnsDiscovery("_http._tcp.").collect { host ->
                    _uiState.update { current ->
                        val updated = (current.mdnsHosts + host).distinctBy { it.hostname + it.ip }
                        current.copy(mdnsHosts = updated)
                    }
                }
            }
        }
    }

    fun setPingTarget(target: String) {
        _uiState.update { it.copy(pingTarget = target) }
    }

    fun executePing(customTarget: String? = null) {
        val target = (customTarget ?: _uiState.value.pingTarget).ifBlank { "1.1.1.1" }
        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            _uiState.update { it.copy(isPinging = true) }
            val result = wifiInspector.pingHost(target)
            _uiState.update { current ->
                val newResults = (listOf(result) + current.pingResults).take(15)
                current.copy(isPinging = false, pingResults = newResults)
            }
        }
    }

    fun clearPingResults() {
        _uiState.update { it.copy(pingResults = emptyList()) }
    }

    fun startSpeedBenchmark() {
        if (_uiState.value.speedBenchmark.isTesting) return
        speedTestJob?.cancel()
        speedTestJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    speedBenchmark = com.example.data.model.NetworkSpeedBenchmark(
                        isTesting = true,
                        progress = 0.05f,
                        statusText = "Mesure de la latence initiale..."
                    )
                )
            }

            val ping = wifiInspector.pingHost("1.1.1.1")
            val initialPingMs = if (ping.isReachable) ping.latencyMs else 25L

            _uiState.update {
                it.copy(
                    speedBenchmark = it.speedBenchmark.copy(
                        pingMs = initialPingMs,
                        statusText = "Test de débit descendant en cours..."
                    )
                )
            }

            val avg = wifiInspector.runSpeedBenchmark { progress, currentSpeed, averageSpeed ->
                _uiState.update { current ->
                    current.copy(
                        speedBenchmark = current.speedBenchmark.copy(
                            progress = progress,
                            currentSpeedMbps = currentSpeed,
                            averageSpeedMbps = averageSpeed,
                            statusText = "Débit : %.1f Mbps (Moyenne : %.1f Mbps)".format(currentSpeed, averageSpeed)
                        )
                    )
                }
            }

            _uiState.update { current ->
                current.copy(
                    speedBenchmark = current.speedBenchmark.copy(
                        isTesting = false,
                        progress = 1f,
                        currentSpeedMbps = avg,
                        averageSpeedMbps = avg,
                        statusText = "Test terminé : %.1f Mbps mesurés".format(avg)
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        subnetScanJob?.cancel()
        mdnsJob?.cancel()
        pingJob?.cancel()
        speedTestJob?.cancel()
    }
}
