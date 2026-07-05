package com.example.pillreminder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillreminder.util.ThemeMode

/** یک وضعیت سراسری ساده که با تغییرش، کل اپ (بدون نیاز به ری‌استارت) پوسته‌اش عوض می‌شود */
object AppThemeState {
    var mode = mutableStateOf(ThemeMode.SYSTEM)
}

// -----------------------------------------------------------------------------------
// سیستم طراحی «گرمای مراقبت و وضوح» (Warm Care & Clarity) — بر اساس طرحی که با Stitch
// روی همین پالت زعفرانی/سبزآبی/مرجانی توسعه داده شد. رنگ‌های دقیق و اندازه‌ی گوشه‌ها
// عیناً از اون طرح گرفته شده تا خروجی با چیزی که تایید شده یکی باشه.
// -----------------------------------------------------------------------------------

// زعفرانی — اکشن‌های اصلی و برجسته
private val Saffron = Color(0xFFE0A72E)
private val OnSaffron = Color(0xFF3A2600)
private val SaffronTint = Color(0xFFFFDEA7) // برای کارت‌های «پیشنهاد هوشمند» — ملایم، بدون حس هشدار
private val OnSaffronTint = Color(0xFF5A3F00)
private val SaffronDark = Color(0xFFF8BD43)
private val OnSaffronDark = Color(0xFF5E4200)

// سبزآبی گیاهی — وضعیت «مصرف‌شده» / موفقیت
private val Herbal = Color(0xFF176A5E)
private val HerbalTint = Color(0xFFA3EEDF)
private val OnHerbalTint = Color(0xFF1E6E62)
private val HerbalDark = Color(0xFF8AD4C6)
private val OnHerbalDark = Color(0xFF00201B)

// مرجانی — هشدار ملایم (تاخیر)، نه قرمزِ خطر
private val Coral = Color(0xFFA03F2E)
private val CoralTint = Color(0xFFFF937F)
private val OnCoralTint = Color(0xFF7C2517)
private val CoralDark = Color(0xFFFFB4A6)
private val OnCoralDark = Color(0xFF81281A)

// بوم کِرم‌رنگ — پس‌زمینه‌ی گرم به‌جای سفید سرد بیمارستانی
private val CreamBackground = Color(0xFFFFF9EE)
private val CreamSurfaceContainer = Color(0xFFF3EDE2)
private val CreamSurfaceVariant = Color(0xFFE8E2D7)
private val Charcoal = Color(0xFF1D1C15)
private val CharcoalVariant = Color(0xFF504535)
private val OutlineGray = Color(0xFF9C8F76)

private val CharcoalBackground = Color(0xFF17150F)
private val CharcoalSurface = Color(0xFF201C14)
private val CharcoalSurfaceVariant = Color(0xFF362E20)
private val CreamOnDark = Color(0xFFEFE7D8)

private val LightColors = lightColorScheme(
    primary = Saffron,
    onPrimary = OnSaffron,
    primaryContainer = SaffronTint,
    onPrimaryContainer = OnSaffronTint,
    secondary = Herbal,
    onSecondary = Color.White,
    secondaryContainer = HerbalTint,
    onSecondaryContainer = OnHerbalTint,
    tertiary = Coral,
    onTertiary = Color.White,
    tertiaryContainer = CoralTint,
    onTertiaryContainer = OnCoralTint,
    background = CreamBackground,
    onBackground = Charcoal,
    surface = CreamBackground,
    onSurface = Charcoal,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = CharcoalVariant,
    outline = OutlineGray,
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6)
)

private val DarkColors = darkColorScheme(
    primary = SaffronDark,
    onPrimary = OnSaffronDark,
    primaryContainer = Color(0xFF5E4200),
    onPrimaryContainer = SaffronTint,
    secondary = HerbalDark,
    onSecondary = OnHerbalDark,
    secondaryContainer = Color(0xFF0B5045),
    onSecondaryContainer = HerbalTint,
    tertiary = CoralDark,
    onTertiary = OnCoralDark,
    tertiaryContainer = Color(0xFF7D2B14),
    onTertiaryContainer = CoralTint,
    background = CharcoalBackground,
    onBackground = CreamOnDark,
    surface = CharcoalSurface,
    onSurface = CreamOnDark,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = Color(0xFFD1C4AA),
    outline = OutlineGray,
    error = Color(0xFFFFB4AB)
)

// گوشه‌های گرد طبق مشخصات دقیق طرح: فیلد ورودی/چیپ ۱۴dp، کارت‌های استاندارد ۲۰dp،
// دکمه‌های اصلی به‌صورت کپسولی (M3 به‌صورت پیش‌فرض این کار رو برای دکمه‌های filled انجام می‌ده)
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// مقیاس تایپوگرافی طرح (فونت پیش‌فرض سیستم؛ اگه بخوای دقیقاً «Work Sans» باشه، باید فایل
// فونت رو دستی به پروژه اضافه کنی، مثل فایل‌های OCR قبلی — همون رویه)
private val AppTypography = Typography().let { base ->
    base.copy(
        headlineLarge = base.headlineLarge.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold),
        bodyLarge = base.bodyLarge.copy(fontSize = 18.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
        bodyMedium = base.bodyMedium.copy(fontSize = 16.sp, lineHeight = 24.sp),
        labelLarge = base.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp)
    )
}

/** اندازه‌های بزرگ‌تر برای حالت سالمندان، مطابق طرح (استفاده در جاهایی که فونت شرطی دارن) */
object ElderlyTextSizes {
    val headline = 28.sp
    val bodyLarge = 26.sp
    val bodyBase = 24.sp
    val label = 20.sp
}

@Composable
fun PillReminderTheme(content: @Composable () -> Unit) {
    val mode = AppThemeState.mode.value
    val useDark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) DarkColors else LightColors,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
