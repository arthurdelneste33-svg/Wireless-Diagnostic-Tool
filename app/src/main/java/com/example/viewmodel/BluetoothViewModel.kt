package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothExplorer
import com.example.data.model.BleDeviceItem
import com.example.data.model.GattServiceItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BluetoothUiState(
    val isSupported: Boolean = true,
    val isEnabled: Boolean = true,
    val isScanning: Boolean = false,
    val devices: List<BleDeviceItem> = emptyList(),
    val selectedDevice: BleDeviceItem? = null,
    val gattServices: List<GattServiceItem> = emptyList(),
    val gattStatus: String = "",
    val isConnectingGatt: Boolean = false,
    val searchQuery: String = ""
)

class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothExplorer = BluetoothExplorer(application)

    private val _uiState = MutableStateFlow(
        BluetoothUiState(
            isSupported = bluetoothExplorer.isBluetoothSupported,
            isEnabled = bluetoothExplorer.isBluetoothEnabled
        )
    )
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    fun checkBluetoothStatus() {
        _uiState.update {
            it.copy(
                isSupported = bluetoothExplorer.isBluetoothSupported,
                isEnabled = bluetoothExplorer.isBluetoothEnabled
            )
        }
    }

    fun startBleScan() {
        checkBluetoothStatus()
        if (!_uiState.value.isSupported || !_uiState.value.isEnabled) return

        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = true) }

        scanJob = viewModelScope.launch {
            try {
                bluetoothExplorer.scanBleDevices().collect { device ->
                    _uiState.update { current ->
                        val updatedMap = current.devices.associateBy { it.address }.toMutableMap()
                        val existing = updatedMap[device.address]
                        val history = ((existing?.rssiHistory ?: emptyList()) + device.rssi).takeLast(10)
                        val enhanced = device.copy(rssiHistory = history)
                        updatedMap[device.address] = enhanced
                        val sorted = updatedMap.values.sortedByDescending { it.rssi }
                        current.copy(devices = sorted)
                    }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun stopBleScan() {
        scanJob?.cancel()
        _uiState.update { it.copy(isScanning = false) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectDeviceForGatt(device: BleDeviceItem) {
        _uiState.update {
            it.copy(
                selectedDevice = device,
                gattServices = emptyList(),
                gattStatus = "Connexion à ${device.name ?: device.address}...",
                isConnectingGatt = true
            )
        }

        bluetoothExplorer.connectAndDiscoverGatt(
            deviceAddress = device.address,
            onStateChange = { status ->
                _uiState.update { it.copy(gattStatus = status) }
            },
            onServicesDiscovered = { services ->
                _uiState.update {
                    it.copy(
                        gattServices = services,
                        isConnectingGatt = false,
                        gattStatus = "${services.size} services découverts"
                    )
                }
            }
        )
    }

    fun disconnectGatt() {
        bluetoothExplorer.disconnectGatt()
        _uiState.update {
            it.copy(
                selectedDevice = null,
                gattServices = emptyList(),
                isConnectingGatt = false,
                gattStatus = ""
            )
        }
    }

    fun clearDevices() {
        _uiState.update { it.copy(devices = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        scanJob?.cancel()
        bluetoothExplorer.disconnectGatt()
    }
}
