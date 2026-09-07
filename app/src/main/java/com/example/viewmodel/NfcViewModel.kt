package com.example.viewmodel

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.NfcTagData
import com.example.nfc.NfcAudioSynthesizer
import com.example.nfc.NfcTagParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NfcUiState(
    val isSupported: Boolean = true,
    val isEnabled: Boolean = true,
    val isListening: Boolean = true,
    val currentTag: NfcTagData? = null,
    val scanHistory: List<NfcTagData> = emptyList(),
    val statusMessage: String = "En attente d'un tag ou d'une carte NFC...",
    val demoTags: List<NfcTagData> = NfcTagParser.getDemoTags(),
    val scanCount: Int = 0,
    val detectionTimestamp: Long = 0L
)

class NfcViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(NfcUiState())
    val uiState: StateFlow<NfcUiState> = _uiState.asStateFlow()

    init {
        // Pre-populate with first demo tag if on emulator/preview so screen is immediately rich and engaging
        val demos = NfcTagParser.getDemoTags()
        if (demos.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    currentTag = demos[0],
                    scanHistory = demos
                )
            }
        }
    }

    fun updateNfcAvailability(supported: Boolean, enabled: Boolean) {
        _uiState.update {
            it.copy(
                isSupported = supported,
                isEnabled = enabled,
                statusMessage = when {
                    !supported -> "NFC non supporté sur cet appareil physique"
                    !enabled -> "NFC désactivé. Activez-le dans les Paramètres système"
                    else -> "Prêt. Approchez une carte ou un tag NFC du dos du smartphone."
                }
            )
        }
    }

    fun onTagDiscovered(tag: Tag) {
        viewModelScope.launch {
            try {
                val tagData = NfcTagParser.parseTag(tag)
                NfcAudioSynthesizer.playDetectionChime()
                _uiState.update { current ->
                    val updatedHistory = listOf(tagData) + current.scanHistory.filter { it.uidHex != tagData.uidHex }
                    current.copy(
                        currentTag = tagData,
                        scanHistory = updatedHistory.take(25),
                        scanCount = current.scanCount + 1,
                        detectionTimestamp = System.currentTimeMillis(),
                        statusMessage = "Tag détecté ! UID: ${tagData.uidHex} (${tagData.tagType})"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(statusMessage = "Erreur lors de la lecture du tag: ${e.message}")
                }
            }
        }
    }

    fun loadDemoTag(tagData: NfcTagData) {
        NfcAudioSynthesizer.playDetectionChime()
        _uiState.update { current ->
            val updatedHistory = listOf(tagData) + current.scanHistory.filter { it.uidHex != tagData.uidHex }
            current.copy(
                currentTag = tagData,
                scanHistory = updatedHistory,
                scanCount = current.scanCount + 1,
                detectionTimestamp = System.currentTimeMillis(),
                statusMessage = "Tag détecté ! UID: ${tagData.uidHex} (${tagData.tagType})"
            )
        }
    }

    fun playDetectionSoundManual() {
        NfcAudioSynthesizer.playDetectionChime()
    }

    fun selectTagFromHistory(tagData: NfcTagData) {
        _uiState.update { it.copy(currentTag = tagData) }
    }

    fun clearHistory() {
        _uiState.update { it.copy(scanHistory = emptyList(), currentTag = null) }
    }

    fun generateDiagnosticReport(tag: NfcTagData): String {
        val sb = StringBuilder()
        sb.append("=== RAPPORT DIAGNOSTIC NFC ===\n")
        sb.append("Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(tag.timestamp))}\n\n")
        sb.append("--- IDENTIFICATION PHYSIQUE ---\n")
        sb.append("UID (Hex): ${tag.uidHex}\n")
        sb.append("UID (Décimal): ${tag.uidDec}\n")
        sb.append("Fabricant de la puce: ${tag.manufacturer}\n")
        sb.append("Type / Modèle: ${tag.tagType}\n")
        tag.nfcForumType?.let { sb.append("Standard NFC Forum: $it\n") }
        tag.memorySizeBytes?.let { sb.append("Taille Mémoire: $it octets\n") }
        tag.isWritable?.let { sb.append("Inscriptible: ${if (it) "Oui" else "Non (Lecture seule)"}\n") }

        sb.append("\n--- COUCHE RADIOFRÉQUENCE (RF) ---\n")
        sb.append("Technologies: ${tag.technologies.joinToString(", ")}\n")
        tag.atqa?.let { sb.append("ATQA: $it\n") }
        tag.sak?.let { sb.append("SAK: $it\n") }
        tag.sakDecoded?.let { sb.append("Interprétation SAK: $it\n") }
        tag.historicalBytes?.let { sb.append("Historical Bytes: $it\n") }
        tag.applicationData?.let { sb.append("Application Data: $it\n") }
        tag.smartCardCategory?.let { sb.append("Diagnostic Carte à Puce: $it\n") }

        if (tag.ndefRecords.isNotEmpty()) {
            sb.append("\n--- ENREGISTREMENTS NDEF (${tag.ndefRecords.size}) ---\n")
            tag.ndefRecords.forEachIndexed { i, record ->
                sb.append("[#${i + 1}] Type: ${record.type} | TNF: ${record.tnf}\n")
                sb.append("      Contenu: ${record.payloadString}\n")
                sb.append("      Hex: ${record.payloadHex}\n")
            }
        }

        tag.rawDumpHex?.let {
            sb.append("\n--- DUMP HEXADÉCIMAL ---\n")
            sb.append(it)
            sb.append("\n")
        }

        sb.append("\nGénéré par Wireless Diagnostic Tool (Android 15+)")
        return sb.toString()
    }
}
