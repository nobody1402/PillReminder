// PrescriptionOcrEngine.kt
package com.example.pillreminder.util

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object PrescriptionOcrEngine {
    
    sealed class OcrResult {
        data class Success(val text: String) : OcrResult()
        data class Error(val message: String) : OcrResult()
        data class MissingLanguageData(val message: String) : OcrResult()
    }

    suspend fun recognize(context: Context, bitmap: Bitmap): OcrResult {
        return suspendCancellableCoroutine { continuation ->
            try {
                // تنظیمات دقیق‌تر برای تشخیص متن انگلیسی
                val options = TextRecognizerOptions.Builder()
                    .build()
                
                val recognizer = TextRecognition.getClient(options)
                val image = InputImage.fromBitmap(bitmap, 0)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val resultText = visionText.text
                        if (resultText.isNotBlank()) {
                            continuation.resume(OcrResult.Success(resultText))
                        } else {
                            continuation.resume(OcrResult.Error("متن تشخیص داده نشد. لطفاً عکس را واضح‌تر بگیرید."))
                        }
                        recognizer.close()
                    }
                    .addOnFailureListener { e ->
                        continuation.resume(OcrResult.Error("خطا در تشخیص متن: ${e.message}"))
                        recognizer.close()
                    }
            } catch (e: Exception) {
                continuation.resume(OcrResult.Error("خطا: ${e.message}"))
            }
        }
    }
}
