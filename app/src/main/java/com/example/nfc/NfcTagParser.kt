package com.example.nfc

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import com.example.data.model.NdefRecordItem
import com.example.data.model.NfcMemoryPage
import com.example.data.model.NfcTagData
import java.math.BigInteger

object NfcTagParser {

    fun parseTag(tag: Tag): NfcTagData {
        val uid = tag.id
        val uidHex = uid?.toHexString() ?: "Inconnu"
        val uidDec = uid?.toDecimalString() ?: "0"

        val rawTechList = tag.techList.map { it.substringAfterLast(".") }
        val manufacturer = resolveManufacturer(uid)

        var detectedType = "Tag Générique"
        var nfcForumType: String? = null
        var memorySize: Int? = null
        var isWritable: Boolean? = null
        var atqa: String? = null
        var sak: String? = null
        var sakDecoded: String? = null
        var historicalBytes: String? = null
        var applicationData: String? = null
        var smartCardCategory: String? = null
        val memoryPages = mutableListOf<NfcMemoryPage>()
        val ndefRecords = mutableListOf<NdefRecordItem>()
        var primaryUri: String? = null
        var rawDumpHex: String? = null

        // 1. Check NfcA
        try {
            val nfcA = NfcA.get(tag)
            if (nfcA != null) {
                atqa = nfcA.atqa?.toHexString()
                val sakInt = nfcA.sak.toInt() and 0xFF
                sak = "0x%02X".format(sakInt)
                sakDecoded = decodeSak(sakInt)
                detectedType = "ISO 14443-3A (NfcA)"
                nfcForumType = "NFC Forum Type 2"
            }
        } catch (_: Exception) {}

        // 2. Check NfcB
        try {
            val nfcB = NfcB.get(tag)
            if (nfcB != null) {
                applicationData = nfcB.applicationData?.toHexString()
                detectedType = "ISO 14443-3B (NfcB)"
                nfcForumType = "NFC Forum Type 4"
            }
        } catch (_: Exception) {}

        // 3. Check IsoDep (Smartcards & Contactless Banking)
        try {
            val isoDep = IsoDep.get(tag)
            if (isoDep != null) {
                historicalBytes = isoDep.historicalBytes?.toHexString()
                    ?: isoDep.hiLayerResponse?.toHexString()
                detectedType = "ISO 14443-4 (IsoDep / SmartCard)"
                nfcForumType = "NFC Forum Type 4"
                smartCardCategory = probeSmartCardIsoDep(isoDep)
            }
        } catch (_: Exception) {}

        // 4. Check MifareClassic
        try {
            val mifare = MifareClassic.get(tag)
            if (mifare != null) {
                memorySize = mifare.size
                val typeDesc = when (mifare.type) {
                    MifareClassic.TYPE_CLASSIC -> "MIFARE Classic (${mifare.sectorCount} secteurs / ${mifare.blockCount} blocs)"
                    MifareClassic.TYPE_PLUS -> "MIFARE Plus (${mifare.sectorCount} secteurs)"
                    MifareClassic.TYPE_PRO -> "MIFARE Pro"
                    else -> "MIFARE Standard"
                }
                detectedType = "$typeDesc (${mifare.size} octets)"
                nfcForumType = "Propriétaire NXP (MIFARE Classic)"
            }
        } catch (_: Exception) {}

        // 5. Check MifareUltralight / NTAG
        try {
            val ultralight = MifareUltralight.get(tag)
            if (ultralight != null) {
                val (pages, refinedModel, ntagSize) = readMifareUltralightPages(ultralight)
                memoryPages.addAll(pages)
                if (ntagSize != null) memorySize = ntagSize

                detectedType = refinedModel ?: when (ultralight.type) {
                    MifareUltralight.TYPE_ULTRALIGHT -> "MIFARE Ultralight (64 octets)"
                    MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C (192 octets)"
                    else -> "MIFARE Ultralight / NTAG"
                }
                nfcForumType = "NFC Forum Type 2"
            }
        } catch (_: Exception) {}

        // 6. Check NfcF (FeliCa)
        try {
            val nfcF = NfcF.get(tag)
            if (nfcF != null) {
                detectedType = "JIS X 6319-4 (Sony FeliCa)"
                nfcForumType = "NFC Forum Type 3"
            }
        } catch (_: Exception) {}

        // 7. Check NfcV (Vicinity)
        try {
            val nfcV = NfcV.get(tag)
            if (nfcV != null) {
                detectedType = "ISO 15693 (NfcV Vicinity / ICODE)"
                nfcForumType = "NFC Forum Type 5"
            }
        } catch (_: Exception) {}

        // 8. Check NDEF
        try {
            val ndef = Ndef.get(tag)
            if (ndef != null) {
                memorySize = memorySize ?: ndef.maxSize
                isWritable = ndef.isWritable
                detectedType = "$detectedType [NDEF Formaté]"

                val message = ndef.cachedNdefMessage
                if (message != null) {
                    val parsed = parseNdefMessage(message)
                    ndefRecords.addAll(parsed)
                    primaryUri = parsed.firstOrNull { it.uriToOpen != null }?.uriToOpen
                    rawDumpHex = formatHexDump(message.toByteArray())
                }
            }
        } catch (_: Exception) {}

        // Fallback hex dump if no NDEF message
        if (rawDumpHex == null && memoryPages.isNotEmpty()) {
            val sb = StringBuilder()
            memoryPages.forEach { page ->
                sb.append("Page %02d [0x%02X]: %s | %s\n".format(page.pageIndex, page.pageIndex, page.hexData, page.asciiData))
            }
            rawDumpHex = sb.toString()
        }

        return NfcTagData(
            uidHex = uidHex,
            uidDec = uidDec,
            technologies = rawTechList,
            tagType = detectedType,
            manufacturer = manufacturer,
            nfcForumType = nfcForumType,
            memorySizeBytes = memorySize,
            isWritable = isWritable,
            ndefRecords = ndefRecords,
            atqa = atqa,
            sak = sak,
            sakDecoded = sakDecoded,
            historicalBytes = historicalBytes,
            applicationData = applicationData,
            smartCardCategory = smartCardCategory,
            memoryPages = memoryPages,
            rawDumpHex = rawDumpHex,
            primaryUri = primaryUri
        )
    }

    /**
     * Identifies chip manufacturer based on ISO/IEC 7816-6 / NFC Forum standard UID prefix.
     */
    fun resolveManufacturer(uid: ByteArray?): String {
        if (uid == null || uid.isEmpty()) return "Inconnu (UID absent)"
        val byte0 = uid[0].toInt() and 0xFF
        return when (byte0) {
            0x04 -> "NXP Semiconductors (MIFARE, NTAG, ICODE)"
            0x02 -> "STMicroelectronics (ST25, M24SR)"
            0x07 -> "Texas Instruments (Tag-it HF-I)"
            0x05 -> "Infineon Technologies (my-d, SLE)"
            0x16 -> "Sony Corporation (FeliCa)"
            0x1D -> "EM Microelectronic-Marin"
            0x2B -> "Maxim Integrated"
            0x01 -> "Motorola"
            0x03 -> "Hitachi"
            0x08 -> "Fujitsu"
            0x48 -> "Inside Secure"
            0x1E -> "Melexis"
            else -> "Fabricant ISO 7816-6 (Préfixe 0x%02X)".format(byte0)
        }
    }

    /**
     * Decodes SAK (Select Acknowledge) byte into human-readable tag architecture description.
     */
    fun decodeSak(sak: Int): String {
        return when (sak) {
            0x00 -> "NXP MIFARE Ultralight / NTAG213/215/216"
            0x08 -> "NXP MIFARE Classic 1K (ou MIFARE Plus 1K SL1)"
            0x09 -> "NXP MIFARE Classic Mini (0.3K)"
            0x10 -> "NXP MIFARE Plus 2K (SL2)"
            0x11 -> "NXP MIFARE Plus 4K (SL2)"
            0x18 -> "NXP MIFARE Classic 4K (ou MIFARE Plus 4K SL1)"
            0x20 -> "ISO/IEC 14443-4 (IsoDep / DESFire / Smart Card / EMV Bancaire)"
            0x28 -> "JCOP30 / MIFARE Classic 1K Emulé"
            0x38 -> "MIFARE Classic 4K Emulé"
            0x88 -> "Infineon MIFARE Classic 1K"
            0x98 -> "Gemplus MPCOS"
            else -> "SAK Standard 0x%02X".format(sak)
        }
    }

    /**
     * Sends safe non-destructive read-only APDU SELECT commands to probe for EMV, Calypso, and ICAO passports.
     */
    private fun probeSmartCardIsoDep(isoDep: IsoDep): String? {
        return try {
            isoDep.connect()
            isoDep.timeout = 400

            // 1. Probe PPSE (Proximity Payment System Environment - Contactless Bank Cards)
            val ppseApdu = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00, 0x0E,
                0x32, 0x50, 0x41, 0x59, 0x2E, 0x53, 0x59, 0x53, 0x2E, 0x44, 0x44, 0x46, 0x30, 0x31,
                0x00
            ) // SELECT "2PAY.SYS.DDF01"
            val ppseResp = isoDep.transceive(ppseApdu)
            if (ppseResp.size >= 2 && ppseResp[ppseResp.size - 2] == 0x90.toByte()) {
                return "Carte Bancaire Sans Contact (EMV / PPSE Détecté)"
            }

            // 2. Probe Calypso Transit (e.g. Navigo, Mobib, Opus)
            val calypsoApdu = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x00, 0x08,
                0x31, 0x54, 0x49, 0x43, 0x2E, 0x49, 0x43, 0x41, 0x00
            ) // SELECT "1TIC.ICA"
            val calypsoResp = isoDep.transceive(calypsoApdu)
            if (calypsoResp.size >= 2 && (calypsoResp[calypsoResp.size - 2] == 0x90.toByte() || calypsoResp[calypsoResp.size - 2] == 0x6A.toByte())) {
                return "Passe Transport Urbain (Calypso / Navigo)"
            }

            // 3. Probe ICAO e-Passport (eMRTD)
            val icaoApdu = byteArrayOf(
                0x00, 0xA4.toByte(), 0x04, 0x0C, 0x07,
                0xA0.toByte(), 0x00, 0x00, 0x02, 0x47, 0x10, 0x01
            )
            val icaoResp = isoDep.transceive(icaoApdu)
            if (icaoResp.size >= 2 && icaoResp[icaoResp.size - 2] == 0x90.toByte()) {
                return "Passeport Électronique ICAO (eMRTD)"
            }

            "Carte à Puce Sécurisée ISO 7816-4"
        } catch (_: Exception) {
            null
        } finally {
            try { isoDep.close() } catch (_: Exception) {}
        }
    }

    /**
     * Reads pages from a MIFARE Ultralight / NTAG chip and determines exact model.
     */
    private fun readMifareUltralightPages(
        ultralight: MifareUltralight
    ): Triple<List<NfcMemoryPage>, String?, Int?> {
        val pages = mutableListOf<NfcMemoryPage>()
        var refinedModel: String? = null
        var totalBytes: Int? = null

        try {
            ultralight.connect()
            ultralight.timeout = 400

            for (pageIndex in 0..12 step 4) {
                try {
                    val buffer = ultralight.readPages(pageIndex)
                    for (i in 0 until 4) {
                        val current = pageIndex + i
                        val chunk = buffer.copyOfRange(i * 4, (i + 1) * 4)
                        val hex = chunk.joinToString(" ") { "%02X".format(it) }
                        val ascii = chunk.map { if (it in 32..126) it.toInt().toChar() else '.' }.joinToString("")
                        val desc = when (current) {
                            0, 1 -> "UID / Numéro de série"
                            2 -> "Lock Bytes (Protection)"
                            3 -> "Capability Container (CC)"
                            4 -> "Début Données (TLV NDEF)"
                            else -> "Mémoire Utilisateur"
                        }
                        pages.add(NfcMemoryPage(current, hex, ascii, desc))
                    }
                } catch (_: Exception) {
                    break
                }
            }

            // Inspect CC (Page 3) to deduce NTAG model
            if (pages.size > 3) {
                val ccRaw = pages[3].hexData.replace(" ", "")
                if (ccRaw.startsWith("E1")) {
                    val sizeByte = ccRaw.substring(4, 6).toIntOrNull(16) ?: 0
                    when (sizeByte) {
                        0x12 -> {
                            refinedModel = "NXP NTAG213 (144 octets utiles)"
                            totalBytes = 180
                        }
                        0x3E -> {
                            refinedModel = "NXP NTAG215 (504 octets utiles)"
                            totalBytes = 540
                        }
                        0x6D, 0x6E -> {
                            refinedModel = "NXP NTAG216 (888 octets utiles)"
                            totalBytes = 924
                        }
                        0x06 -> {
                            refinedModel = "NXP MIFARE Ultralight (48 octets utiles)"
                            totalBytes = 64
                        }
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            try { ultralight.close() } catch (_: Exception) {}
        }

        return Triple(pages, refinedModel, totalBytes)
    }

    fun parseNdefMessage(message: NdefMessage): List<NdefRecordItem> {
        val list = mutableListOf<NdefRecordItem>()
        for (record in message.records) {
            val tnfStr = when (record.tnf) {
                NdefRecord.TNF_WELL_KNOWN -> "Well-Known (RTD)"
                NdefRecord.TNF_MIME_MEDIA -> "MIME Media"
                NdefRecord.TNF_ABSOLUTE_URI -> "Absolute URI"
                NdefRecord.TNF_EXTERNAL_TYPE -> "External Type"
                NdefRecord.TNF_UNKNOWN -> "Inconnu"
                NdefRecord.TNF_UNCHANGED -> "Inchangé"
                else -> "Autre (${record.tnf})"
            }

            var payloadStr = ""
            var mimeType: String? = null
            val typeStr = String(record.type, Charsets.US_ASCII)
            var uriToOpen: String? = null

            if (record.tnf == NdefRecord.TNF_WELL_KNOWN) {
                if (record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                    payloadStr = parseTextRecord(record.payload)
                } else if (record.type.contentEquals(NdefRecord.RTD_URI)) {
                    val uri = parseUriRecord(record.payload)
                    payloadStr = uri
                    uriToOpen = uri
                } else if (record.type.contentEquals(NdefRecord.RTD_SMART_POSTER)) {
                    payloadStr = "Smart Poster (Affiche interactive NDEF)"
                } else {
                    payloadStr = String(record.payload, Charsets.UTF_8)
                }
            } else if (record.tnf == NdefRecord.TNF_MIME_MEDIA) {
                mimeType = typeStr
                payloadStr = String(record.payload, Charsets.UTF_8)
            } else if (record.tnf == NdefRecord.TNF_ABSOLUTE_URI) {
                payloadStr = String(record.payload, Charsets.UTF_8)
                uriToOpen = payloadStr
            } else {
                payloadStr = String(record.payload, Charsets.UTF_8)
            }

            // If payload happens to be a URL, allow opening it
            if (uriToOpen == null && (payloadStr.startsWith("http://") || payloadStr.startsWith("https://"))) {
                uriToOpen = payloadStr
            }

            list.add(
                NdefRecordItem(
                    type = typeStr.ifEmpty { "Inconnu" },
                    payloadString = payloadStr.ifEmpty { "(Charge utile binaire)" },
                    payloadHex = record.payload.toHexString(),
                    mimeType = mimeType,
                    tnf = tnfStr,
                    uriToOpen = uriToOpen
                )
            )
        }
        return list
    }

    private fun parseTextRecord(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        return try {
            val statusByte = payload[0].toInt()
            val isUtf16 = (statusByte and 0x80) != 0
            val languageCodeLength = statusByte and 0x3F
            val charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8

            val langCode = String(payload, 1, languageCodeLength, Charsets.US_ASCII)
            val text = String(
                payload,
                1 + languageCodeLength,
                payload.size - 1 - languageCodeLength,
                charset
            )
            "[$langCode] $text"
        } catch (_: Exception) {
            String(payload, Charsets.UTF_8)
        }
    }

    private val URI_PREFIX_MAP = arrayOf(
        "",
        "http://www.",
        "https://www.",
        "http://",
        "https://",
        "tel:",
        "mailto:",
        "ftp://anonymous:anonymous@",
        "ftp://ftp.",
        "ftps://",
        "sftp://",
        "smb://",
        "nfs://",
        "ftp://",
        "dav://",
        "news:",
        "telnet://",
        "imap:",
        "rtsp://",
        "urn:",
        "pop:",
        "sip:",
        "sips:",
        "tftp:",
        "btspp://",
        "btl2cap://",
        "btgoep://",
        "tcpobex://",
        "irdaobex://",
        "file://",
        "urn:epc:id:",
        "urn:epc:tag:",
        "urn:epc:pat:",
        "urn:epc:raw:",
        "urn:epc:",
        "urn:nfc:"
    )

    private fun parseUriRecord(payload: ByteArray): String {
        if (payload.isEmpty()) return ""
        val prefixIndex = payload[0].toInt() and 0xFF
        val prefix = if (prefixIndex in URI_PREFIX_MAP.indices) URI_PREFIX_MAP[prefixIndex] else ""
        val suffix = String(payload, 1, payload.size - 1, Charsets.UTF_8)
        return prefix + suffix
    }

    fun formatHexDump(bytes: ByteArray): String {
        val sb = StringBuilder()
        for (i in bytes.indices step 16) {
            val end = minOf(i + 16, bytes.size)
            val chunk = bytes.copyOfRange(i, end)
            sb.append("%04X:  ".format(i))
            for (j in 0 until 16) {
                if (j < chunk.size) {
                    sb.append("%02X ".format(chunk[j]))
                } else {
                    sb.append("   ")
                }
                if (j == 7) sb.append(" ")
            }
            sb.append(" | ")
            for (b in chunk) {
                sb.append(if (b in 32..126) b.toInt().toChar() else '.')
            }
            sb.append("\n")
        }
        return sb.toString()
    }

    private fun ByteArray.toHexString(): String {
        return joinToString(":") { "%02X".format(it) }
    }

    private fun ByteArray.toDecimalString(): String {
        return try {
            BigInteger(1, this).toString(10)
        } catch (_: Exception) {
            "0"
        }
    }

    /**
     * Provides pre-configured high-fidelity diagnostic tags for testing or simulation.
     */
    fun getDemoTags(): List<NfcTagData> {
        return listOf(
            NfcTagData(
                uidHex = "04:E2:4B:9A:8C:63:80",
                uidDec = "1380918739182336",
                technologies = listOf("NfcA", "MifareUltralight", "Ndef"),
                tagType = "NXP NTAG215 (504 octets utiles) [NDEF Formaté]",
                manufacturer = "NXP Semiconductors (MIFARE, NTAG, ICODE)",
                nfcForumType = "NFC Forum Type 2",
                memorySizeBytes = 540,
                isWritable = true,
                atqa = "00:44",
                sak = "0x00",
                sakDecoded = "NXP MIFARE Ultralight / NTAG213/215/216",
                ndefRecords = listOf(
                    NdefRecordItem(
                        type = "U",
                        payloadString = "https://ai.studio/build",
                        payloadHex = "04:61:69:2E:73:74:75:64:69:6F:2F:62:75:69:6C:64",
                        tnf = "Well-Known (RTD)",
                        uriToOpen = "https://ai.studio/build"
                    ),
                    NdefRecordItem(
                        type = "T",
                        payloadString = "[fr] Balise Diagnostic Sans-Fil v2.0",
                        payloadHex = "02:66:72:42:61:6C:69:73:65:20:44:69:61:67:6E:6F:73:74:69:63",
                        tnf = "Well-Known (RTD)"
                    )
                ),
                memoryPages = listOf(
                    NfcMemoryPage(0, "04 E2 4B 8A", "....", "UID / Numéro de série"),
                    NfcMemoryPage(1, "9A 8C 63 80", "..c.", "UID / Numéro de série"),
                    NfcMemoryPage(2, "48 48 00 00", "HH..", "Lock Bytes (Protection)"),
                    NfcMemoryPage(3, "E1 10 3E 00", "..>.", "Capability Container (CC)"),
                    NfcMemoryPage(4, "03 23 D1 01", ".#..", "Début Données (TLV NDEF)"),
                    NfcMemoryPage(5, "1F 55 04 61", ".U.a", "Mémoire Utilisateur"),
                    NfcMemoryPage(6, "69 2E 73 74", "i.st", "Mémoire Utilisateur"),
                    NfcMemoryPage(7, "75 64 69 6F", "udio", "Mémoire Utilisateur")
                ),
                rawDumpHex = """
                    0000:  03 23 D1 01 1F 55 04 61  69 2E 73 74 75 64 69 6F  | .#...U.ai.studio
                    0010:  2F 62 75 69 6C 64 54 02  66 72 42 61 6C 69 73 65  | /buildT.frBalise
                    0020:  20 44 69 61 67 6E 6F 73  74 69 63 FE 00 00 00 00  |  Diagnostic....
                """.trimIndent(),
                primaryUri = "https://ai.studio/build"
            ),
            NfcTagData(
                uidHex = "3B:82:19:4C",
                uidDec = "998373708",
                technologies = listOf("NfcA", "IsoDep"),
                tagType = "ISO 14443-4 (IsoDep / SmartCard)",
                manufacturer = "Fabricant ISO 7816-6 (Préfixe 0x3B)",
                nfcForumType = "NFC Forum Type 4",
                memorySizeBytes = 8192,
                isWritable = false,
                atqa = "00:04",
                sak = "0x20",
                sakDecoded = "ISO/IEC 14443-4 (IsoDep / DESFire / Smart Card / EMV Bancaire)",
                historicalBytes = "80:4F:02:11:00",
                smartCardCategory = "Carte Bancaire Sans Contact (EMV / PPSE Détecté)",
                rawDumpHex = """
                    0000:  6F 28 84 0E 32 50 41 59  2E 53 59 53 2E 44 44 46  | o(..2PAY.SYS.DDF
                    0010:  30 31 A5 16 BF 0C 13 61  11 4F 07 A0 00 00 00 04  | 01.....a.O.....
                    0020:  10 10 87 01 01 9F 0A 08  00 01 02 03 04 05 06 07  | ................
                """.trimIndent()
            ),
            NfcTagData(
                uidHex = "A2:84:9F:3B",
                uidDec = "2726600507",
                technologies = listOf("NfcA", "MifareClassic"),
                tagType = "MIFARE Classic (16 secteurs / 64 blocs) (1024 octets)",
                manufacturer = "Fabricant ISO 7816-6 (Préfixe 0xA2)",
                nfcForumType = "Propriétaire NXP (MIFARE Classic)",
                memorySizeBytes = 1024,
                isWritable = true,
                atqa = "00:04",
                sak = "0x08",
                sakDecoded = "NXP MIFARE Classic 1K (ou MIFARE Plus 1K SL1)",
                rawDumpHex = """
                    0000:  A2 84 9F 3B 53 08 04 00  01 02 03 04 05 06 07 08  | ...;S...........
                    0010:  00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00  | ................
                    0020:  FF FF FF FF FF FF FF 07  80 69 FF FF FF FF FF FF  | .........i......
                """.trimIndent()
            ),
            NfcTagData(
                uidHex = "04:A1:33:B8:22:90:80",
                uidDec = "1302819283749201",
                technologies = listOf("NfcA", "IsoDep"),
                tagType = "ISO 14443-4 (IsoDep / Calypso)",
                manufacturer = "NXP Semiconductors (MIFARE, NTAG, ICODE)",
                nfcForumType = "NFC Forum Type 4",
                atqa = "00:44",
                sak = "0x20",
                sakDecoded = "ISO/IEC 14443-4 (IsoDep / DESFire / Smart Card / EMV Bancaire)",
                historicalBytes = "10:78:33:49:43:41",
                smartCardCategory = "Passe Transport Urbain (Calypso / Navigo)",
                rawDumpHex = """
                    0000:  6F 1E 84 08 31 54 49 43  2E 49 43 41 A5 12 80 02  | o...1TIC.ICA...
                    0010:  00 01 81 04 01 02 03 04  82 06 0A 0B 0C 0D 0E 0F  | ................
                """.trimIndent()
            )
        )
    }
}
