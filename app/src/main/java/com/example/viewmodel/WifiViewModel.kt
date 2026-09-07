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
    val statusMessage: String = "Prêt à inspecter le réseau local"
)

class WifiViewModel(application: Application) : AndroidViewModel(application) {

    private val wifiInspector = WifiInspector(application)

    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    private var subnetScanJob: Job? = null
    private var mdnsJob: Job? = null

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

    override fun onCleared() {
        super.onCleared()
        subnetScanJob?.cancel()
        mdnsJob?.cancel()
    }
}
