package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.NdefRecordItem
import com.example.data.model.NfcMemoryPage
import com.example.data.model.NfcTagData
import com.example.ui.components.DiagnosticMetricItem
import com.example.viewmodel.NfcViewModel
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NfcScreen(
    viewModel: NfcViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var copiedNotice by remember { mutableStateOf<String?>(null) }
    var selectedInspectorTab by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("nfc_screen")
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.nfc_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Analyse approfondie RF 13.56 MHz, SAK, ATQA, NDEF & Smartcard",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isEnabled) Color(0xFF00E5FF) else Color(0xFFFF5252))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (uiState.isEnabled) "13.56 MHz" else "Inactif",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Demo Tags Quick Bar (Allows immediate interaction and test on emulator/no card)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXEMPLES & SIMULATION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tester sans carte",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.demoTags.forEach { demo ->
                        val isCurrent = uiState.currentTag?.uidHex == demo.uidHex
                        AssistChip(
                            onClick = { viewModel.loadDemoTag(demo) },
                            label = {
                                Text(
                                    text = when {
                                        demo.tagType.contains("NTAG") -> "🏷️ NTAG215"
                                        demo.tagType.contains("SmartCard") || demo.smartCardCategory != null -> "💳 Carte EMV"
                                        demo.tagType.contains("MIFARE Classic") -> "🔑 Mifare 1K"
                                        demo.tagType.contains("Calypso") -> "🚆 Calypso Navigo"
                                        else -> demo.tagType.take(12)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = if (isCurrent) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                },
                                labelColor = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                        )
                    }
                }
            }
        }

        // NFC Hardware Status & Radar Visualizer
        item {
            if (!uiState.isSupported) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "NFC Non Supporté Matériellement",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Vous pouvez utiliser les tags de simulation ci-dessus pour inspecter l'interface et les protocoles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else if (!uiState.isEnabled) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "NFC Désactivé sur l'appareil",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Activez le contrôleur NFC dans vos paramètres Android pour scanner des badges et cartes physiques.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.nfc_open_settings))
                        }
                    }
                }
            } else {
                // High-Tech Cyber Antenna Radar Visualizer
                NfcAntennaRadar(
                    isTagDetected = uiState.currentTag != null,
                    statusText = uiState.statusMessage
                )
            }
        }

        // Active / Selected Tag: Holographic Smartcard & Detailed Inspector
        uiState.currentTag?.let { tag ->
            item {
                NfcHolographicCard(
                    tag = tag,
                    onCopyUid = {
                        clipboardManager.setText(AnnotatedString(tag.uidHex))
                        copiedNotice = "UID ${tag.uidHex} copié"
                    },
                    onShareReport = {
                        val report = viewModel.generateDiagnosticReport(tag)
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, report)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Partager le rapport NFC"))
                    }
                )
            }

            // Copy feedback toast banner if triggered
            if (copiedNotice != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = copiedNotice ?: "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF00E5FF),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "OK",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { copiedNotice = null }
                            )
                        }
                    }
                }
            }

            // Diagnostic Inspector Tabs
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("nfc_inspector_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        TabRow(
                            selectedTabIndex = selectedInspectorTab,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.primary,
                            divider = {}
                        ) {
                            Tab(
                                selected = selectedInspectorTab == 0,
                                onClick = { selectedInspectorTab = 0 },
                                text = { Text(stringResource(R.string.nfc_tab_overview), style = MaterialTheme.typography.labelMedium) },
                                icon = { Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = selectedInspectorTab == 1,
                                onClick = { selectedInspectorTab = 1 },
                                text = {
                                    Text(
                                        "${stringResource(R.string.nfc_tab_ndef)} (${tag.ndefRecords.size})",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                icon = { Icon(Icons.Outlined.CreditCard, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = selectedInspectorTab == 2,
                                onClick = { selectedInspectorTab = 2 },
                                text = { Text(stringResource(R.string.nfc_tab_memory), style = MaterialTheme.typography.labelMedium) },
                                icon = { Icon(Icons.Outlined.Memory, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                            Tab(
                                selected = selectedInspectorTab == 3,
                                onClick = { selectedInspectorTab = 3 },
                                text = { Text(stringResource(R.string.nfc_tab_rf), style = MaterialTheme.typography.labelMedium) },
                                icon = { Icon(Icons.Outlined.Radio, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        when (selectedInspectorTab) {
                            0 -> NfcOverviewTab(tag = tag)
                            1 -> NfcNdefTab(tag = tag, context = context)
                            2 -> NfcMemoryTab(tag = tag)
                            3 -> NfcRfTab(tag = tag)
                        }
                    }
                }
            }
        }

        // History of scanned tags
        if (uiState.scanHistory.size > 1) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "HISTORIQUE DES TAGS (${uiState.scanHistory.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedButton(
                        onClick = { viewModel.clearHistory() },
                        contentPadding = ButtonDefaults.TextButtonContentPadding,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Effacer", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            items(uiState.scanHistory) { historyTag ->
                val isSelected = uiState.currentTag?.uidHex == historyTag.uidHex
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectTagFromHistory(historyTag) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        }
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (historyTag.smartCardCategory != null) Icons.Default.CreditCard else Icons.Default.Nfc,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = historyTag.uidHex,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = historyTag.tagType,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${historyTag.technologies.size} Techs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Animated High-Tech Canvas Radar Visualizer for NFC Antenna Field.
 */
@Composable
fun NfcAntennaRadar(
    isTagDetected: Boolean,
    statusText: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radarTransition")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweepAngle"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D1420)
        ),
        border = BorderStroke(1.dp, Color(0xFF1E2D42))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val primaryColor = Color(0xFF00E5FF)
                val accentTeal = Color(0xFF00F5A0)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxRadius = size.width / 2f - 4.dp.toPx()

                    // Background grid circles
                    drawCircle(
                        color = Color(0xFF172335),
                        radius = maxRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF172335),
                        radius = maxRadius * 0.66f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF172335),
                        radius = maxRadius * 0.33f,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )

                    // Expanding RF Pulse Wave
                    val waveRadius = maxRadius * pulseProgress
                    val waveAlpha = (1f - pulseProgress).coerceIn(0f, 1f) * 0.8f
                    drawCircle(
                        color = primaryColor.copy(alpha = waveAlpha),
                        radius = waveRadius,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Secondary offset wave
                    val wave2Progress = (pulseProgress + 0.5f) % 1f
                    val wave2Radius = maxRadius * wave2Progress
                    val wave2Alpha = (1f - wave2Progress).coerceIn(0f, 1f) * 0.5f
                    drawCircle(
                        color = accentTeal.copy(alpha = wave2Alpha),
                        radius = wave2Radius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Radar sweep beam line
                    val rad = Math.toRadians(sweepAngle.toDouble())
                    val endX = center.x + (maxRadius * cos(rad)).toFloat()
                    val endY = center.y + (maxRadius * sin(rad)).toFloat()
                    drawLine(
                        brush = Brush.linearGradient(
                            listOf(primaryColor, primaryColor.copy(alpha = 0.1f)),
                            start = center,
                            end = Offset(endX, endY)
                        ),
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    // Crosshair tick marks (8 directions)
                    for (i in 0 until 8) {
                        val angleDeg = i * 45.0
                        val rRad = Math.toRadians(angleDeg)
                        val innerX = center.x + ((maxRadius - 8.dp.toPx()) * cos(rRad)).toFloat()
                        val innerY = center.y + ((maxRadius - 8.dp.toPx()) * sin(rRad)).toFloat()
                        val outerX = center.x + (maxRadius * cos(rRad)).toFloat()
                        val outerY = center.y + (maxRadius * sin(rRad)).toFloat()
                        drawLine(
                            color = primaryColor.copy(alpha = 0.4f),
                            start = Offset(innerX, innerY),
                            end = Offset(outerX, outerY),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    // Center target indicator
                    drawCircle(
                        color = primaryColor.copy(alpha = 0.25f),
                        radius = 16.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = primaryColor,
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }

                Icon(
                    imageVector = Icons.Default.Sensors,
                    contentDescription = null,
                    tint = if (isTagDetected) Color(0xFF00F5A0) else Color(0xFF00E5FF),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isTagDetected) "Tag Connecté au Champ RF" else "Antenne NFC Active (13.56 MHz)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isTagDetected) Color(0xFF00F5A0) else Color(0xFF00E5FF)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Photorealistic Holographic Contactless Card Visualization.
 */
@Composable
fun NfcHolographicCard(
    tag: NfcTagData,
    onCopyUid: () -> Unit,
    onShareReport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("nfc_holographic_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.5.dp,
            Brush.linearGradient(
                listOf(
                    Color(0xFF00E5FF).copy(alpha = 0.8f),
                    Color(0xFF7C4DFF).copy(alpha = 0.4f),
                    Color(0xFF00F5A0).copy(alpha = 0.6f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF142032),
                            Color(0xFF0A1019)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // Top Row: Chip family badge & contactless wave symbol
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = tag.nfcForumType ?: "NFC TAG",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (tag.smartCardCategory != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFF00F5A0).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF00F5A0).copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "SMARTCARD",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    color = Color(0xFF00F5A0),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Contactless Icon Graphic
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Contactless",
                        tint = Color(0xFF00E5FF),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Middle Row: Metallic Microchip Icon + Manufacturer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Golden Microchip Visual
                    Surface(
                        modifier = Modifier.size(width = 46.dp, height = 34.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFD4AF37),
                        border = BorderStroke(1.dp, Color(0xFFFFE082))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                drawLine(
                                    color = Color(0xFF997A15),
                                    start = Offset(w * 0.33f, 0f),
                                    end = Offset(w * 0.33f, h),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color(0xFF997A15),
                                    start = Offset(w * 0.66f, 0f),
                                    end = Offset(w * 0.66f, h),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color(0xFF997A15),
                                    start = Offset(0f, h * 0.5f),
                                    end = Offset(w, h * 0.5f),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = tag.manufacturer.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tag.tagType,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // UID Monospace Highlight
                Text(
                    text = "IDENTIFIANT UNIQUE (UID)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = tag.uidHex,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF00E5FF)
                )
                Text(
                    text = "UID Décimal : ${tag.uidDec}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Action Bar: Copier UID & Partager Rapport
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onCopyUid,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E2D42),
                            contentColor = Color(0xFF00E5FF)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copier UID", style = MaterialTheme.typography.labelMedium)
                    }

                    Button(
                        onClick = onShareReport,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00E5FF),
                            contentColor = Color(0xFF00363F)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Partager Rapport", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Tab 0: Overview (Synthèse).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NfcOverviewTab(tag: NfcTagData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Smartcard Detection Banner if applicable
        tag.smartCardCategory?.let { category ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF00F5A0).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFF00F5A0).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = Color(0xFF00F5A0),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "DÉTECTION SPÉCIALISÉE SMARTCARD",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00F5A0),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiagnosticMetricItem(
                label = "Fabricant",
                value = tag.manufacturer,
                icon = Icons.Default.Memory,
                modifier = Modifier.weight(1f)
            )
            DiagnosticMetricItem(
                label = "Standard NFC Forum",
                value = tag.nfcForumType ?: "ISO/IEC 14443",
                badge = tag.sakDecoded?.take(16),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DiagnosticMetricItem(
                label = "Mémoire Totale",
                value = tag.memorySizeBytes?.let { "$it octets" } ?: "Non spécifié",
                badge = if (tag.isWritable == true) "Inscriptible" else if (tag.isWritable == false) "Lecture Seule" else null,
                modifier = Modifier.weight(1f)
            )
            DiagnosticMetricItem(
                label = "Enregistrements NDEF",
                value = "${tag.ndefRecords.size} enregistrement(s)",
                badge = if (tag.ndefRecords.isNotEmpty()) "NDEF Détecté" else "Brut",
                modifier = Modifier.weight(1f)
            )
        }

        // Technologies Chips
        Text(
            text = "Technologies Détectées",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            tag.technologies.forEach { tech ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = tech,
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Tab 1: NDEF Records.
 */
@Composable
fun NfcNdefTab(tag: NfcTagData, context: Context) {
    if (tag.ndefRecords.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.CreditCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Aucun enregistrement NDEF",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ce tag utilise un formatage brut (propriétaire ou non formaté en NDEF).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tag.ndefRecords.forEachIndexed { index, record ->
                NdefRecordCard(index = index + 1, record = record, context = context)
            }
        }
    }
}

/**
 * Tab 2: Memory & Hex Dump.
 */
@Composable
fun NfcMemoryTab(tag: NfcTagData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Hex Dump View
        Text(
            text = "DUMP HEXADÉCIMAL (16 OCTETS / LIGNE)",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF090E17),
            border = BorderStroke(1.dp, Color(0xFF1C2A3D))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = tag.rawDumpHex ?: "Aucun dump mémoire brut disponible pour ce type de tag.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    ),
                    color = Color(0xFF00E5FF)
                )
            }
        }

        // Memory Pages list if present
        if (tag.memoryPages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "PAGES MÉMOIRE DÉCODÉES (${tag.memoryPages.size} PAGES)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            tag.memoryPages.forEach { page ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Page %02d".format(page.pageIndex),
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                page.description?.let { desc ->
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${page.hexData}  |  ${page.asciiData}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tab 3: Low-level RF layer (ATQA, SAK, Historical bytes, APDU).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NfcRfTab(tag: NfcTagData) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "MÉTADONNÉES BAS-NIVEAU ISO / RADIOFRÉQUENCE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // SAK Details
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SAK (Select Acknowledge)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = tag.sak ?: "N/A",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tag.sakDecoded ?: "Valeur SAK non renseignée ou tag non ISO 14443-3A",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // ATQA Details
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ATQA (Answer To Request A)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Trame d'anticollision et dimension d'UID",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = tag.atqa ?: "N/A",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Historical Bytes / ATS if IsoDep
        tag.historicalBytes?.let { hist ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Historical Bytes (ATS / Réponse ISO 14443-4)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = hist,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Application Data (NfcB)
        tag.applicationData?.let { appData ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Application Data (ISO 14443-3B)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = appData,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * NDEF Record Card with URL launch action, copy, and hex viewer.
 */
@Composable
fun NdefRecordCard(
    index: Int,
    record: NdefRecordItem,
    context: Context
) {
    var expandedHex by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#$index • ${record.tnf}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Type: ${record.type}",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = record.payloadString,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If URI is present, provide direct button to open it
                if (record.uriToOpen != null) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(record.uriToOpen))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ouvrir", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { clipboardManager.setText(AnnotatedString(record.payloadString)) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copier",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (expandedHex) "Masquer Hex" else "Voir Hex",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { expandedHex = !expandedHex }
                    )
                }
            }

            AnimatedVisibility(visible = expandedHex) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Surface(
                        color = Color(0xFF090E17),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, Color(0xFF1C2A3D))
                    ) {
                        Text(
                            text = record.payloadHex,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(8.dp),
                            color = Color(0xFF00E5FF)
                        )
                    }
                }
            }
        }
    }
}
