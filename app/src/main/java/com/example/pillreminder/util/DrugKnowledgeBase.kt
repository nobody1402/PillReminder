package com.example.pillreminder.util

import com.example.pillreminder.data.FoodRelation

/**
 * پیشنهاد هوشمند بر اساس نام دارو: رابطه با غذا، فاصله زمانی ثابت (برای آنتی‌بیوتیک‌ها)،
 * و متن راهنما که به کاربر نشان داده می‌شود.
 * منبع قوانین: توصیه‌های عمومی منتشرشده توسط سازمان غذا و دارو و پزشکان (لووتیروکسین/آهن/آنتی‌بیوتیک‌ها)
 * — این صرفاً یک راهنمای عمومی است و جایگزین نظر پزشک/داروساز نیست.
 */
data class DrugRuleSuggestion(
    val foodRelation: FoodRelation?,
    val waitAfterMinutes: Int = 0,
    val fixedIntervalHours: Int? = null,
    val note: String,
    val englishName: String
)

/**
 * پایگاه دانش داخلیِ آفلاین برای شناخت داروهای پرکاربرد از روی نام فارسی/فینگلیش،
 * هم برای پیشنهاد خودکار قانون مصرف و هم برای تلفظ صحیح انگلیسیِ اسم دارو در TTS.
 */
object DrugKnowledgeBase {

    fun normalize(raw: String): String {
        return raw.trim()
            .replace("ي", "ی")
            .replace("ك", "ک")
            .replace("‌", " ") // نیم‌فاصله -> فاصله عادی
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    private val rules: List<Pair<List<String>, DrugRuleSuggestion>> = listOf(
        listOf("لووتیروکسین", "یوتیروکس", "التروکسین", "levothyroxine", "eltroxin", "synthroid", "euthyrox") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.BEFORE_FOOD,
                waitAfterMinutes = 45,
                note = "این دارو بهتر است با معده خالی و حداقل ۳۰ تا ۶۰ دقیقه قبل از صبحانه مصرف شود؛ از مصرف هم‌زمان با لبنیات، آهن، کلسیم و قهوه تا ۴ ساعت خودداری کنید.",
                englishName = "Levothyroxine"
            ),
        listOf("آهن", "فروسولفات", "فروس سولفات", "iron", "ferrous sulfate") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.BEFORE_FOOD,
                waitAfterMinutes = 60,
                note = "این دارو بهتر است با معده خالی مصرف شود تا بهتر جذب شود؛ حداقل ۲ ساعت با لبنیات و آنتی‌بیوتیک‌ها فاصله بگذارید.",
                englishName = "Ferrous Sulfate"
            ),
        listOf("آموکسی سیلین", "آموکسی‌سیلین", "amoxicillin") to
            DrugRuleSuggestion(
                foodRelation = null, fixedIntervalHours = 8,
                note = "آنتی‌بیوتیک‌ها باید سر ساعت ثابت (هر ۸ ساعت) و تا پایان دوره کامل مصرف شوند، حتی اگر حالتان بهتر شد.",
                englishName = "Amoxicillin"
            ),
        listOf("آزیترومایسین", "azithromycin", "زیترو") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.BEFORE_FOOD, waitAfterMinutes = 60, fixedIntervalHours = 24,
                note = "ترجیحاً با معده خالی و یک‌بار در روز، سر ساعت ثابت مصرف شود.",
                englishName = "Azithromycin"
            ),
        listOf("سیپروفلوکساسین", "سیپرو", "ciprofloxacin") to
            DrugRuleSuggestion(
                foodRelation = null, fixedIntervalHours = 12,
                note = "این آنتی‌بیوتیک باید هر ۱۲ ساعت سر وقت مصرف شود؛ حداقل ۲ ساعت با لبنیات و مکمل آهن/کلسیم فاصله بگذارید.",
                englishName = "Ciprofloxacin"
            ),
        listOf("داکسی سایکلین", "داکسی‌سایکلین", "doxycycline") to
            DrugRuleSuggestion(
                foodRelation = null, fixedIntervalHours = 12,
                note = "هر ۱۲ ساعت سر وقت مصرف شود؛ با لبنیات، آهن و کلسیم حداقل ۲ ساعت فاصله بگذارید.",
                englishName = "Doxycycline"
            ),
        listOf("سفالکسین", "cephalexin", "cefalexin") to
            DrugRuleSuggestion(
                foodRelation = null, fixedIntervalHours = 6,
                note = "هر ۶ ساعت سر وقت مصرف شود و دوره درمان کامل رعایت شود.",
                englishName = "Cephalexin"
            ),
        listOf("متفورمین", "گلوکوفاژ", "metformin") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.WITH_FOOD,
                note = "همراه یا بلافاصله بعد از غذا مصرف شود تا ناراحتی گوارشی کمتر شود.",
                englishName = "Metformin"
            ),
        listOf("ایبوپروفن", "بروفن", "ibuprofen") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.AFTER_FOOD,
                note = "برای پیشگیری از تحریک معده، بعد از غذا مصرف شود.",
                englishName = "Ibuprofen"
            ),
        listOf("مفنامیک اسید", "پونستان", "mefenamic", "ponstan") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.AFTER_FOOD,
                note = "بعد از غذا مصرف شود تا معده تحریک نشود.",
                englishName = "Mefenamic Acid"
            ),
        listOf("استامینوفن", "پاراستامول", "تایلنول", "acetaminophen", "paracetamol") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.NO_RELATION,
                note = "معمولاً فرقی با غذا ندارد؛ فاصله دوزها را حداقل ۴ ساعت نگه دارید.",
                englishName = "Acetaminophen"
            ),
        listOf("آسپرین", "aspirin") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.AFTER_FOOD,
                note = "برای کاهش تحریک معده، بعد از غذا مصرف شود.",
                englishName = "Aspirin"
            ),
        listOf("امپرازول", "لوزک", "omeprazole") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.BEFORE_FOOD, waitAfterMinutes = 30,
                note = "ترجیحاً صبح، ناشتا و حدود ۳۰ دقیقه قبل از صبحانه مصرف شود.",
                englishName = "Omeprazole"
            ),
        listOf("پنتوپرازول", "pantoprazole") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.BEFORE_FOOD, waitAfterMinutes = 30,
                note = "ترجیحاً صبح، ناشتا و حدود ۳۰ دقیقه قبل از صبحانه مصرف شود.",
                englishName = "Pantoprazole"
            ),
        listOf("آلندرونات", "فوزاماکس", "alendronate", "fosamax") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.BEFORE_FOOD, waitAfterMinutes = 30,
                note = "صبح ناشتا با یک لیوان آب ساده مصرف شود؛ تا ۳۰-۶۰ دقیقه بعد دراز نکشید و چیزی نخورید.",
                englishName = "Alendronate"
            ),
        listOf("کلسیم", "calcium") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.WITH_FOOD,
                note = "همراه غذا بهتر جذب می‌شود؛ با لووتیروکسین و برخی آنتی‌بیوتیک‌ها حداقل ۲ تا ۴ ساعت فاصله بگذارید.",
                englishName = "Calcium"
            ),
        listOf("ویتامین د", "ویتامین دی", "vitamin d") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.WITH_FOOD,
                note = "همراه غذای چرب بهتر جذب می‌شود.",
                englishName = "Vitamin D"
            ),
        listOf("وارفارین", "warfarin") to
            DrugRuleSuggestion(
                foodRelation = FoodRelation.NO_RELATION,
                note = "سر ساعت ثابت (معمولاً عصر) مصرف شود و زمان‌بندی آن را با پزشک/آزمایش INR هماهنگ کنید.",
                englishName = "Warfarin"
            )
    )

    /** بر اساس شباهت متنی، قانون شناخته‌شده برای این نام دارو را برمی‌گرداند (اگر وجود داشته باشد) */
    fun findRule(pillName: String): DrugRuleSuggestion? {
        val n = normalize(pillName)
        if (n.isBlank()) return null
        for ((keywords, rule) in rules) {
            if (keywords.any { n.contains(normalize(it)) }) return rule
        }
        return null
    }

    /** برای TTS: اگر اسم دارو در پایگاه دانش شناخته شده باشد، تلفظ صحیح انگلیسی آن را برمی‌گرداند */
    fun englishNameFor(pillName: String): String? = findRule(pillName)?.englishName
}
