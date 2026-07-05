package com.example.pillreminder.util

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * موتور OCR کاملاً آفلاین (بدون نیاز به اینترنت) برای خواندن متن فارسیِ عکسِ نسخه،
 * با استفاده از کتابخانه Tesseract4Android.
 *
 * نکته مهم نصب: فایل زبان فارسی (fas.traineddata) باید از قبل در مسیر
 * app/src/main/assets/tessdata/fas.traineddata قرار داده شده باشد (به دلیل حجم بالا،
 * این فایل باید دستی از مخزن رسمی tesseract-ocr/tessdata دانلود و در پروژه کپی شود).
 * اگر این فایل موجود نباشد، تابع زیر خطای مشخصی برمی‌گرداند تا کاربر بداند مشکل کجاست.
 */
object PrescriptionOcrEngine {

    private const val LANGUAGE = "fas" // کد زبان فارسی در Tesseract

    sealed class OcrResult {
        data class Success(val text: String) : OcrResult()
        data class MissingLanguageData(val message: String) : OcrResult()
        data class Error(val message: String) : OcrResult()
    }

    private fun tessDataDir(context: Context): File =
        File(context.filesDir, "tesseract")

    /** فایل زبان را یک‌بار از assets به مسیر قابل‌خواندن توسط Tesseract کپی می‌کند */
    private fun ensureLanguageDataCopied(context: Context): Boolean {
        val dir = tessDataDir(context)
        val tessdataSubDir = File(dir, "tessdata")
        if (!tessdataSubDir.exists()) tessdataSubDir.mkdirs()
        val target = File(tessdataSubDir, "$LANGUAGE.traineddata")
        if (target.exists() && target.length() > 0) return true

        return try {
            context.assets.open("tessdata/$LANGUAGE.traineddata").use { input ->
                FileOutputStream(target).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /** عکس نسخه را می‌گیرد و متن فارسی استخراج‌شده را برمی‌گرداند؛ کاملاً روی خود گوشی، بدون اینترنت */
    suspend fun recognize(context: Context, bitmap: Bitmap): OcrResult = withContext(Dispatchers.Default) {
        if (!ensureLanguageDataCopied(context)) {
            return@withContext OcrResult.MissingLanguageData(
                "فایل زبان فارسی OCR (fas.traineddata) پیدا نشد. باید این فایل را یک‌بار از " +
                    "مخزن رسمی tesseract-ocr/tessdata دانلود کرده و در مسیر " +
                    "app/src/main/assets/tessdata/fas.traineddata قرار دهید تا خواندن نسخه کار کند."
            )
        }
        val api = TessBaseAPI()
        return@withContext try {
            val ok = api.init(tessDataDir(context).absolutePath, LANGUAGE)
            if (!ok) {
                OcrResult.Error("راه‌اندازی OCR ناموفق بود.")
            } else {
                api.setImage(bitmap)
                val text = api.getUTF8Text() ?: ""
                OcrResult.Success(text)
            }
        } catch (e: Exception) {
            OcrResult.Error("خطا در خواندن عکس: ${e.message}")
        } finally {
            api.recycle()
        }
    }
}
