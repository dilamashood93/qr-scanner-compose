package nfm.qrscanner

import androidx.camera.core.Camera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

class QrScannerController internal constructor(
    internal var cameraRef: Camera? = null
) {
    internal var sessionEmitted = 0
    internal val lastEmits = LinkedHashMap<String, Long>() // code -> timestamp

    /** Token that bumps when you call resetSession() so QrScannerView can react */
    var sessionToken: Int = 0
        private set

    fun toggleTorch(enabled: Boolean) {
        cameraRef?.cameraControl?.enableTorch(enabled)
    }

    val isTorchOn: Boolean
        get() = cameraRef?.cameraInfo?.torchState?.value == androidx.camera.core.TorchState.ON

    /** Clears multi-scan memory (dedupe/debounce counters). */
    fun resetSession() {
        sessionEmitted = 0
        lastEmits.clear()
        sessionToken++     // triggers LaunchedEffect(controller.sessionToken)
    }
}

/** Compose-friendly helper so callers can hold a controller across recompositions */
@Composable
fun rememberQrScannerController(): QrScannerController = remember { QrScannerController() }
