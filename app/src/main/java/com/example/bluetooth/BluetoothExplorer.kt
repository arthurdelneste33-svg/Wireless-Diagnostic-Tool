package com.example.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.util.Log
import com.example.data.model.BleDeviceItem
import com.example.data.model.GattCharacteristicItem
import com.example.data.model.GattServiceItem
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

class BluetoothExplorer(private val context: Context) {

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val bleScanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun scanBleDevices(): Flow<BleDeviceItem> = callbackFlow {
        val scanner = bleScanner
        if (scanner == null || !isBluetoothEnabled) {
            close()
            return@callbackFlow
        }

        // Emit already bonded devices if any
        bluetoothAdapter?.bondedDevices?.forEach { device ->
            try {
                val bondStateDesc = when (device.bondState) {
                    BluetoothDevice.BOND_BONDED -> "Appairé"
                    BluetoothDevice.BOND_BONDING -> "Appairage..."
                    else -> "Non appairé"
                }
                val typeDesc = when (device.type) {
                    BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic (BR/EDR)"
                    BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
                    BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual (Classic + BLE)"
                    else -> "Inconnu"
                }
                trySend(
                    BleDeviceItem(
                        address = device.address,
                        name = device.name,
                        rssi = -70,
                        bondState = bondStateDesc,
                        deviceType = typeDesc
                    )
                )
            } catch (e: Exception) {
                Log.e("BluetoothExplorer", "Erreur lecture device appairé", e)
            }
        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device = result.device ?: return
                val uuids = result.scanRecord?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()
                val bondStateDesc = when (device.bondState) {
                    BluetoothDevice.BOND_BONDED -> "Appairé"
                    BluetoothDevice.BOND_BONDING -> "Appairage..."
                    else -> "Non appairé"
                }
                val typeDesc = when (device.type) {
                    BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic (BR/EDR)"
                    BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
                    BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual Mode"
                    else -> "BLE"
                }
                val txPower = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    result.txPower.takeIf { it != ScanResult.TX_POWER_NOT_PRESENT }
                } else null
                val distance = calculateDistance(result.rssi, txPower ?: -59)
                val item = BleDeviceItem(
                    address = device.address,
                    name = device.name ?: result.scanRecord?.deviceName,
                    rssi = result.rssi,
                    bondState = bondStateDesc,
                    deviceType = typeDesc,
                    advertisedServiceUuids = uuids,
                    txPowerDbm = txPower,
                    estimatedDistanceMeters = distance
                )
                trySend(item)
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { onScanResult(0, it) }
            }

            override fun onScanFailed(errorCode: Int) {
                Log.e("BluetoothExplorer", "Échec scan BLE: code $errorCode")
            }
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
        } catch (e: Exception) {
            Log.e("BluetoothExplorer", "Exception startScan", e)
        }

        awaitClose {
            try {
                scanner.stopScan(scanCallback)
            } catch (_: Exception) {}
        }
    }

    private var activeGatt: BluetoothGatt? = null

    @SuppressLint("MissingPermission")
    fun connectAndDiscoverGatt(
        deviceAddress: String,
        onStateChange: (String) -> Unit,
        onServicesDiscovered: (List<GattServiceItem>) -> Unit
    ) {
        disconnectGatt()
        val device = bluetoothAdapter?.getRemoteDevice(deviceAddress)
        if (device == null) {
            onStateChange("Périphérique introuvable")
            return
        }

        onStateChange("Connexion en cours...")

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        onStateChange("Connecté. Découverte des services GATT...")
                        try {
                            gatt.discoverServices()
                        } catch (e: Exception) {
                            onStateChange("Erreur découverte services: ${e.message}")
                        }
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        onStateChange("Déconnecté")
                        disconnectGatt()
                    }
                    else -> {
                        onStateChange("État: $newState")
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val serviceItems = gatt.services.map { service ->
                        parseGattService(service)
                    }
                    onStateChange("Services découverts (${serviceItems.size})")
                    onServicesDiscovered(serviceItems)
                } else {
                    onStateChange("Échec découverte services GATT: $status")
                }
            }
        }

        try {
            activeGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: Exception) {
            onStateChange("Erreur: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnectGatt() {
        try {
            activeGatt?.disconnect()
            activeGatt?.close()
        } catch (_: Exception) {}
        activeGatt = null
    }

    private fun parseGattService(service: BluetoothGattService): GattServiceItem {
        val serviceUuid = service.uuid.toString()
        val serviceName = resolveGattServiceName(service.uuid)
        val chars = service.characteristics.map { c ->
            val charUuid = c.uuid.toString()
            val charName = resolveGattCharacteristicName(c.uuid)
            val props = mutableListOf<String>()
            if ((c.properties and BluetoothGattCharacteristic.PROPERTY_READ) != 0) props.add("READ")
            if ((c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) props.add("WRITE")
            if ((c.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) props.add("WRITE_NO_RESP")
            if ((c.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) props.add("NOTIFY")
            if ((c.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0) props.add("INDICATE")

            GattCharacteristicItem(
                uuid = charUuid,
                name = charName,
                propertiesDesc = props,
                valueHex = c.value?.joinToString(":") { "%02X".format(it) },
                valueStr = c.value?.let { String(it, Charsets.UTF_8).filter { ch -> ch in ' '..'~' } }
            )
        }

        return GattServiceItem(
            uuid = serviceUuid,
            name = serviceName,
            isPrimary = service.type == BluetoothGattService.SERVICE_TYPE_PRIMARY,
            characteristics = chars
        )
    }

    companion object {
        fun resolveGattServiceName(uuid: UUID): String {
            val s = uuid.toString().uppercase()
            return when {
                s.startsWith("00001800") -> "Generic Access"
                s.startsWith("00001801") -> "Generic Attribute"
                s.startsWith("0000180A") -> "Device Information"
                s.startsWith("0000180F") -> "Battery Service"
                s.startsWith("0000180D") -> "Heart Rate"
                s.startsWith("00001812") -> "Human Interface Device (HID)"
                s.startsWith("00001802") -> "Immediate Alert"
                s.startsWith("00001803") -> "Link Loss"
                s.startsWith("00001804") -> "Tx Power"
                s.startsWith("00001805") -> "Current Time"
                s.startsWith("00001809") -> "Health Thermometer"
                s.startsWith("0000181A") -> "Environmental Sensing"
                s.startsWith("0000FE2C") -> "Fast Pair (Google)"
                s.startsWith("0000FEE0") -> "Mi Band / Wearable Service"
                s.startsWith("0000FD6F") -> "Exposure Notification"
                else -> "Service Personnalisé (${s.take(8)})"
            }
        }

        fun resolveGattCharacteristicName(uuid: UUID): String {
            val s = uuid.toString().uppercase()
            return when {
                s.startsWith("00002A00") -> "Device Name"
                s.startsWith("00002A01") -> "Appearance"
                s.startsWith("00002A02") -> "Peripheral Privacy Flag"
                s.startsWith("00002A04") -> "Peripheral Preferred Connection Parameters"
                s.startsWith("00002A19") -> "Battery Level (%)"
                s.startsWith("00002A24") -> "Model Number String"
                s.startsWith("00002A25") -> "Serial Number String"
                s.startsWith("00002A26") -> "Firmware Revision String"
                s.startsWith("00002A27") -> "Hardware Revision String"
                s.startsWith("00002A28") -> "Software Revision String"
                s.startsWith("00002A29") -> "Manufacturer Name String"
                s.startsWith("00002A37") -> "Heart Rate Measurement"
                s.startsWith("00002A38") -> "Body Sensor Location"
                s.startsWith("00002A06") -> "Alert Level"
                s.startsWith("00002A07") -> "Tx Power Level"
                else -> "Caractéristique (${s.take(8)})"
            }
        }

        fun calculateDistance(rssi: Int, txPower: Int): Double {
            if (rssi == 0) return -1.0
            val ratio = rssi.toDouble() / txPower.toDouble()
            return if (ratio < 1.0) {
                Math.pow(ratio, 10.0)
            } else {
                val dist = (0.89976) * Math.pow(ratio, 7.7095) + 0.111
                (Math.round(dist * 10.0) / 10.0).coerceIn(0.1, 50.0)
            }
        }
    }
}
