// در تابع runOcr، به جای استفاده از TablePrescriptionParser، از TableRegexParser استفاده کنید:

fun runOcr(bitmap: Bitmap) {
    PrescriptionScanState.isProcessing = true
    PrescriptionScanState.errorMessage = null
    PrescriptionScanState.parsedItems = emptyList()
    PrescriptionScanState.addedItemIndices = emptySet()
    scope.launch {
        when (val result = PrescriptionOcrEngine.recognize(context, bitmap)) {
            is PrescriptionOcrEngine.OcrResult.Success -> {
                PrescriptionScanState.ocrText = result.text
                PrescriptionScanState.ocrWords = result.words
                
                // ====== استفاده از Regex Parser جدید ======
                val regexItems = TableRegexParser.parse(result.text)
                if (regexItems.isNotEmpty()) {
                    PrescriptionScanState.parsedItems = regexItems
                } else {
                    // اگر Regex جواب نداد، از پارسر جدول استفاده کن
                    val tableResult = TablePrescriptionParser.parse(result.words)
                    PrescriptionScanState.parsedItems = tableResult ?: PrescriptionParser.parse(result.text)
                }
                // ==========================================
            }
            is PrescriptionOcrEngine.OcrResult.MissingLanguageData -> {
                PrescriptionScanState.errorMessage = result.message
            }
            is PrescriptionOcrEngine.OcrResult.Error -> {
                PrescriptionScanState.errorMessage = result.message
            }
        }
        PrescriptionScanState.isProcessing = false
    }
}
