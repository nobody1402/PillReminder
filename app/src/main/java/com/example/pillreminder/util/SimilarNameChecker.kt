package com.example.pillreminder.util

/**
 * اگه کاربر یه اسم دارو رو با یه غلط تایپی کوچیک بنویسه (مثلاً «استامینوفون» به‌جای
 * «استامینوفن»)، به‌جای این‌که یه ردیف جدید و تکراری بسازه، بهش هشدار می‌ده که شاید
 * منظورش همون داروی قبلاً ثبت‌شده بوده.
 */
object SimilarNameChecker {

    private fun levenshtein(a: String, b: String): Int {
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            for (j in 1..b.length) {
                dp[i][j] = if (a[i - 1] == b[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[a.length][b.length]
    }

    /** اگه اسم مشابه (ولی نه دقیقاً یکسان) توی داروهای موجود پیدا شد، همون اسم رو برمی‌گردونه */
    fun findSimilar(name: String, existingNames: List<String>): String? {
        val n = DrugKnowledgeBase.normalize(name)
        if (n.length < 3) return null
        for (existing in existingNames) {
            val e = DrugKnowledgeBase.normalize(existing)
            if (e.isBlank() || e == n) continue
            val threshold = if (n.length <= 5) 1 else 2
            if (levenshtein(n, e) in 1..threshold) return existing
        }
        return null
    }
}
