package com.example.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.example.data.model.NetworkHost
import com.example.data.model.WifiConnectionDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.FileReader
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.min

class WifiInspector(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val nsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    fun getWifiConnectionDetails(): WifiConnectionDetails {
        try {
            val activeNetwork: Network? = connectivityManager.activeNetwork
            val capabilities = activeNetwork?.let { connectivityManager.getNetworkCapabilities(it) }
            val linkProperties: LinkProperties? = activeNetwork?.let { connectivityManager.getLinkProperties(it) }

            val hasWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

            if (!hasWifi || activeNetwork == null) {
                return WifiConnectionDetails(isConnected = false)
            }

            var wifiInfo: WifiInfo? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                wifiInfo = capabilities?.transportInfo as? WifiInfo
            }
            if (wifiInfo == null) {
                @Suppress("DEPRECATION")
                wifiInfo = wifiManager.connectionInfo
            }

            var rawSsid = wifiInfo?.ssid?.replace("\"", "") ?: "Wi-Fi Connecté"
            if (rawSsid == "<unknown ssid>") {
                rawSsid = "Wi-Fi (SSID masqué - Autorisation requise)"
            }
            val bssid = wifiInfo?.bssid ?: "--:--:--:--:--:--"
            val rssi = wifiInfo?.rssi ?: 0
            val linkSpeed = wifiInfo?.linkSpeed ?: 0
            val freq = wifiInfo?.frequency ?: 0

            val band = when {
                freq in 2400..2500 -> "2.4 GHz"
                freq in 4900..5900 -> "5 GHz"
                freq in 5925..7125 -> "6 GHz (Wi-Fi 6E/7)"
                else -> if (freq > 0) "$freq MHz" else "Inconnu"
            }

            var localIp = "0.0.0.0"
            var netmask = "255.255.255.0"
            var gateway = "0.0.0.0"
            var dns1 = "0.0.0.0"
            var dns2: String? = null

            if (linkProperties != null) {
                for (linkAddress in linkProperties.linkAddresses) {
                    val addr = linkAddress.address
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        localIp = addr.hostAddress ?: "0.0.0.0"
                        val prefixLength = linkAddress.prefixLength
                        netmask = prefixLengthToSubnetMask(prefixLength)
                        break
                    }
                }

                val routes = linkProperties.routes
                for (route in routes) {
                    if (route.isDefaultRoute && route.gateway is Inet4Address) {
                        gateway = route.gateway?.hostAddress ?: "0.0.0.0"
                        break
                    }
                }

                val dnsServers = linkProperties.dnsServers.filterIsInstance<Inet4Address>()
                if (dnsServers.isNotEmpty()) {
                    dns1 = dnsServers[0].hostAddress ?: "0.0.0.0"
                    if (dnsServers.size > 1) {
                        dns2 = dnsServers[1].hostAddress
                    }
                }
            }

            @Suppress("DEPRECATION")
            val dhcp = wifiManager.dhcpInfo
            val dhcpServer = if (dhcp != null && dhcp.serverAddress != 0) {
                intToIp(dhcp.serverAddress)
            } else gateway

            return WifiConnectionDetails(
                isConnected = true,
                ssid = rawSsid,
                bssid = bssid,
                rssiDbm = rssi,
                linkSpeedMbps = linkSpeed,
                frequencyMhz = freq,
                band = band,
                ipAddress = localIp,
                gateway = gateway,
                netmask = netmask,
                dhcpServer = dhcpServer,
                dns1 = dns1,
                dns2 = dns2
            )
        } catch (e: Exception) {
            Log.e("WifiInspector", "Erreur lors de la récupération des détails Wi-Fi", e)
            return WifiConnectionDetails(isConnected = false)
        }
    }

    /**
     * Fast subnet sweep to discover active hosts.
     */
    suspend fun scanSubnet(
        localIp: String,
        gateway: String,
        onProgress: (Int, Int) -> Unit
    ): List<NetworkHost> = withContext(Dispatchers.IO) {
        val discoveredHosts = mutableListOf<NetworkHost>()

        if (localIp == "0.0.0.0" || !localIp.contains(".")) {
            return@withContext emptyList()
        }

        val prefix = localIp.substringBeforeLast(".")
        val totalHosts = 254
        var scannedCount = 0
        val semaphore = Semaphore(35) // Concurrency throttle

        val arpTable = readArpTable()

        val jobs = (1..totalHosts).map { hostNum ->
            async {
                val targetIp = "$prefix.$hostNum"
                val host = semaphore.withPermit {
                    checkHostReachable(targetIp, isGateway = (targetIp == gateway), arpTable[targetIp])
                }
                synchronized(discoveredHosts) {
                    scannedCount++
                    onProgress(scannedCount, totalHosts)
                    if (host != null) {
                        discoveredHosts.add(host)
                    }
                }
                host
            }
        }

        jobs.awaitAll()
        discoveredHosts.sortedBy { host ->
            host.ip.substringAfterLast(".").toIntOrNull() ?: 0
        }
    }

    private fun checkHostReachable(ip: String, isGateway: Boolean, arpMac: String?): NetworkHost? {
        val startTime = System.currentTimeMillis()
        var reachable = false
        val openPorts = mutableListOf<Int>()
        var hostName: String? = null

        // 1. Try common ports with quick timeout
        val testPorts = listOf(80, 443, 53, 8080, 22, 5353)
        for (port in testPorts) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), 180)
                    reachable = true
                    openPorts.add(port)
                }
            } catch (_: Exception) {}
        }

        // 2. Try ICMP / standard isReachable if socket didn't hit
        if (!reachable) {
            try {
                val inetAddr = InetAddress.getByName(ip)
                if (inetAddr.isReachable(220)) {
                    reachable = true
                }
            } catch (_: Exception) {}
        }

        // Also if ARP table has entry, host is present on LAN
        if (!reachable && arpMac != null && arpMac != "00:00:00:00:00:00") {
            reachable = true
        }

        if (reachable || isGateway) {
            val responseTime = System.currentTimeMillis() - startTime
            try {
                val inet = InetAddress.getByName(ip)
                val canonical = inet.canonicalHostName
                if (canonical != ip) {
                    hostName = canonical
                } else {
                    val rawHost = inet.hostName
                    if (rawHost != ip) {
                        hostName = rawHost
                    }
                }
            } catch (_: Exception) {}

            val defaultName = when {
                isGateway -> "Passerelle / Routeur Principal"
                ip.endsWith(".1") -> "Routeur / Box"
                else -> null
            }

            return NetworkHost(
                ip = ip,
                mac = arpMac ?: "(Protégé - Android 10+)",
                hostname = hostName ?: defaultName,
                responseTimeMs = if (responseTime == 0L) 1L else responseTime,
                isGateway = isGateway,
                openPorts = openPorts
            )
        }

        return null
    }

    /**
     * Discovers mDNS / DNS-SD services (Chromecast, printers, Apple AirPlay, workstations).
     */
    fun startMdnsDiscovery(serviceType: String): Flow<NetworkHost> = callbackFlow {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d("WifiInspector", "mDNS Discovery démarré pour $regType")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                try {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.w("WifiInspector", "Échec résolution mDNS: $errorCode")
                        }

                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val hostAddress = serviceInfo.host?.hostAddress
                            if (hostAddress != null) {
                                val host = NetworkHost(
                                    ip = hostAddress,
                                    hostname = serviceInfo.serviceName,
                                    serviceInfo = "${serviceInfo.serviceType} (Port ${serviceInfo.port})"
                                )
                                trySend(host)
                            }
                        }
                    })
                } catch (e: Exception) {
                    Log.e("WifiInspector", "Erreur lors de la résolution du service", e)
                }
            }

            override fun onServiceLost(service: NsdServiceInfo) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                close()
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }

        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (_: Exception) {}
        }
    }

    private fun readArpTable(): Map<String, String> {
        val result = mutableMapOf<String, String>()
        try {
            BufferedReader(FileReader("/proc/net/arp")).use { reader ->
                reader.forEachLine { line ->
                    val tokens = line.split("\\s+".toRegex())
                    if (tokens.size >= 4 && tokens[0].matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                        val ip = tokens[0]
                        val mac = tokens[3]
                        if (mac != "00:00:00:00:00:00") {
                            result[ip] = mac
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return result
    }

    private fun prefixLengthToSubnetMask(prefixLength: Int): String {
        val mask = (0xFFFFFFFFL shl (32 - prefixLength)).toInt()
        return "%d.%d.%d.%d".format(
            (mask shr 24) and 0xFF,
            (mask shr 16) and 0xFF,
            (mask shr 8) and 0xFF,
            mask and 0xFF
        )
    }

    private fun intToIp(i: Int): String {
        return "%d.%d.%d.%d".format(
            i and 0xFF,
            (i shr 8) and 0xFF,
            (i shr 16) and 0xFF,
            (i shr 24) and 0xFF
        )
    }

    /**
     * Pings a specific host or domain (e.g. 1.1.1.1, google.com, local gateway) and measures exact round-trip time.
     */
    suspend fun pingHost(target: String): com.example.data.model.NetworkPingResult = withContext(Dispatchers.IO) {
        val cleanTarget = target.trim().replace("https://", "").replace("http://", "").substringBefore("/")
        val startTime = System.currentTimeMillis()
        try {
            val inet = InetAddress.getByName(cleanTarget)
            // Try socket connect on 80/443 for fast internet targets, or icmp
            var reachable = false
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(inet, 443), 800)
                    reachable = true
                }
            } catch (_: Exception) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(inet, 80), 800)
                        reachable = true
                    }
                } catch (_: Exception) {
                    reachable = inet.isReachable(1000)
                }
            }

            val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(1L)
            if (reachable) {
                com.example.data.model.NetworkPingResult(
                    target = cleanTarget,
                    isReachable = true,
                    latencyMs = latency,
                    message = "Réponse de ${inet.hostAddress} : temps = ${latency}ms"
                )
            } else {
                com.example.data.model.NetworkPingResult(
                    target = cleanTarget,
                    isReachable = false,
                    latencyMs = 0L,
                    message = "Hôte inaccessible ou délai dépassé (>1000ms)"
                )
            }
        } catch (e: Exception) {
            com.example.data.model.NetworkPingResult(
                target = cleanTarget,
                isReachable = false,
                latencyMs = 0L,
                message = "Erreur de résolution DNS : ${e.message ?: "Introuvable"}"
            )
        }
    }

    /**
     * Executes real HTTP bandwidth throughput test to evaluate link and Internet download performance.
     */
    suspend fun runSpeedBenchmark(
        onProgress: (progress: Float, currentSpeedMbps: Double, averageSpeedMbps: Double) -> Unit
    ): Double = withContext(Dispatchers.IO) {
        val testUrl = "https://speed.cloudflare.com/__down?bytes=10000000" // 10MB test stream
        var totalBytesRead = 0L
        val startTime = System.currentTimeMillis()

        try {
            val url = java.net.URL(testUrl)
            val connection = url.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = 4000
            connection.readTimeout = 7000
            connection.instanceFollowRedirects = true
            connection.connect()

            val totalBytesExpected = 10_000_000L
            val buffer = ByteArray(8192)
            val inputStream = connection.inputStream

            var lastSampleTime = System.currentTimeMillis()
            var bytesInSample = 0L

            while (true) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break
                totalBytesRead += bytesRead
                bytesInSample += bytesRead

                val now = System.currentTimeMillis()
                val elapsedSinceSample = now - lastSampleTime
                if (elapsedSinceSample >= 250) {
                    val currentSpeed = (bytesInSample * 8.0) / (elapsedSinceSample / 1000.0) / 1_000_000.0
                    val totalElapsedSec = (now - startTime) / 1000.0
                    val avgSpeed = if (totalElapsedSec > 0) (totalBytesRead * 8.0) / totalElapsedSec / 1_000_000.0 else 0.0
                    val progress = (totalBytesRead.toFloat() / totalBytesExpected.toFloat()).coerceIn(0f, 1f)
                    onProgress(progress, currentSpeed, avgSpeed)
                    bytesInSample = 0L
                    lastSampleTime = now
                }
            }
            inputStream.close()
            connection.disconnect()
        } catch (_: Exception) {
            // In case of offline/timeout, simulate with link speed factor if connected
        }

        val totalElapsedSec = (System.currentTimeMillis() - startTime) / 1000.0
        val finalAverage = if (totalElapsedSec > 0 && totalBytesRead > 0) {
            (totalBytesRead * 8.0) / totalElapsedSec / 1_000_000.0
        } else {
            0.0
        }
        onProgress(1f, finalAverage, finalAverage)
        finalAverage
    }
}
