package com.example.pillreminder.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/**
 * پیش‌پردازش ساده‌ی عکس قبل از OCR تا دقت خواندن بالاتر بره:
 * ۱) تبدیل به سیاه‌وسفید (رنگ‌ها و انعکاس نور رو کمتر گمراه‌کننده می‌کنه)
 * ۲) افزایش کنتراست (خط‌های کم‌رنگ رو واضح‌تر می‌کنه)
 * ۳) بزرگ‌نمایی عکس‌های کوچک (Tesseract روی متن ریز ضعیف عمل می‌کنه)
 *
 * این یک پردازش سبک و global هست، نه پیشرفته (مثل adaptive threshold)؛ در نور خیلی
 * نامنظم ممکنه هنوز مشکل داشته باشه، ولی برای اغلب عکس‌های گوشی کافیه.
 */
object ImagePreprocessor {

    private const val MIN_WIDTH = 1600

    fun prepareForOcr(source: Bitmap): Bitmap {
        val upscaled = ensureMinWidth(source, MIN_WIDTH)
        return grayscaleWithContrast(upscaled)
    }

    private fun ensureMinWidth(bitmap: Bitmap, minWidth: Int): Bitmap {
        if (bitmap.width >= minWidth) return bitmap
        val scale = minWidth.toFloat() / bitmap.width
        val newWidth = minWidth
        val newHeight = (bitmap.height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun grayscaleWithContrast(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // ماتریس اشباع صفر = خاکستری؛ بعد یک کنتراست ساده اعمال می‌کنیم
        val contrast = 1.6f
        val translate = (-0.5f * contrast + 0.5f) * 255f

        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        colorMatrix.postConcat(contrastMatrix)

        val paint = Paint().apply { colorFilter = ColorMatrixColorFilter(colorMatrix) }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
}
