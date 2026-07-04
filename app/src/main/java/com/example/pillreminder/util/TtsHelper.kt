package com.example.pillreminder.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * موتور TTS اندروید برای زبان فارسی روی همه گوشی‌ها نصب نیست؛ اگر موجود نبود، به‌آرامی
 * هیچ کاری نمی‌کند (Notification همچنان نمایش داده می‌شود).
 *
 * نکته مهم: کاربر ایرانی اسم دارو رو با حروف فارسی تایپ می‌کند (مثلا «استامینوفن»)، اما
 * موتور TTS فارسی نمی‌تواند تلفظ انگلیسیِ درستِ این اسم‌ها رو دربیاره. به همین دلیل، اگر
 * اسم دارو توی DrugKnowledgeBase شناخته‌شده باشه، فقط همون تکه از جمله با صدای انگلیسی
 * (Locale.US) خونده می‌شه و بقیه‌ی جمله («وقت داروی ... است») همچنان فارسیه؛ در نمایش
 * متنی (نوتیفیکیشن) همیشه اسم فارسی/همونی که کاربر وارد کرده نشون داده می‌شه.
 */
object TtsHelper {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val pendingActions = mutableListOf<() -> Unit>()

    private fun ensureInit(context: Context, onReady: () -> Unit) {
        if (ready) {
            onReady()
            return
        }
        pendingActions.add(onReady)
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                ready = status == TextToSpeech.SUCCESS
                if (ready) {
                    val actions = pendingActions.toList()
                    pendingActions.clear()
                    actions.forEach { it() }
                }
            }
        }
    }

    private fun speakPart(engine: TextToSpeech, text: String, locale: Locale, isFirst: Boolean) {
        val result = engine.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            // این زبان روی گوشی نصب نیست؛ به جای کرش کردن، با زبان پیش‌فرض دستگاه بخون
            engine.language = Locale.getDefault()
        }
        val queueMode = if (isFirst) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        engine.speak(text, queueMode, null, "med_reminder_" + text.hashCode())
    }

    /**
     * پیام یادآوری زمان مصرف دارو را می‌خواند. اگر اسم دارو در پایگاه دانش شناخته شده
     * باشد، خودِ اسم با تلفظ صحیح انگلیسی خوانده می‌شود؛ در غیر این صورت کل جمله فارسی
     * خوانده می‌شود (رفتار قدیمی، بدون تغییر).
     */
    fun speakReminder(context: Context, pillName: String) {
        val englishName = DrugKnowledgeBase.englishNameFor(pillName)
        ensureInit(context) {
            val engine = tts ?: return@ensureInit
            if (englishName != null) {
                speakPart(engine, "وقت داروی", Locale("fa", "IR"), isFirst = true)
                speakPart(engine, englishName, Locale.US, isFirst = false)
                speakPart(engine, "است", Locale("fa", "IR"), isFirst = false)
            } else {
                speakPart(engine, "وقت داروی $pillName است", Locale("fa", "IR"), isFirst = true)
            }
        }
    }

    /** برای پیام‌های آزاد و ساده (سازگار با فراخوانی‌های قدیمی) */
    fun speak(context: Context, text: String) {
        ensureInit(context) {
            tts?.let { speakPart(it, text, Locale("fa", "IR"), isFirst = true) }
        }
    }
}
