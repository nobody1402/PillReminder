package com.example.pillreminder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.example.pillreminder.util.ThemeMode

/** یک وضعیت سراسری ساده که با تغییرش، کل اپ (بدون نیاز به ری‌استارت) پوسته‌اش عوض می‌شود */
object AppThemeState {
    var mode = mutableStateOf(ThemeMode.SYSTEM)
}

private val Seed = Color(0xFF2E7D5B) // سبز ملایم و آرام، مناسب فضای سلامت/دارو

private val LightColors = lightColorScheme(
    primary = Seed,
    onPrimary = Color.White,
    secondary = Color(0xFF4C6B5A),
    background = Color(0xFFF7FAF8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE4EFE8),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8ED2AE),
    onPrimary = Color(0xFF00391F),
    secondary = Color(0xFFB2CCBB),
    background = Color(0xFF10140F),
    surface = Color(0xFF181D17),
    surfaceVariant = Color(0xFF2C332A),
    error = Color(0xFFFFB4AB)
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
        content = content
    )
}
