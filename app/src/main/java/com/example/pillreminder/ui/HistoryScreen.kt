@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.pillreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.pillreminder.data.DoseStatus
import com.example.pillreminder.data.PillRepository
import com.example.pillreminder.util.DayAdherence
import com.example.pillreminder.util.HistoryBuilder
import java.time.LocalDate
import java.time.ZoneId

private val persianWeekdayShort = mapOf(
    java.time.DayOfWeek.SATURDAY to "ش",
    java.time.DayOfWeek.SUNDAY to "ی",
    java.time.DayOfWeek.MONDAY to "د",
    java.time.DayOfWeek.TUESDAY to "س",
    java.time.DayOfWeek.WEDNESDAY to "چ",
    java.time.DayOfWeek.THURSDAY to "پ",
    java.time.DayOfWeek.FRIDAY to "ج"
)

// ---------------------------------------------------------------------------------
// صفحه «تاریخچه و پیشرفت»: درصد پایبندی ۷ روز اخیر، نمای هفتگی، و گزارش روزانه —
// همه از روی داده‌ی واقعیِ dose_logs محاسبه می‌شه، نه عدد نمایشی ساختگی.
// ---------------------------------------------------------------------------------
@Composable
fun HistoryScreen(nav: NavHostController, repo: PillRepository) {
    val zone = ZoneId.systemDefault()
    val today = remember { LocalDate.now(zone) }
    val rangeStart = remember { today.minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli() }
    val rangeEnd = remember { today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() }

    val pills by repo.observePills().collectAsState(initial = emptyList())
    val logs by repo.observeLogsForDay(rangeStart, rangeEnd).collectAsState(initial = emptyList())

    val weekly = remember(pills, logs) { HistoryBuilder.buildWeekly(pills, logs) }
    val overallPercent = remember(weekly) { HistoryBuilder.overallPercent(weekly) }

    var selectedDay by remember { mutableStateOf(today) }
    val dailyLog = remember(pills, logs, selectedDay) { HistoryBuilder.buildDailyLog(pills, logs, selectedDay) }

    BottomBarScaffold(nav) { modifier ->
        Column(modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("تاریخچه و پیشرفت", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))

            // --- دایره‌ی درصد پایبندی کلی هفته اخیر ---
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().height(180.dp)) {
                CircularProgressIndicator(
                    progress = { overallPercent / 100f },
                    modifier = Modifier.size(150.dp),
                    strokeWidth = 12.dp,
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$overallPercent٪", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text(adherenceMessage(overallPercent), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("۷ روز اخیر", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weekly.forEach { day -> DayCircle(day, selected = day.date == selectedDay) { selectedDay = day.date } }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                if (selectedDay == today) "گزارش امروز" else "گزارش روز ${dayLabel(selectedDay)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            if (dailyLog.isEmpty()) {
                Text("برای این روز هنوز نوبتی نرسیده یا دارویی ثبت نشده.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                dailyLog.forEach { entry ->
                    val (icon, color) = when {
                        entry.status == DoseStatus.TAKEN -> "✅" to MaterialTheme.colorScheme.secondary
                        entry.status == DoseStatus.SKIPPED -> "❌" to MaterialTheme.colorScheme.error
                        !entry.hasLog -> "⚠️" to MaterialTheme.colorScheme.tertiary
                        else -> "🕗" to MaterialTheme.colorScheme.outline
                    }
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(
                            Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(entry.pillName, fontWeight = FontWeight.Medium)
                                Text(entry.timeText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "$icon ${statusLabel(entry)}",
                                color = color,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCircle(day: DayAdherence, selected: Boolean, onClick: () -> Unit) {
    val color = when {
        day.totalDue == 0 -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        day.percent >= 80 -> MaterialTheme.colorScheme.secondary
        day.percent >= 40 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (selected) color else color.copy(alpha = 0.35f))
                .then(Modifier.clickableSafe(onClick)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (day.totalDue == 0) "–" else "${day.percent}",
                fontSize = 11.sp,
                color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(persianWeekdayShort[day.date.dayOfWeek] ?: "", fontSize = 11.sp)
    }
}

private fun adherenceMessage(percent: Int): String = when {
    percent >= 90 -> "عالی! همین‌طور ادامه بده"
    percent >= 70 -> "خوبه، فقط چندتا جا افتاده"
    percent >= 40 -> "بهتره بیشتر مراقب باشی"
    else -> "نیاز به توجه بیشتر داره"
}

private fun statusLabel(entry: com.example.pillreminder.util.DailyLogEntry): String = when {
    entry.status == DoseStatus.TAKEN -> "مصرف شد"
    entry.status == DoseStatus.SKIPPED -> "رد شد"
    !entry.hasLog -> "ثبت نشده"
    else -> "نامشخص"
}

private fun dayLabel(date: LocalDate): String = com.example.pillreminder.util.PersianDateUtils.formatShort(date.toEpochDay())
