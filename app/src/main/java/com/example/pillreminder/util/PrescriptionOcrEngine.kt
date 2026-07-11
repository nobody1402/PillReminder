package com.example.pillreminder.util

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** یک کلمه‌ی خونده‌شده به همراه مختصات دقیقش روی عکس — برای بازسازی جدول لازمه */
data class OcrWord(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

/**
 * موتور OCR کاملاً آفلاین (بدون نیاز به اینترنت) برای خواندن متن نسخه، با Tesseract4Android.
 *
 * از ترکیب زبان فارسی + انگلیسی («fas+eng») استفاده می‌کنیم چون نسخه‌ها معمولاً اسم
 * برند دارو رو با حروف انگلیسی دارن (مثل Apotel) وسط یک خط فارسی؛ اگر فقط مدل فارسی
 * بارگذاری بشه، این کلمات انگلیسی رو اشتباه به اعداد/حروف فارسی تبدیل می‌کنه.
 *
 * علاوه بر متن ساده، مختصات دقیق هر کلمه (bounding box) رو هم برمی‌گردونیم؛ چون فرمت‌های
 * جدولی (مثل سامانه نسخه الکترونیک) رو فقط از روی متن خام نمی‌شه بازسازی کرد — باید بر
 * اساس موقعیت هر کلمه، ردیف‌ها و ستون‌ها رو دوباره ساخت (TablePrescriptionParser همین کارو می‌کنه).
 *
 * نکته نصب: هر دو فایل fas.traineddata و eng.traineddata باید از قبل در مسیر
 * app/src/main/assets/tessdata/ قرار داده شده باشند (به دلیل حجم بالا، این فایل‌ها باید
 * دستی از مخزن رسمی tesseract-ocr/tessdata_fast دانلود و در پروژه کپی شوند).
 */
object PrescriptionOcrEngine {

    private const val LANGUAGES = "fas+eng"
    private val requiredFiles = listOf("fas.traineddata", "eng.traineddata")

    sealed class OcrResult {
        data class Success(val text: String, val words: List<OcrWord> = emptyList()) : OcrResult()
        data class MissingLanguageData(val message: String) : OcrResult()
        data class Error(val message: String) : OcrResult()
    }

    private fun tessDataDir(context: Context): File =
        File(context.filesDir, "tesseract")

    /** فایل‌های زبان رو یک‌بار از assets به مسیر قابل‌خواندن توسط Tesseract کپی می‌کند */
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

    /** عکس نسخه رو می‌گیره و متن + مختصات کلمات رو برمی‌گردونه؛ کاملاً روی خود گوشی، بدون اینترنت */
    suspend fun recognize(context: Context, bitmap: Bitmap): OcrResult = withContext(Dispatchers.Default) {
        if (!ensureLanguageDataCopied(context)) {
            return@withContext OcrResult.MissingLanguageData(
                "فایل‌های زبان OCR (fas.traineddata و eng.traineddata) پیدا نشدن. باید این دو فایل رو یک‌بار از " +
                    "مخزن رسمی tesseract-ocr/tessdata_fast دانلود کرده و در مسیر " +
                    "app/src/main/assets/tessdata/ قرار بدی تا خواندن نسخه درست کار کنه."
            )
        }
        val processedBitmap = try {
            ImagePreprocessor.prepareForOcr(bitmap)
        } catch (e: Exception) {
            bitmap
        }

        val api = TessBaseAPI()
        return@withContext try {
            val ok = api.init(tessDataDir(context).absolutePath, LANGUAGES)
            if (!ok) {
                OcrResult.Error("راه‌اندازی OCR ناموفق بود.")
            } else {
                // یک بلوک متنی یکپارچه فرض کن (نه چند ستون)؛ باعث می‌شه خط‌های کم‌رنگ یا
                // کنار هم رو کمتر جا بندازه نسبت به حالت خودکار پیش‌فرض
                api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)
                api.setVariable("preserve_interword_spaces", "1")
                api.setImage(processedBitmap)

                // getUTF8Text() باید قبل از گرفتن ایتریتور صدا زده بشه، وگرنه نتایج خالیه
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
