package com.example.pillreminder.util

import android.content.Context

enum class ThemeMode { LIGHT, DARK, SYSTEM }

object SettingsStore {
    private const val PREFS = "pill_reminder_settings"
    private const val KEY_ELDERLY_MODE = "elderly_mode"
    private const val KEY_THEME_MODE = "theme_mode"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isElderlyMode(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ELDERLY_MODE, false)

    fun setElderlyMode(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ELDERLY_MODE, enabled).apply()
    }

    fun getThemeMode(context: Context): ThemeMode {
        val name = prefs(context).getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        prefs(context).edit().putString(KEY_THEME_MODE, mode.name).apply()
    }
}
