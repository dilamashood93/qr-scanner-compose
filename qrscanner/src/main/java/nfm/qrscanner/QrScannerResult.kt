package nfm.qrscanner

sealed interface QrScannerResult {
    data class Success(val rawValue: String, val format: Int) : QrScannerResult
    data class Error(val throwable: Throwable) : QrScannerResult
    data object Canceled : QrScannerResult
}
