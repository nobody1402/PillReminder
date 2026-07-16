package com.example.pillreminder.util

import com.example.pillreminder.data.FoodRelation
import java.time.LocalTime
import kotlin.math.abs

/**
 * پارسر اختصاصی برای فرمت جدولیِ «سامانه نسخه الکترونیک» (تامین اجتماعی و مشابه):
 * ستون‌های ردیف / عنوان دارو / تعداد / مقادیر مصرف / زمان مصرف / طریقه مصرف.
 *
 * چون این یک جدوله (نه متن خطی مثل فیش داروخانه)، از متن ساده‌ی OCR نمی‌شه رج‌ها رو
 * تشخیص داد — کلمه‌های ستون‌های مختلف قاطی می‌شن. برای همین از مختصات دقیق هر کلمه
 * (OcrWord) استفاده می‌کنیم: اول کلمه‌ها بر اساس ارتفاع، به ردیف دسته‌بندی می‌شن؛ بعد
 * توی هر ردیف، هر کلمه به نزدیک‌ترین ستون (بر اساس موقعیت افقی سرستون‌ها) نسبت داده می‌شه.
 *
 * توجه: این پیاده‌سازی بر پایه‌ی طراحی و منطقِ فرمت رسمی نوشته شده، ولی چون امکان اجرای
 * واقعیِ Tesseract روی گوشی در محیط توسعه در دسترس نبود، ممکنه با اولین تست‌های واقعی
 * نیاز به تنظیم دقیق‌تر (مثلاً آستانه‌های فاصله) داشته باشه.
 */
object TablePrescriptionParser {

    private val allHeaderKeywords = listOf("ردیف", "عنوان", "دارو", "تعداد", "مقادیر", "زمان", "طریقه", "مصرف")

    private fun norm(s: String): String {
        return s.trim()
            .lowercase()
            .replace("ي", "ی")
            .replace("ك", "ک")
            .replace("‌", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun centerX(w: OcrWord) = (w.left + w.right) / 2
    private fun centerY(w: OcrWord) = (w.top + w.bottom) / 2
    
    private fun clusterRows(words: List<OcrWord>): List<List<OcrWord>> {
        if (words.isEmpty()) return emptyList()
        val sorted = words.sortedBy { centerY(it) }
        val avgHeight = sorted.map { it.bottom - it.top }.average().takeIf { it > 0 } ?: 20.0
        val rows = mutableListOf<MutableList<OcrWord>>()
        for (w in sorted) {
            val cy = centerY(w)
            val currentRow = rows.lastOrNull()
            val fitsCurrentRow = currentRow != null &&
                abs(cy - currentRow.map { centerY(it) }.average()) < avgHeight * 0.7
            if (fitsCurrentRow) currentRow!!.add(w) else rows.add(mutableListOf(w))
        }
        return rows
    }

    private fun headerScore(row: List<OcrWord>): Int {
        val texts = row.map { norm(it.text) }
        return allHeaderKeywords.count { kw -> texts.any { it == norm(kw) } }
    }

    private fun findWordX(row: List<OcrWord>, keyword: String): Int? =
        row.firstOrNull { norm(it.text) == norm(keyword) }?.let { centerX(it) }

    /** آدرس افقیِ هر ستون رو از روی سرستون‌ها پیدا می‌کند؛ اگه کمتر از ۳ ستون شناسایی بشه، null برمی‌گردونه */
    private fun detectColumnAnchors(headerRow: List<OcrWord>): Map<String, Int>? {
        val radif = findWordX(headerRow, "ردیف")
        val onvan = findWordX(headerRow, "عنوان")
        val darou = findWordX(headerRow, "دارو")
        val tedad = findWordX(headerRow, "تعداد")
        val meghdar = findWordX(headerRow, "مقادیر")
        val zaman = findWordX(headerRow, "زمان")
        val tarighe = findWordX(headerRow, "طریقه")

        // کلمه‌ی "مصرف" توی سه سرستون تکرار می‌شه؛ هرکدوم رو به نزدیک‌ترین کلمه‌ی متمایزش می‌چسبونیم
        val masrafWords = headerRow.filter { norm(it.text) == norm("مصرف") }
        fun nearestMasrafTo(x: Int?): Int? {
            if (x == null || masrafWords.isEmpty()) return null
            return masrafWords.minByOrNull { abs(centerX(it) - x) }?.let { centerX(it) }
        }

        val onvanDarou = listOfNotNull(onvan, darou).takeIf { it.isNotEmpty() }?.average()?.toInt()
        val meghdarMasraf = nearestMasrafTo(meghdar)
        val zamanMasraf = nearestMasrafTo(zaman)
        val targhieMasraf = nearestMasrafTo(tarighe)
        val meghdarCenter = listOfNotNull(meghdar, meghdarMasraf).takeIf { it.isNotEmpty() }?.average()?.toInt()
        val zamanCenter = listOfNotNull(zaman, zamanMasraf).takeIf { it.isNotEmpty() }?.average()?.toInt()
        val targhieCenter = listOfNotNull(tarighe, targhieMasraf).takeIf { it.isNotEmpty() }?.average()?.toInt()

        val result = mutableMapOf<String, Int>()
        radif?.let { result["ردیف"] = it }
        onvanDarou?.let { result["عنوان دارو"] = it }
        tedad?.let { result["تعداد"] = it }
        meghdarCenter?.let { result["مقادیر مصرف"] = it }
        zamanCenter?.let { result["زمان مصرف"] = it }
        targhieCenter?.let { result["طریقه مصرف"] = it }

        return if (result.size >= 3) result else null
    }


    private fun displayName(rawEnglish: String): String {
        val rule = DrugKnowledgeBase.findRule(rawEnglish)
        val fa = DrugKnowledgeBase.persianNameFor(rawEnglish)
        return if (rule != null && fa != null && !rawEnglish.contains(fa, ignoreCase = true)) "$fa (${rule.englishName})" else rawEnglish
    }

    private fun treatmentDays(quantity: Int?, doseAmount: Double, times: List<LocalTime>): Int? {
        if (quantity == null || quantity <= 0 || doseAmount <= 0.0 || times.isEmpty()) return null
        val dailyUse = doseAmount * times.size
        return kotlin.math.ceil(quantity / dailyUse).toInt().coerceAtLeast(1)
    }

    /** آیا این مجموعه کلمات، شبیه فرمت جدولیِ سامانه نسخه الکترونیکه؟ (برای تصمیم "این پارسر رو امتحان کنم یا نه") */
    fun looksLikeTable(words: List<OcrWord>): Boolean {
        if (words.size < 10) return false
        val rows = clusterRows(words)
        return rows.any { headerScore(it) >= 3 }
    }

    /** اگه فرمت جدولی تشخیص داده بشه، لیست داروها رو برمی‌گردونه؛ وگرنه null (تا پارسر خطی جایگزینش بشه) */
    fun parse(words: List<OcrWord>): List<ParsedPrescriptionItem>? {
        val rows = clusterRows(words)
        if (rows.size < 2) return null

        val headerRowIndex = rows.indices.maxByOrNull { headerScore(rows[it]) } ?: return null
        if (headerScore(rows[headerRowIndex]) < 3) return null

        val anchors = detectColumnAnchors(rows[headerRowIndex]) ?: return null
        anchors["عنوان دارو"] ?: return null

        val items = mutableListOf<ParsedPrescriptionItem>()
        for (row in rows.drop(headerRowIndex + 1)) {
            if (row.size < 2) continue

            val buckets = mutableMapOf<String, MutableList<OcrWord>>()
            for (word in row) {
                val wx = centerX(word)
                val nearestColumn = anchors.entries.minByOrNull { abs(it.value - wx) }?.key ?: continue
                buckets.getOrPut(nearestColumn) { mutableListOf() }.add(word)
            }

            fun colText(key: String, leftToRight: Boolean): String =
                (buckets[key] ?: emptyList())
                    .sortedBy { if (leftToRight) it.left else -it.left }
                    .joinToString(" ") { it.text }

            // اسم دارو انگلیسیه (چپ‌به‌راست)؛ بقیه ستون‌ها فارسی‌ان (راست‌به‌چپ)
            val drugName = colText("عنوان دارو", leftToRight = true).trim()
            if (drugName.isBlank() || drugName.length < 2) continue

            val quantityText = colText("تعداد", leftToRight = true)
            val quantity = TimeParseUtils.normalizeDigits(quantityText).filter { it.isDigit() }.toIntOrNull()

            val doseText = colText("مقادیر مصرف", leftToRight = false)
            val timingText = colText("زمان مصرف", leftToRight = false)
            val methodText = colText("طریقه مصرف", leftToRight = false)

            val instruction = PrescriptionInstructionParser.parse(doseText, timingText, methodText)
            val foodRelation = instruction.foodRelation
            val times = instruction.times
            val needsManual = instruction.needsManualTiming
            val doseAmount = instruction.doseAmount
            val rule = DrugKnowledgeBase.findRule(drugName)
            val finalFoodRelation = if (foodRelation == FoodRelation.NO_RELATION) rule?.foodRelation ?: foodRelation else foodRelation
            val durationDays = instruction.durationDays ?: treatmentDays(quantity, doseAmount, times)

            val summaryParts = listOfNotNull(
                doseText.takeIf { it.isNotBlank() },
                timingText.takeIf { it.isNotBlank() },
                methodText.takeIf { it.isNotBlank() }
            )
            val note = if (needsManual) {
                "⚠️ نوبت‌بندی این دارو («${timingText.ifBlank { doseText }}») با آلارم روزانه قابل تنظیم خودکار نیست — لطفاً ساعت‌ها رو دستی مطابق نسخه تنظیم کن."
            } else {
                "طبق نسخه: ${summaryParts.joinToString("، ")}"
            }

            items.add(
                ParsedPrescriptionItem(
                    rawLine = summaryParts.joinToString(" | ").ifBlank { drugName },
                    name = displayName(drugName),
                    formHint = null,
                    quantity = quantity,
                    suggestedDoseAmount = doseAmount,
                    suggestedTreatmentDurationDays = durationDays,
                    suggestedInventoryCount = quantity?.toDouble(),
                    suggestedTimesOfDay = times,
                    recognizedRule = DrugRuleSuggestion(
                        foodRelation = finalFoodRelation,
                        waitAfterMinutes = rule?.waitAfterMinutes ?: 0,
                        fixedIntervalHours = instruction.fixedIntervalHours ?: rule?.fixedIntervalHours,
                        note = listOfNotNull(note, rule?.note).joinToString("\n"),
                        englishName = rule?.englishName ?: drugName
                    )
                )
            )
        }
        return items.ifEmpty { null }
    }
}
