package com.example

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.PermissionRequiredBanner
import com.example.ui.screens.BluetoothScreen
import com.example.ui.screens.NfcScreen
import com.example.ui.screens.WifiScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.BluetoothViewModel
import com.example.viewmodel.NfcViewModel
import com.example.viewmodel.WifiViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

enum class DiagnosticTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    NFC(R.string.tab_nfc, Icons.Filled.Nfc, Icons.Outlined.Nfc, "tab_nfc"),
    WIFI(R.string.tab_wifi, Icons.Filled.Wifi, Icons.Outlined.Wifi, "tab_wifi"),
    BLUETOOTH(R.string.tab_bluetooth, Icons.Filled.Bluetooth, Icons.Outlined.Bluetooth, "tab_bluetooth")
}

class MainActivity : ComponentActivity(), NfcAdapter.ReaderCallback {

    private var nfcAdapter: NfcAdapter? = null
    private val nfcViewModel: NfcViewModel by viewModels()
    private val wifiViewModel: WifiViewModel by viewModels()
    private val bluetoothViewModel: BluetoothViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        checkNfcAvailability()

        // Handle initial NFC tag intent if launched via NFC tap
        handleNfcIntent(intent)

        setContent {
            MyApplicationTheme {
                MainAppScreen(
                    nfcViewModel = nfcViewModel,
                    wifiViewModel = wifiViewModel,
                    bluetoothViewModel = bluetoothViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkNfcAvailability()
        enableNfcReaderMode()
    }

    override fun onPause() {
        super.onPause()
        disableNfcReaderMode()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNfcIntent(intent)
    }

    private fun checkNfcAvailability() {
        val adapter = nfcAdapter
        val isSupported = adapter != null
        val isEnabled = adapter?.isEnabled == true
        nfcViewModel.updateNfcAvailability(supported = isSupported, enabled = isEnabled)
    }

    private fun enableNfcReaderMode() {
        val adapter = nfcAdapter ?: return
        if (adapter.isEnabled) {
            val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
            adapter.enableReaderMode(this, this, flags, Bundle())

            // Also configure Foreground Dispatch as robust fallback
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            val techFilter = IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
            val ndefFilter = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                addDataType("*/*")
            }
            val tagFilter = IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            adapter.enableForegroundDispatch(this, pendingIntent, arrayOf(ndefFilter, techFilter, tagFilter), null)
        }
    }

    private fun disableNfcReaderMode() {
        val adapter = nfcAdapter ?: return
        try {
            adapter.disableReaderMode(this)
            adapter.disableForegroundDispatch(this)
        } catch (_: Exception) {}
    }

    private fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    android.os.VibrationEffect.createOneShot(70, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(android.os.VibrationEffect.createOneShot(70, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(70)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onTagDiscovered(tag: Tag?) {
        if (tag != null) {
            triggerHapticFeedback()
            runOnUiThread {
                nfcViewModel.onTagDiscovered(tag)
            }
        }
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        if (action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
            action == NfcAdapter.ACTION_TECH_DISCOVERED ||
            action == NfcAdapter.ACTION_TAG_DISCOVERED
        ) {
            val tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            if (tag != null) {
                nfcViewModel.onTagDiscovered(tag)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainAppScreen(
    nfcViewModel: NfcViewModel,
    wifiViewModel: WifiViewModel,
    bluetoothViewModel: BluetoothViewModel
) {
    var selectedTab by remember { mutableStateOf(DiagnosticTab.NFC) }

    // Dynamic Permission Handling for Android 15+ & modern Android
    val requiredPermissions = remember {
        val list = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(Manifest.permission.BLUETOOTH_SCAN)
            list.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }
        list
    }

    val permissionsState = rememberMultiplePermissionsState(permissions = requiredPermissions)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "ANALYSEUR SANS FIL MULTI-PROTOCOLE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .testTag("bottom_navigation_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                DiagnosticTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = stringResource(tab.titleRes)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(tab.titleRes),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Permission request banner if needed for Wi-Fi / BT
            if (!permissionsState.allPermissionsGranted && selectedTab != DiagnosticTab.NFC) {
                PermissionRequiredBanner(
                    title = stringResource(R.string.permission_required_title),
                    description = stringResource(R.string.permission_required_desc),
                    onRequestPermission = { permissionsState.launchMultiplePermissionRequest() },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "tabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        DiagnosticTab.NFC -> NfcScreen(viewModel = nfcViewModel)
                        DiagnosticTab.WIFI -> WifiScreen(viewModel = wifiViewModel)
                        DiagnosticTab.BLUETOOTH -> BluetoothScreen(viewModel = bluetoothViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
