package com.example.pillreminder.util

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class OcrWord(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

object PrescriptionOcrEngine {

    // ====== استفاده از انگلیسی + ساده ======
    private const val LANGUAGES = "eng"
    private val requiredFiles = listOf("eng.traineddata")

    sealed class OcrResult {
        data class Success(val text: String, val words: List<OcrWord> = emptyList()) : OcrResult()
        data class MissingLanguageData(val message: String) : OcrResult()
        data class Error(val message: String) : OcrResult()
    }

    private fun tessDataDir(context: Context): File =
        File(context.filesDir, "tesseract")

    private fun ensureLanguageDataCopied(context: Context): Boolean {
        val tessdataSubDir = File(tessDataDir(context), "tessdata")
        if (!tessdataSubDir.exists()) tessdataSubDir.mkdirs()

        var allOk = true
        for (fileName in requiredFiles) {
            val target = File(tessdataSubDir, fileName)
            if (target.exists() && target.length() > 0) continue
            val copied = try {
                context.assets.open("tessdata/$fileName").use { input ->
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
                true
            } catch (e: Exception) {
                false
            }
            if (!copied) allOk = false
        }
        return allOk
    }

    suspend fun recognize(context: Context, bitmap: Bitmap): OcrResult = withContext(Dispatchers.Default) {
        if (!ensureLanguageDataCopied(context)) {
            return@withContext OcrResult.MissingLanguageData(
                "فایل زبان OCR (eng.traineddata) پیدا نشد."
            )
        }
        
        // ====== بدون پیش‌پردازش ======
        val api = TessBaseAPI()
        return@withContext try {
            val ok = api.init(tessDataDir(context).absolutePath, LANGUAGES)
            if (!ok) {
                OcrResult.Error("راه‌اندازی OCR ناموفق بود.")
            } else {
                // ====== حالت SINGLE_BLOCK برای متن روان ======
                api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)
                api.setImage(bitmap)

                val text = api.getUTF8Text() ?: ""

                val words = mutableListOf<OcrWord>()
                val iterator = api.getResultIterator()
                if (iterator != null) {
                    iterator.begin()
                    do {
                        val wordText = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                        val rect = iterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                        if (!wordText.isNullOrBlank() && rect != null) {
                            words.add(OcrWord(wordText.trim(), rect.left, rect.top, rect.right, rect.bottom))
                        }
                    } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))
                    iterator.delete()
                }

                OcrResult.Success(text, words)
            }
        } catch (e: Exception) {
            OcrResult.Error("خطا در خواندن عکس: ${e.message}")
        } finally {
            api.recycle()
        }
    }
}
