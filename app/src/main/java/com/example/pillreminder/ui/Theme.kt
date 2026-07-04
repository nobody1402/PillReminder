package com.example.pillreminder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.pillreminder.util.ThemeMode

/** یک وضعیت سراسری ساده که با تغییرش، کل اپ (بدون نیاز به ری‌استارت) پوسته‌اش عوض می‌شود */
object AppThemeState {
    var mode = mutableStateOf(ThemeMode.SYSTEM)
}

// -----------------------------------------------------------------------------------
// پالت رنگی: زعفرانی/کهربایی (گرم و ایرانی) + سبزآبی تیره (آرام‌بخش، حس درمان/گیاهی)
// به‌جای پالت‌های کلیشه‌ای رایج، برای فضای «دارو و مراقبت خانگی» طراحی شده.
// -----------------------------------------------------------------------------------
private val Saffron = Color(0xFFE0A72E)       // رنگ اصلی — گرم، زنده، ایرانی
private val SaffronDark = Color(0xFFF2C25C)   // نسخه روشن‌تر برای حالت تاریک
private val DeepTeal = Color(0xFF1F6F63)      // رنگ دوم — آرام، حس گیاهی/درمانی
private val DeepTealLight = Color(0xFF7FD6C4) // نسخه روشن‌تر برای حالت تاریک
private val Coral = Color(0xFFDB6B57)         // رنگ سوم — برای هشدار ملایم/فوریت (نه قرمز خطر)
private val CoralLight = Color(0xFFFFB4A0)

private val CreamBackground = Color(0xFFFBF5EA)
private val CreamSurface = Color(0xFFFFFCF6)
private val CreamSurfaceVariant = Color(0xFFF1E5CE)

private val CharcoalBackground = Color(0xFF1B1712)
private val CharcoalSurface = Color(0xFF241F18)
private val CharcoalSurfaceVariant = Color(0xFF3A3226)

private val LightColors = lightColorScheme(
    primary = Saffron,
    onPrimary = Color(0xFF3A2600),
    primaryContainer = Color(0xFFFFDEA0),
    onPrimaryContainer = Color(0xFF291A00),
    secondary = DeepTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB7ECDD),
    onSecondaryContainer = Color(0xFF00201A),
    tertiary = Coral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBD1),
    onTertiaryContainer = Color(0xFF3B0900),
    background = CreamBackground,
    onBackground = Color(0xFF241F18),
    surface = CreamSurface,
    onSurface = Color(0xFF241F18),
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = Color(0xFF4E4738),
    outline = Color(0xFF9C8F76),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = SaffronDark,
    onPrimary = Color(0xFF422D00),
    primaryContainer = Color(0xFF5E4200),
    onPrimaryContainer = Color(0xFFFFDEA0),
    secondary = DeepTealLight,
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF0B5045),
    onSecondaryContainer = Color(0xFFB7ECDD),
    tertiary = CoralLight,
    onTertiary = Color(0xFF5C1400),
    tertiaryContainer = Color(0xFF7D2B14),
    onTertiaryContainer = Color(0xFFFFDBD1),
    background = CharcoalBackground,
    onBackground = Color(0xFFEAE1D3),
    surface = CharcoalSurface,
    onSurface = Color(0xFFEAE1D3),
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = Color(0xFFD1C4AA),
    outline = Color(0xFF9A8E76),
    error = Color(0xFFFFB4AB)
)

// شکل‌های گردتر و نرم‌تر در همه‌ی کامپوننت‌ها (دکمه، کارت، فیلد ورودی، FAB و ...) —
// این باعث می‌شه کل اپ بدون نیاز به دست‌کاری تک‌تک صفحه‌ها، لبه‌های برجسته و منحنی داشته باشه.
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

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
        content = content
    )
}
