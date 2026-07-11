package com.example.pillreminder.util

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * قبلاً وضعیت صفحه‌ی اسکن نسخه (عکس/متن OCR/لیست داروهای تشخیص‌داده‌شده) فقط با
 * `remember` نگه داشته می‌شد؛ چون رفتن به صفحه‌ی «افزودن دارو» و برگشتن باعث از بین رفتن
 * کامپوزیشن اون صفحه می‌شد، کل لیست پاک می‌شد و کاربر مجبور بود دوباره عکس بگیره.
 * با نگه‌داشتن این وضعیت توی یک singleton (نه در حافظه‌ی خودِ Composable)، وقتی کاربر
 * یک دارو رو ذخیره می‌کنه و به صفحه‌ی اسکن برمی‌گرده، بقیه‌ی داروهای تشخیص‌داده‌شده
 * هنوز همون‌جا هستن.
 */
object PrescriptionScanState {
    var imageBitmap by mutableStateOf<Bitmap?>(null)
    var ocrText by mutableStateOf("")
    var ocrWords by mutableStateOf<List<OcrWord>>(emptyList())
    var isProcessing by mutableStateOf(false)
    var errorMessage by mutableStateOf<String?>(null)
    var parsedItems by mutableStateOf<List<ParsedPrescriptionItem>>(emptyList())
    var addedItemIndices by mutableStateOf<Set<Int>>(emptySet())

    fun reset() {
        imageBitmap = null
        ocrText = ""
        ocrWords = emptyList()
        isProcessing = false
        errorMessage = null
        parsedItems = emptyList()
        addedItemIndices = emptySet()
    }
}
