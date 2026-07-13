package com.example.pillreminder.util

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/** یک کلمه‌ی خونده‌شده به همراه مختصات دقیقش روی عکس و میزان اطمینان OCR (۰ تا ۱۰۰) */
data class OcrWord(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val confidence: Float = 0f
)

/**
 * موتور OCR کاملاً آفلاین (بدون نیاز به اینترنت) برای خواندن متن نسخه، با Tesseract4Android.
 *
 * روی سندهای دوزبانه (فارسی+انگلیسی توی یک جدول)، مدل ترکیبیِ «fas+eng» گاهی دقتش برای
 * هر دو زبان همزمان ضعیف می‌شه (چون مدل باید حدس بزنه هر کاراکتر مال کدوم زبانه). برای
 * همین، علاوه بر یک پاسِ ترکیبی (برای متن نمایشی/قابل‌ویرایش)، دو پاسِ جداگانه هم با
 * «فقط فارسی» و «فقط انگلیسی» اجرا می‌کنیم و نتیجه‌ی هرکدوم که برای هر ناحیه از عکس
 * اطمینان (confidence) بالاتری داشته باشه رو نگه می‌داریم — این لیست دقیق‌تر برای
 * بازسازی جدول (TablePrescriptionParser) استفاده می‌شه.
 *
 * نکته نصب: هر دو فایل fas.traineddata و eng.traineddata باید از قبل در مسیر
 * app/src/main/assets/tessdata/ قرار داده شده باشند.
 */
object PrescriptionOcrEngine {

    private const val COMBINED_LANGUAGES = "fas+eng"
    private val requiredFiles = listOf("fas.traineddata", "eng.traineddata")

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

    private fun collectWords(api: TessBaseAPI): List<OcrWord> {
        val words = mutableListOf<OcrWord>()
        val iterator = api.getResultIterator()
        if (iterator != null) {
            iterator.begin()
            do {
                val wordText = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                val rect = iterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                val conf = try {
                    iterator.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD)
                } catch (e: Exception) {
                    0f
                }
                if (!wordText.isNullOrBlank() && rect != null) {
                    words.add(OcrWord(wordText.trim(), rect.left, rect.top, rect.right, rect.bottom, conf))
                }
            } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))
            iterator.delete()
        }
        return words
    }

    /** یک پاسِ OCR با یک زبان مشخص اجرا می‌کند؛ اگه ناموفق بود null برمی‌گردونه */
    private fun runPass(context: Context, bitmap: Bitmap, languages: String): Pair<String, List<OcrWord>>? {
        val api = TessBaseAPI()
        return try {
            val ok = api.init(tessDataDir(context).absolutePath, languages)
            if (!ok) return null
            api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
            api.setVariable("preserve_interword_spaces", "1")
            api.setImage(bitmap)
            val text = api.getUTF8Text() ?: "" // باید قبل از ایتریتور صدا زده بشه
            val words = collectWords(api)
            text to words
        } catch (e: Exception) {
            null
        } finally {
            api.recycle()
        }
    }

    private fun iou(a: OcrWord, b: OcrWord): Float {
        val left = maxOf(a.left, b.left)
        val top = maxOf(a.top, b.top)
        val right = minOf(a.right, b.right)
        val bottom = minOf(a.bottom, b.bottom)
        if (right <= left || bottom <= top) return 0f
        val overlap = ((right - left) * (bottom - top)).toFloat()
        val areaA = ((a.right - a.left) * (a.bottom - a.top)).toFloat()
        val areaB = ((b.right - b.left) * (b.bottom - b.top)).toFloat()
        val union = areaA + areaB - overlap
        return if (union <= 0f) 0f else overlap / union
    }

    /** برای هر ناحیه از عکس، کلمه‌ای که موتورِ تک‌زبانه (فارسی یا انگلیسی) با اطمینان بیشتر خونده رو نگه می‌داره */
    private fun mergeByConfidence(fasWords: List<OcrWord>, engWords: List<OcrWord>): List<OcrWord> {
        val result = mutableListOf<OcrWord>()
        val usedEng = BooleanArray(engWords.size)
        for (fw in fasWords) {
            var bestIndex = -1
            var bestOverlap = 0.3f // حداقل همپوشانی برای اینکه «همون ناحیه» حساب بشه
            for ((i, ew) in engWords.withIndex()) {
                if (usedEng[i]) continue
                val overlap = iou(fw, ew)
                if (overlap > bestOverlap) {
                    bestOverlap = overlap
                    bestIndex = i
                }
            }
            if (bestIndex >= 0) {
                usedEng[bestIndex] = true
                val ew = engWords[bestIndex]
                result.add(if (ew.confidence > fw.confidence) ew else fw)
            } else {
                result.add(fw)
            }
        }
        engWords.forEachIndexed { i, ew -> if (!usedEng[i]) result.add(ew) }
        return result
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

        return@withContext try {
            // پاس اول: ترکیبی (fas+eng) — برای متنِ نمایشی/قابل‌ویرایش و پارسر خطیِ ساده
            val combined = runPass(context, processedBitmap, COMBINED_LANGUAGES)
                ?: return@withContext OcrResult.Error("راه‌اندازی OCR ناموفق بود.")
            val (text, combinedWords) = combined

            // پاس دوم و سوم: هر زبان جدا — فقط برای ساخت لیست دقیق‌تر جهت بازسازی جدول.
            // اگه هرکدوم به هر دلیلی ناموفق بود، نتیجه‌ی ترکیبی رو به‌عنوان جایگزین نگه می‌داریم.
            val fasOnly = runPass(context, processedBitmap, "fas")?.second
            val engOnly = runPass(context, processedBitmap, "eng")?.second
            val tableWords = if (fasOnly != null && engOnly != null) {
                mergeByConfidence(fasOnly, engOnly)
            } else {
                combinedWords
            }

            OcrResult.Success(text, tableWords)
        } catch (e: Exception) {
            OcrResult.Error("خطا در خواندن عکس: ${e.message}")
        }
    }
}
