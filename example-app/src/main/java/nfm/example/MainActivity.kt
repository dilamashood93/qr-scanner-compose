package nfm.example

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import nfm.qrscanner.QrScannerResult
import nfm.qrscanner.QrScannerConfig
import nfm.qrscanner.QrScannerView
import nfm.qrscanner.ScanMode
import nfm.qrscanner.rememberQrScannerController
import androidx.compose.material3.CenterAlignedTopAppBar
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {

    private fun haptic() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(30)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val ctx = LocalContext.current
            val controller = rememberQrScannerController()
            var scans by remember { mutableStateOf(listOf<String>()) }
            val scroll = rememberScrollState()
            var showHistory by remember { mutableStateOf(false) }

            fun openIfUrl(raw: String) {
                var url = raw.trim()
                if (!android.util.Patterns.WEB_URL.matcher(url).matches()) return
                if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)
                ctx.startActivity(intent)
            }

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("NFM QR Demo") },
                        actions = {
                            TextButton(onClick = { showHistory = !showHistory }) {
                                Text(if (showHistory) "Hide History" else "Show History")
                            }
                            TextButton(onClick = {
                                controller.resetSession()
                                scans = emptyList()
                                Toast.makeText(ctx, "New session", Toast.LENGTH_SHORT).show()
                            }) { Text("New Session") }
                        }
                    )
                }
            ) { padding ->
                Column(
                    Modifier
                        .padding(padding)
                        .navigationBarsPadding()
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f) // 👈 Let camera fill space
                    ) {
                        QrScannerView(
                            controller = controller,
                            config = QrScannerConfig(
                                scanMode = ScanMode.CONTINUOUS,
                                uniqueOnly = true,
                                debounceMillis = 800,
                                maxResultsPerSession = 50
                            ),
                            onResult = { res ->
                                when (res) {
                                    is QrScannerResult.Success -> {
                                        haptic()
                                        Toast.makeText(ctx, "Scanned: ${res.rawValue}", Toast.LENGTH_SHORT).show()
                                        scans = (listOf(res.rawValue) + scans).distinct().take(5)
                                        openIfUrl(res.rawValue)
                                    }
                                    is QrScannerResult.Error ->
                                        Toast.makeText(ctx, "Error: ${res.throwable.message}", Toast.LENGTH_SHORT).show()
                                    QrScannerResult.Canceled -> Unit
                                }
                            }
                        )
                    }

                    // Show history only if enabled & not empty
                    if (showHistory && scans.isNotEmpty()) {
                        Divider()
                        Text("Last scans:", style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                        scans.forEach {
                            Text("• $it", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }

        }
    }
}
