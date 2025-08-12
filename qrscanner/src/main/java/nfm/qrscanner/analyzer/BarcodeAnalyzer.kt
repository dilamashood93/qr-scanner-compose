package nfm.qrscanner.analyzer

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

internal class BarcodeAnalyzer(
    formats: Set<Int>,
    private val onResult: (String, Int) -> Unit,
    private val onError: (Throwable) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(formats.first(), *formats.drop(1).toIntArray())
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || imageProxy.format == ImageFormat.PRIVATE) {
            imageProxy.close(); return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.let { b ->
                    val raw = b.rawValue.orEmpty()
                    if (raw.isNotEmpty()) onResult(raw, b.format)
                }
            }
            .addOnFailureListener { onError(it) }
            .addOnCompleteListener { imageProxy.close() }
    }
}
