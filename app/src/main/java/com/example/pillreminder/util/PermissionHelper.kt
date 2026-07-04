package com.example.pillreminder.util

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun isExactAlarmGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 31) return true
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return am.canScheduleExactAlarms()
    }

    /** کاربر را مستقیماً به صفحه‌ی "آلارم‌ها و یادآوری‌ها" برای همین اپ می‌برد */
    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= 31) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    fun isXiaomi(): Boolean =
        Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

    /** صفحه‌ی «اجرای خودکار» MIUI را باز می‌کند (روی اکثر نسخه‌های MIUI کار می‌کند، ولی تضمینی نیست) */
    fun openXiaomiAutoStartSettings(context: Context): Boolean {
        val attempts = listOf(
            Intent().setComponent(
                android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            Intent().setComponent(
                android.content.ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity2"
                )
            ),
            Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                putExtra("extra_pkgname", context.packageName)
            }
        )
        for (intent in attempts) {
            try {
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // این نسخه از MIUI این اکتیویتی را ندارد، بعدی را امتحان کن
            }
        }
        // اگر هیچ‌کدام کار نکرد، حداقل صفحه‌ی تنظیمات خود اپ را باز کن
        openAppSettings(context)
        return false
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** درخواست مستقیم معافیت از بهینه‌سازی باتری برای این اپ */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }

    fun isNotificationPermissionGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.POST_NOTIFICATIONS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    /** صفحه تنظیمات اپ (برای مواقعی که کاربر یک‌بار مجوز نوتیفیکیشن را رد کرده و دیگر دیالوگ نمی‌آید) */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    }

    /** از اندروید ۱۴ (API 34) به بعد، برای این‌که نوتیف بلافاصله و حتی روی صفحه‌ی قفل ظاهر شود، این مجوز باید جدا داده شود */
    fun isFullScreenIntentGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        return nm.canUseFullScreenIntent()
    }

    fun openFullScreenIntentSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= 34) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                // بعضی گوشی‌ها این صفحه را ندارند
            }
        }
        openAppSettings(context)
    }
}
