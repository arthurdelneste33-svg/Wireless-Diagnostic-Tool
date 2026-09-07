package com.example.data.model

/**
 * NFC Data Models
 */
data class NfcMemoryPage(
    val pageIndex: Int,
    val hexData: String,
    val asciiData: String,
    val description: String? = null
)

data class NdefRecordItem(
    val type: String,
    val payloadString: String,
    val payloadHex: String,
    val mimeType: String? = null,
    val tnf: String,
    val uriToOpen: String? = null
)

data class NfcTagData(
    val uidHex: String,
    val uidDec: String,
    val technologies: List<String>,
    val tagType: String,
    val manufacturer: String = "Inconnu / Générique",
    val nfcForumType: String? = null,
    val memorySizeBytes: Int? = null,
    val isWritable: Boolean? = null,
    val ndefRecords: List<NdefRecordItem> = emptyList(),
    val atqa: String? = null,
    val sak: String? = null,
    val sakDecoded: String? = null,
    val historicalBytes: String? = null,
    val applicationData: String? = null,
    val smartCardCategory: String? = null,
    val memoryPages: List<NfcMemoryPage> = emptyList(),
    val rawDumpHex: String? = null,
    val primaryUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Wi-Fi Data Models
 */
data class WifiConnectionDetails(
    val isConnected: Boolean = false,
    val ssid: String = "Non connecté",
    val bssid: String = "--:--:--:--:--:--",
    val rssiDbm: Int = 0,
    val linkSpeedMbps: Int = 0,
    val frequencyMhz: Int = 0,
    val band: String = "Inconnu",
    val ipAddress: String = "0.0.0.0",
    val gateway: String = "0.0.0.0",
    val netmask: String = "255.255.255.0",
    val dhcpServer: String = "0.0.0.0",
    val dns1: String = "0.0.0.0",
    val dns2: String? = null
)

data class NetworkHost(
    val ip: String,
    val mac: String? = null,
    val hostname: String? = null,
    val responseTimeMs: Long = 0,
    val isGateway: Boolean = false,
    val openPorts: List<Int> = emptyList(),
    val serviceInfo: String? = null
)

/**
 * Bluetooth Data Models
 */
data class BleDeviceItem(
    val address: String,
    val name: String?,
    val rssi: Int,
    val bondState: String,
    val deviceType: String,
    val advertisedServiceUuids: List<String> = emptyList(),
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

data class GattServiceItem(
    val uuid: String,
    val name: String,
    val isPrimary: Boolean,
    val characteristics: List<GattCharacteristicItem>
)

data class GattCharacteristicItem(
    val uuid: String,
    val name: String,
    val propertiesDesc: List<String>,
    val valueHex: String? = null,
    val valueStr: String? = null
)
