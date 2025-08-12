package nfm.qrscanner

import com.google.mlkit.vision.barcode.common.Barcode

enum class ScanMode { SINGLE, CONTINUOUS }

data class QrScannerConfig(
    val formats: Set<Int> = setOf(
        Barcode.FORMAT_QR_CODE,
        Barcode.FORMAT_CODE_128,
        Barcode.FORMAT_EAN_13
    ),
    val enableTorchToggle: Boolean = true,
    val useBackCamera: Boolean = true,

    // Multi-scan controls
    val scanMode: ScanMode = ScanMode.SINGLE, // SINGLE (default) or CONTINUOUS
    val uniqueOnly: Boolean = true,           // only emit new codes
    val debounceMillis: Long = 800L,          // ignore repeats inside this window
    val maxResultsPerSession: Int? = null     // stop emitting after N (CONTINUOUS only)
)
