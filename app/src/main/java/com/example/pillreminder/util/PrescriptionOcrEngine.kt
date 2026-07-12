package com.example.pillreminder.util

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class OcrWord(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

object PrescriptionOcrEngine {

    sealed class OcrResult {
        data class Success(val text: String, val words: List<OcrWord> = emptyList()) : OcrResult()
        data class Error(val message: String) : OcrResult()
    }

    suspend fun recognize(context: Context, bitmap: Bitmap): OcrResult {
        return suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val resultText = visionText.text
                        val words = visionText.textBlocks.flatMap { block ->
                            block.lines.flatMap { line ->
                                line.elements.map { element ->
                                    val boundingBox = element.boundingBox
                                    if (boundingBox != null) {
                                        OcrWord(
                                            text = element.text,
                                            left = boundingBox.left,
                                            top = boundingBox.top,
                                            right = boundingBox.right,
                                            bottom = boundingBox.bottom
                                        )
                                    } else {
                                        null
                                    }
                                }.filterNotNull()
                            }
                        }
                        continuation.resume(OcrResult.Success(resultText, words))
                        recognizer.close()
                    }
                    .addOnFailureListener { e ->
                        continuation.resume(OcrResult.Error("خطا: ${e.message}"))
                        recognizer.close()
                    }
            } catch (e: Exception) {
                continuation.resume(OcrResult.Error("خطا: ${e.message}"))
            }
        }
    }
}
