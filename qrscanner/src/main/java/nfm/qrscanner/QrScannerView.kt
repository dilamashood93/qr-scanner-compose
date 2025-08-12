package nfm.qrscanner

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.TorchState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import nfm.qrscanner.analyzer.BarcodeAnalyzer
import nfm.qrscanner.ui.ScanOverlay

@Composable
fun QrScannerView(
    controller: QrScannerController,
    modifier: Modifier = Modifier,
    config: QrScannerConfig = QrScannerConfig(),
    onResult: (QrScannerResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> permissionGranted = granted }

    LaunchedEffect(Unit) {
        if (!permissionGranted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!permissionGranted) {
        Text("Camera permission is required.")
        return
    }

    var singleFired by remember { mutableStateOf(false) }
    LaunchedEffect(controller.sessionToken) { singleFired = false }

    fun tryEmit(raw: String, format: Int) {
        val now = System.currentTimeMillis()
        if (config.scanMode == ScanMode.SINGLE) {
            if (!singleFired) {
                singleFired = true
                onResult(QrScannerResult.Success(raw, format))
            }
            return
        }
        config.maxResultsPerSession?.let { if (controller.sessionEmitted >= it) return }
        val last = controller.lastEmits[raw]
        val shouldSkip = if (config.uniqueOnly) {
            last != null && (now - last) < config.debounceMillis
        } else {
            last != null && (now - last) < (config.debounceMillis / 2)
        }
        if (shouldSkip) return
        controller.lastEmits[raw] = now
        controller.sessionEmitted += 1
        onResult(QrScannerResult.Success(raw, format))
    }

    // Compose state so UI reacts when camera binds
    var boundCamera by remember { mutableStateOf<Camera?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }

                    val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
                    val cameraSelector =
                        if (config.useBackCamera) CameraSelector.DEFAULT_BACK_CAMERA
                        else CameraSelector.DEFAULT_FRONT_CAMERA

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build().apply {
                            setAnalyzer(
                                ContextCompat.getMainExecutor(ctx),
                                BarcodeAnalyzer(
                                    formats = config.formats,
                                    onResult = { raw, format -> tryEmit(raw, format) },
                                    onError = { t -> onResult(QrScannerResult.Error(t)) }
                                )
                            )
                        }

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, analysis
                    )

                    controller.cameraRef = camera
                    boundCamera = camera // trigger recomposition

                    previewView
                }
            )

            ScanOverlay(Modifier.fillMaxSize())
        }

        // Observe torch state from the bound camera
        val torchState = boundCamera
            ?.cameraInfo
            ?.torchState
            ?.observeAsState(TorchState.OFF)

        val hasFlash = boundCamera?.cameraInfo?.hasFlashUnit() == true
        val torchOn = torchState?.value == TorchState.ON

        // Always show the row if enabled; disable button when no flash
        if (config.enableTorchToggle) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Point your camera at codes")
                Button(
                    enabled = hasFlash,
                    onClick = { boundCamera?.cameraControl?.enableTorch(!torchOn) }
                ) {
                    Text(
                        when {
                            !hasFlash -> "No Flash"
                            torchOn   -> "Torch Off"
                            else      -> "Torch On"
                        }
                    )
                }
            }
        }
    }
}
