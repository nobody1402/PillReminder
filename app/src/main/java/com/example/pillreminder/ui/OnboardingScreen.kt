@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.pillreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pillreminder.util.SettingsStore
import kotlinx.coroutines.launch

private data class OnboardingSlide(
    val emoji: String,
    val title: String,
    val description: String
)

private val slides = listOf(
    OnboardingSlide(
        emoji = "💊",
        title = "به «آلارم دارو» خوش اومدی",
        description = "این اپ کمکت می‌کنه داروهاتو سر وقت یادت نره؛ کاملاً آفلاین و مخصوص کاربر ایرانی."
    ),
    OnboardingSlide(
        emoji = "➕",
        title = "افزودن دارو، سه روش",
        description = "می‌تونی دستی مرحله‌به‌مرحله وارد کنی، یه جمله بنویسی تا خودکار پر بشه، یا فقط عکس نسخه رو بگیری تا داروها رو خودش تشخیص بده."
    ),
    OnboardingSlide(
        emoji = "🧠",
        title = "پیشنهاد هوشمند و حافظه‌ی داروها",
        description = "برای داروهای شناخته‌شده (مثل لووتیروکسین یا آهن) خودش قانون مصرف رو پیشنهاد می‌ده؛ و اگه قبلاً یه دارو رو ثبت کرده باشی، دفعه بعد فقط اسمش رو بنویس، بقیه خودکار پر می‌شه."
    ),
    OnboardingSlide(
        emoji = "⏰",
        title = "امروز و آلارم‌ها",
        description = "توی تب «امروز» برنامه‌ی همون روز رو می‌بینی. سر وقت هر دارو، حتی روی صفحه قفل، یه اعلان کامل با دکمه‌های «مصرف کردم»، «۱۰ دقیقه بعد» و «رد شد» میاد."
    ),
    OnboardingSlide(
        emoji = "📊",
        title = "تداخل‌ها و تاریخچه",
        description = "می‌تونی قانون فاصله‌ی زمانی بین دو دارو تعریف کنی، و توی تب «تاریخچه» درصد پایبندی و گزارش روزانه‌ی واقعی خودتو ببینی."
    ),
    OnboardingSlide(
        emoji = "🔔",
        title = "یه قدم آخر مهم",
        description = "توی «تنظیمات» یه بخش هست به اسم «چرا آلارم نمی‌آید؟» — حتماً یه‌بار برو اونجا و مطمئن شو همه‌ی مجوزها فعاله، وگرنه ممکنه بعضی آلارم‌ها زده نشن."
    )
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { slides.size })
    val scope = rememberCoroutineScope()

    fun finish() {
        SettingsStore.setOnboardingSeen(context)
        onFinished()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextButton(onClick = { finish() }) { Text("رد کردن") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
                val slide = slides[page]
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(slide.emoji, fontSize = 64.sp)
                    Spacer(Modifier.height(24.dp))
                    Text(slide.title, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        slide.description,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(slides.size) { index ->
                    val selected = pagerState.currentPage == index
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 10.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(16.dp)) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        modifier = Modifier.weight(1f)
                    ) { Text("قبلی") }
                    Spacer(Modifier.width(8.dp))
                }
                Button(
                    onClick = {
                        if (pagerState.currentPage == slides.size - 1) {
                            finish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (pagerState.currentPage == slides.size - 1) "شروع کن" else "بعدی")
                }
            }
        }
    }
}
