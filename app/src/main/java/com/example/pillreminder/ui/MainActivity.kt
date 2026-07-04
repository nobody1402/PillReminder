@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.pillreminder.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.pillreminder.alarm.AlarmScheduler
import com.example.pillreminder.alarm.InventoryCheckWorker
import com.example.pillreminder.alarm.NotificationHelper
import com.example.pillreminder.data.*
import com.example.pillreminder.util.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class MainActivity : ComponentActivity() {

    private val requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // قبلاً وقتی آلارم زده می‌شد و صفحه قفل بود، اپ فقط پشت صفحه قفل رزیوم می‌شد و
        // دیده نمی‌شد. این پرچم‌ها باعث می‌شن اپ واقعاً روی صفحه قفل بیاد بالا و صفحه روشن بشه.
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(android.app.KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        NotificationHelper.ensureChannel(this)
        InventoryCheckWorker.schedule(this)
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        AppThemeState.mode.value = SettingsStore.getThemeMode(this)

        val repo = PillRepository(applicationContext)

        setContent {
            PillReminderTheme {
                val nav = rememberNavController()
                AppNav(nav, repo)
            }
        }
    }
}

@Composable
fun AppNav(nav: NavHostController, repo: PillRepository) {
    NavHost(navController = nav, startDestination = "today") {
        composable("today") { TodayScreen(nav, repo) }
        composable("pills") { PillListScreen(nav, repo) }
        composable("addPill") { AddEditPillScreen(nav, repo, existing = null) }
        composable("editPill/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toLongOrNull()
            AddEditPillScreen(nav, repo, existingId = id)
        }
        composable("rules") { InteractionRulesScreen(nav, repo) }
        composable("settings") { SettingsScreen(nav) }
        composable("scanPrescription") { PrescriptionScanScreen(nav) }
    }
}

@Composable
fun ThemeToggleButton() {
    val context = LocalContext.current
    val isDarkNow = when (AppThemeState.mode.value) {
        com.example.pillreminder.util.ThemeMode.DARK -> true
        com.example.pillreminder.util.ThemeMode.LIGHT -> false
        com.example.pillreminder.util.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    TextButton(onClick = {
        val next = if (isDarkNow) com.example.pillreminder.util.ThemeMode.LIGHT else com.example.pillreminder.util.ThemeMode.DARK
        AppThemeState.mode.value = next
        SettingsStore.setThemeMode(context, next)
    }) {
        Text(if (isDarkNow) "☀️ روشن" else "🌙 تاریک")
    }
}

@Composable
fun BottomBarScaffold(nav: NavHostController, content: @Composable (Modifier) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("آلارم دارو") },
                actions = { ThemeToggleButton() }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = false, onClick = { nav.navigate("today") },
                    icon = {}, label = { Text("امروز") })
                NavigationBarItem(selected = false, onClick = { nav.navigate("pills") },
                    icon = {}, label = { Text("داروها") })
                NavigationBarItem(selected = false, onClick = { nav.navigate("rules") },
                    icon = {}, label = { Text("تداخل‌ها") })
                NavigationBarItem(selected = false, onClick = { nav.navigate("settings") },
                    icon = {}, label = { Text("تنظیمات") })
            }
        }
    ) { padding -> content(Modifier.padding(padding)) }
}

// ---------------------------------------------------------------------------------
// صفحه امروز: خلاصه وضعیت مصرف امروز، مثل: ✅ متفورمین صبح، ❌ ویتامین D، 🕗 آسپرین ۲۲:۰۰
// ---------------------------------------------------------------------------------
@Composable
fun TodayScreen(nav: NavHostController, repo: PillRepository) {
    val context = LocalContext.current
    val elderlyMode by produceState(initialValue = false, key1 = context) {
        value = SettingsStore.isElderlyMode(context)
    }
    val pills by repo.observePills().collectAsState(initial = emptyList())
    val zone = ZoneId.systemDefault()
    val dayStart = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    val dayEnd = dayStart + 24 * 3600 * 1000 - 1
    val logs by repo.observeLogsForDay(dayStart, dayEnd).collectAsState(initial = emptyList())
    val items = remember(pills, logs) { TodayScheduleBuilder.build(pills, logs) }

    BottomBarScaffold(nav) { modifier ->
        Column(modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "برنامه امروز",
                fontSize = if (elderlyMode) 26.sp else 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            if (items.isEmpty()) {
                Text("هنوز دارویی ثبت نکرده‌اید.", fontSize = if (elderlyMode) 22.sp else 16.sp)
            }
            LazyColumn {
                items(items) { item ->
                    val (icon, accentColor) = when {
                        item.status == DoseStatus.TAKEN -> "✅" to Color(0xFF2E7D5B)
                        item.status == DoseStatus.SKIPPED -> "❌" to MaterialTheme.colorScheme.error
                        item.isOverdue -> "⏰" to Color(0xFFE0A100)
                        else -> "🕗" to MaterialTheme.colorScheme.outline
                    }
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            Box(
                                Modifier
                                    .width(5.dp)
                                    .fillMaxHeight()
                                    .background(accentColor)
                            )
                            Row(
                                Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "$icon ${item.pillName}",
                                        fontSize = if (elderlyMode) 26.sp else 18.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${item.timeText} • ${item.doseText}",
                                        fontSize = if (elderlyMode) 20.sp else 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// لیست داروها
// ---------------------------------------------------------------------------------
@Composable
fun PillListScreen(nav: NavHostController, repo: PillRepository) {
    val pills by repo.observePills().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("داروهای من") },
                actions = { ThemeToggleButton() }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(onClick = { nav.navigate("scanPrescription") }) {
                    Text("📷", fontSize = 18.sp)
                }
                Spacer(Modifier.height(10.dp))
                FloatingActionButton(onClick = { nav.navigate("addPill") }) { Icon(Icons.Default.Add, contentDescription = "افزودن دارو") }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = false, onClick = { nav.navigate("today") }, icon = {}, label = { Text("امروز") })
                NavigationBarItem(selected = true, onClick = { }, icon = {}, label = { Text("داروها") })
                NavigationBarItem(selected = false, onClick = { nav.navigate("rules") }, icon = {}, label = { Text("تداخل‌ها") })
                NavigationBarItem(selected = false, onClick = { nav.navigate("settings") }, icon = {}, label = { Text("تنظیمات") })
            }
        }
    ) { padding ->
        if (pills.isEmpty()) {
            Column(
                Modifier.padding(padding).fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("هنوز دارویی اضافه نکردی", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text("با دکمه‌ی + یه دارو اضافه کن", fontSize = 14.sp)
            }
        }
        LazyColumn(Modifier.padding(padding).padding(16.dp)) {
            items(pills) { pill ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickableSafe { nav.navigate("editPill/${pill.id}") }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(pill.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("ساعات: ${pill.timesOfDay}")
                        Text("دوز: ${PillTextFormatter.doseAmountText(pill.doseAmount)}")
                        if (pill.inventoryCount != null) {
                            Text("موجودی: ${pill.inventoryCount} واحد")
                        }
                        Row(Modifier.padding(top = 8.dp)) {
                            TextButton(onClick = {
                                scope.launch { repo.deletePill(pill) }
                            }) { Text("حذف") }
                        }
                    }
                }
            }
        }
    }
}

// کمکی برای جلوگیری از وابستگی اضافه به foundation.clickable در همه جا
fun Modifier.clickableSafe(onClick: () -> Unit): Modifier =
    this.clickable(onClick = onClick)

// ---------------------------------------------------------------------------------
// فرم افزودن / ویرایش دارو
// ---------------------------------------------------------------------------------
@Composable
fun AddEditPillScreen(nav: NavHostController, repo: PillRepository, existing: Pill? = null, existingId: Long? = null) {
    val scope = rememberCoroutineScope()
    val pillsState by repo.observePills().collectAsState(initial = emptyList())
    val loaded = remember(pillsState, existingId, existing) {
        existing ?: existingId?.let { id -> pillsState.find { it.id == id } }
    }

    var name by remember(loaded) { mutableStateOf(loaded?.name ?: "") }
    var doseAmount by remember(loaded) { mutableStateOf(loaded?.doseAmount ?: 1.0) }
    var foodRelation by remember(loaded) { mutableStateOf(loaded?.foodRelation ?: FoodRelation.NO_RELATION) }
    var waitAfter by remember(loaded) { mutableStateOf((loaded?.waitAfterMinutes ?: 0).toString()) }
    var timesList by remember(loaded) {
        mutableStateOf(
            TimeParseUtils.safeParseList(loaded?.timesOfDay ?: "08:00").ifEmpty { listOf(java.time.LocalTime.of(8, 0)) }
        )
    }
    var durationDays by remember(loaded) { mutableStateOf(loaded?.treatmentDurationDays?.toString() ?: "") }
    var inventory by remember(loaded) { mutableStateOf(loaded?.inventoryCount?.toString() ?: "") }
    var lowStockDays by remember(loaded) { mutableStateOf((loaded?.lowStockThresholdDays ?: 3).toString()) }
    var warningText by remember { mutableStateOf<String?>(null) }

    // اگر از صفحه‌ی «اسکن نسخه» اومده باشیم، یک بار فرم رو با پیشنهاد اون دارو پر می‌کنیم
    var prefillConsumed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!prefillConsumed && existingId == null) {
            DrugPrefillState.pending?.let { item ->
                name = item.name
                doseAmount = item.suggestedDoseAmount
                if (item.suggestedTimesOfDay.isNotEmpty()) timesList = item.suggestedTimesOfDay
                item.recognizedRule?.foodRelation?.let { foodRelation = it }
                item.recognizedRule?.waitAfterMinutes?.let { if (it > 0) waitAfter = it.toString() }
                DrugPrefillState.pending = null
            }
        }
        prefillConsumed = true
    }

    // پیشنهاد هوشمند بر اساس نام دارو: هم سابقه‌ی قبلیِ همین کاربر، هم قوانین داخلی
    // (لووتیروکسین/آهن/آنتی‌بیوتیک و ...)، حتی اگر کاربر چیزی درباره رابطه با غذا وارد نکرده باشد.
    var historySuggestion by remember { mutableStateOf<DrugHistory?>(null) }
    var ruleSuggestion by remember { mutableStateOf<DrugRuleSuggestion?>(null) }
    var ruleSuggestionDismissed by remember(loaded) { mutableStateOf(false) }

    LaunchedEffect(name, existingId) {
        if (existingId == null && name.isNotBlank()) {
            kotlinx.coroutines.delay(350) // debounce تا با هر حرف کوئری نزنیم
            historySuggestion = repo.findDrugHistory(name)
            ruleSuggestion = DrugKnowledgeBase.findRule(name)
        } else {
            historySuggestion = null
            ruleSuggestion = null
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(if (existingId == null) "افزودن داروی جدید" else "ویرایش دارو") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScrollSafe()
        ) {
            if (existingId == null) {
                var quickText by remember { mutableStateOf("") }
                var quickMissing by remember { mutableStateOf<List<String>?>(null) }
                Card(Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("✨ ثبت سریع با متن", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "مثال: استامینوفن هر هشت ساعت دوتا شروع از هشت صبح موجودی سی عدد",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = quickText,
                            onValueChange = { quickText = it },
                            label = { Text("توضیح رو اینجا بنویس") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val result = QuickEntryParser.parse(quickText)
                                result.name?.let { name = it }
                                result.doseAmount?.let { doseAmount = it }
                                if (result.timesOfDay.isNotEmpty()) timesList = result.timesOfDay
                                result.inventoryCount?.let { inventory = it.toString() }
                                quickMissing = result.missingFields
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("تجزیه و پر کردن فرم پایین") }
                        quickMissing?.let { missing ->
                            Spacer(Modifier.height(8.dp))
                            if (missing.isEmpty()) {
                                Text("✅ همه چیز تشخیص داده شد؛ فرم پایین رو چک کن و ذخیره کن.", fontSize = 12.sp)
                            } else {
                                Text(
                                    "⚠️ این موارد تشخیص داده نشد، لطفاً دستی توی فرم پایین پرشون کن: ${missing.joinToString("، ")}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("نام دارو") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))

            historySuggestion?.let { hist ->
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("🕘 سابقه یافت شد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "قبلاً «${hist.displayName}» رو با دوز ${PillTextFormatter.doseAmountText(hist.doseAmount)} و ساعت‌های ${hist.timesOfDay} ثبت کرده بودی.",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                        Button(onClick = {
                            doseAmount = hist.doseAmount
                            foodRelation = hist.foodRelation
                            waitAfter = hist.waitAfterMinutes.toString()
                            timesList = TimeParseUtils.safeParseList(hist.timesOfDay).ifEmpty { timesList }
                            durationDays = hist.treatmentDurationDays?.toString() ?: ""
                            lowStockDays = hist.lowStockThresholdDays.toString()
                        }, modifier = Modifier.fillMaxWidth()) { Text("استفاده از تنظیمات قبلی") }
                    }
                }
            }

            if (ruleSuggestion != null && !ruleSuggestionDismissed) {
                val rule = ruleSuggestion!!
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("🧠 پیشنهاد هوشمند", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(rule.note, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                        Row {
                            Button(onClick = {
                                rule.foodRelation?.let { foodRelation = it }
                                if (rule.waitAfterMinutes > 0) waitAfter = rule.waitAfterMinutes.toString()
                                rule.fixedIntervalHours?.let { hours ->
                                    val start = timesList.minOrNull() ?: java.time.LocalTime.of(8, 0)
                                    val startMinutes = start.hour * 60 + start.minute
                                    val numTimes = (24 / hours).coerceAtLeast(1)
                                    timesList = (0 until numTimes).map { i ->
                                        val m = (startMinutes + i * hours * 60) % 1440
                                        java.time.LocalTime.of(m / 60, m % 60)
                                    }.distinct().sorted()
                                }
                                ruleSuggestionDismissed = true
                            }, modifier = Modifier.weight(1f)) { Text("اعمال") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { ruleSuggestionDismissed = true }, modifier = Modifier.weight(1f)) { Text("نادیده بگیر") }
                        }
                    }
                }
            }

            Text("مقدار مصرف")
            Row {
                listOf(1.0 to "یک واحد", 0.5 to "نصف", 0.25 to "یک‌چهارم", 2.0 to "دو واحد").forEach { (v, label) ->
                    FilterChip(selected = doseAmount == v, onClick = { doseAmount = v }, label = { Text(label) }, modifier = Modifier.padding(end = 4.dp))
                }
            }
            Spacer(Modifier.height(8.dp))

            Text("رابطه با غذا")
            Row {
                FilterChip(selected = foodRelation == FoodRelation.BEFORE_FOOD, onClick = { foodRelation = FoodRelation.BEFORE_FOOD }, label = { Text("قبل غذا") }, modifier = Modifier.padding(end = 4.dp))
                FilterChip(selected = foodRelation == FoodRelation.AFTER_FOOD, onClick = { foodRelation = FoodRelation.AFTER_FOOD }, label = { Text("بعد غذا") }, modifier = Modifier.padding(end = 4.dp))
                FilterChip(selected = foodRelation == FoodRelation.WITH_FOOD, onClick = { foodRelation = FoodRelation.WITH_FOOD }, label = { Text("همراه غذا") }, modifier = Modifier.padding(end = 4.dp))
                FilterChip(selected = foodRelation == FoodRelation.NO_RELATION, onClick = { foodRelation = FoodRelation.NO_RELATION }, label = { Text("فرقی ندارد") })
            }
            if (foodRelation == FoodRelation.BEFORE_FOOD) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = waitAfter, onValueChange = { waitAfter = it }, label = { Text("چند دقیقه بعد می‌توان غذا خورد؟") }, modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(8.dp))
            Text("ساعات مصرف")
            var showTimePicker by remember { mutableStateOf(false) }
            Row(modifier = Modifier.fillMaxWidth()) {
                timesList.sorted().forEach { t ->
                    AssistChip(
                        onClick = { timesList = timesList.filter { it != t } },
                        label = { Text("${TimeParseUtils.formatTime(t)}  ✕") },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
                AssistChip(onClick = { showTimePicker = true }, label = { Text("+ افزودن ساعت") })
            }
            if (showTimePicker) {
                TimePickerDialog(
                    initial = java.time.LocalTime.of(8, 0),
                    onDismiss = { showTimePicker = false },
                    onConfirm = { picked ->
                        if (timesList.none { it == picked }) timesList = timesList + picked
                        showTimePicker = false
                    }
                )
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = durationDays, onValueChange = { durationDays = it }, label = { Text("تعداد روزهای درمان (خالی = نامحدود)") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = inventory, onValueChange = { inventory = it }, label = { Text("موجودی فعلی (تعداد واحد، اختیاری)") }, modifier = Modifier.fillMaxWidth())

            if (inventory.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = lowStockDays, onValueChange = { lowStockDays = it }, label = { Text("چند روز مانده هشدار بده؟") }, modifier = Modifier.fillMaxWidth())
            }

            warningText?.let {
                Spacer(Modifier.height(8.dp))
                Card { Text("⚠️ $it", Modifier.padding(12.dp)) }
            }

            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                scope.launch {
                    val pill = Pill(
                        id = loaded?.id ?: 0,
                        name = name,
                        doseAmount = doseAmount,
                        foodRelation = foodRelation,
                        waitAfterMinutes = waitAfter.toIntOrNull() ?: 0,
                        timesOfDay = timesList.sorted().joinToString(",") { TimeParseUtils.formatTime(it) },
                        startDateEpochDay = loaded?.startDateEpochDay ?: LocalDate.now().toEpochDay(),
                        treatmentDurationDays = durationDays.toIntOrNull(),
                        inventoryCount = inventory.toDoubleOrNull(),
                        lowStockThresholdDays = lowStockDays.toIntOrNull() ?: 3
                    )
                    val allPills = repo.getAllPillsSnapshot()
                    val rules = emptyList<InteractionRule>() // قوانین از صفحه تداخل‌ها مدیریت می‌شوند
                    val warnings = repo.addOrUpdatePill(pill, allPills, rules)
                    if (warnings.isNotEmpty()) {
                        warningText = warnings.joinToString("\n") { it.message }
                    } else {
                        nav.popBackStack()
                    }
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("ذخیره")
            }
        }
    }
}

@Composable
fun Modifier.verticalScrollSafe(): Modifier =
    this.verticalScroll(rememberScrollState())

@Composable
fun TimePickerDialog(
    initial: java.time.LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (java.time.LocalTime) -> Unit
) {
    val state = androidx.compose.material3.rememberTimePickerState(
        initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true
    )
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.material3.TimePicker(state = state)
                Row(Modifier.padding(top = 8.dp)) {
                    TextButton(onClick = onDismiss) { Text("انصراف") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(java.time.LocalTime.of(state.hour, state.minute)) }) { Text("تایید") }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// صفحه قوانین تداخل بین دو دارو
// ---------------------------------------------------------------------------------
@Composable
fun InteractionRulesScreen(nav: NavHostController, repo: PillRepository) {
    val pills by repo.observePills().collectAsState(initial = emptyList())
    val rules by repo.observeRules().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var pillAId by remember { mutableStateOf<Long?>(null) }
    var pillBId by remember { mutableStateOf<Long?>(null) }
    var gapHours by remember { mutableStateOf("2") }

    BottomBarScaffold(nav) { modifier ->
        Column(modifier.padding(16.dp).verticalScrollSafe()) {
            Text("قوانین فاصله زمانی بین داروها", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                items(rules) { rule ->
                    val a = pills.find { it.id == rule.pillAId }?.name ?: "؟"
                    val b = pills.find { it.id == rule.pillBId }?.name ?: "؟"
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$a  ⇔  $b  •  حداقل ${rule.minGapMinutes / 60} ساعت")
                            TextButton(onClick = { scope.launch { repo.deleteRule(rule.id) } }) { Text("حذف") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("افزودن قانون جدید")
            PillDropdown("داروی اول", pills, pillAId) { pillAId = it }
            Spacer(Modifier.height(8.dp))
            PillDropdown("داروی دوم", pills, pillBId) { pillBId = it }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = gapHours, onValueChange = { gapHours = it }, label = { Text("حداقل فاصله (ساعت)") })
            Spacer(Modifier.height(8.dp))
            Button(onClick = {
                val a = pillAId; val b = pillBId
                if (a != null && b != null && a != b) {
                    scope.launch {
                        repo.addRule(InteractionRule(pillAId = a, pillBId = b, minGapMinutes = (gapHours.toIntOrNull() ?: 2) * 60))
                    }
                }
            }) { Text("افزودن قانون") }
        }
    }
}

@Composable
fun PillDropdown(label: String, pills: List<Pill>, selected: Long?, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = pills.find { it.id == selected }?.name ?: label
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(selectedName) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            pills.forEach { pill ->
                DropdownMenuItem(text = { Text(pill.name) }, onClick = { onSelect(pill.id); expanded = false })
            }
        }
    }
}

// ---------------------------------------------------------------------------------
// تنظیمات: حالت ساده برای سالمندان
// ---------------------------------------------------------------------------------
@Composable
fun SettingsScreen(nav: NavHostController) {
    val context = LocalContext.current
    var elderlyMode by remember { mutableStateOf(SettingsStore.isElderlyMode(context)) }

    // هر بار که این صفحه دوباره نمایش داده می‌شود (مثلا بعد از برگشت از تنظیمات سیستم) وضعیت را دوباره چک کن
    var refreshKey by remember { mutableStateOf(0) }
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) refreshKey++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val exactAlarmOk = remember(refreshKey) { PermissionHelper.isExactAlarmGranted(context) }
    val batteryOk = remember(refreshKey) { PermissionHelper.isIgnoringBatteryOptimizations(context) }
    val notifOk = remember(refreshKey) { PermissionHelper.isNotificationPermissionGranted(context) }
    val fullScreenOk = remember(refreshKey) { PermissionHelper.isFullScreenIntentGranted(context) }

    BottomBarScaffold(nav) { modifier ->
        Column(modifier.padding(24.dp).verticalScrollSafe()) {
            Text("تنظیمات", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("حالت ساده برای سالمندان", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                    Text("فونت بزرگ، صدای خواندن نام دارو، ویبره قوی و اعلان‌های ماندگار", fontSize = 14.sp)
                }
                Switch(checked = elderlyMode, onCheckedChange = {
                    elderlyMode = it
                    SettingsStore.setElderlyMode(context, it)
                })
            }

            Spacer(Modifier.height(24.dp))
            Text("پوسته اپ", fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row {
                FilterChip(
                    selected = AppThemeState.mode.value == ThemeMode.LIGHT,
                    onClick = { AppThemeState.mode.value = ThemeMode.LIGHT; SettingsStore.setThemeMode(context, ThemeMode.LIGHT) },
                    label = { Text("☀️ روشن") },
                    modifier = Modifier.padding(end = 6.dp)
                )
                FilterChip(
                    selected = AppThemeState.mode.value == ThemeMode.DARK,
                    onClick = { AppThemeState.mode.value = ThemeMode.DARK; SettingsStore.setThemeMode(context, ThemeMode.DARK) },
                    label = { Text("🌙 تاریک") },
                    modifier = Modifier.padding(end = 6.dp)
                )
                FilterChip(
                    selected = AppThemeState.mode.value == ThemeMode.SYSTEM,
                    onClick = { AppThemeState.mode.value = ThemeMode.SYSTEM; SettingsStore.setThemeMode(context, ThemeMode.SYSTEM) },
                    label = { Text("📱 مطابق گوشی") }
                )
            }

            Spacer(Modifier.height(32.dp))
            Text("چرا آلارم نمی‌آید؟", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("این سه مورد باید همه ✅ باشند وگرنه سیستم‌عامل جلوی آلارم را می‌گیرد:", fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))

            PermissionRow(
                title = "مجوز آلارم دقیق",
                ok = exactAlarmOk,
                onFix = { PermissionHelper.openExactAlarmSettings(context) }
            )
            PermissionRow(
                title = "معافیت از بهینه‌سازی باتری",
                ok = batteryOk,
                onFix = { PermissionHelper.requestIgnoreBatteryOptimizations(context) }
            )
            PermissionRow(
                title = "مجوز نمایش نوتیفیکیشن",
                ok = notifOk,
                onFix = { PermissionHelper.openAppSettings(context) }
            )
            PermissionRow(
                title = "نمایش فوری روی صفحه (حتی قفل)",
                ok = fullScreenOk,
                onFix = { PermissionHelper.openFullScreenIntentSettings(context) }
            )

            if (PermissionHelper.isXiaomi()) {
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("⚠️ گوشی شیائومی شناسایی شد", fontWeight = FontWeight.Bold)
                        Text(
                            "روی گوشی‌های شیائومی (MIUI) حتی با فعال بودن سه مورد بالا، خود سیستم MIUI ممکن است اپ رو در پس‌زمینه بکشه. حتماً این‌ها رو هم انجام بده:",
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                        Text("۱. «اجرای خودکار» (Autostart) رو براش روشن کن", fontSize = 13.sp)
                        Text("۲. توی برنامه‌ی امنیت (Security) → صرفه‌جویی باتری → این اپ رو بذار روی «بدون محدودیت»", fontSize = 13.sp)
                        Text("۳. توی صفحه‌ی برنامه‌های اخیر (Recent Apps)، کارت این اپ رو نگه‌دار و قفلش کن (آیکون قفل)", fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { PermissionHelper.openXiaomiAutoStartSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                            Text("باز کردن تنظیمات اجرای خودکار")
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            var testSent by remember { mutableStateOf(false) }
            var testError by remember { mutableStateOf<String?>(null) }
            Button(
                onClick = {
                    try {
                        AlarmScheduler.scheduleTestAlarm(context, secondsFromNow = 10)
                        testSent = true
                        testError = null
                    } catch (e: Throwable) {
                        testError = "${e.javaClass.simpleName}: ${e.message}"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("🔔 تست آلارم (۱۰ ثانیه دیگر زنگ می‌زند)") }
            if (testSent) {
                Spacer(Modifier.height(8.dp))
                Text("اپ رو ببند و صفحه گوشی رو خاموش کن. اگه تا ۱۰ ثانیه دیگه نوتیفیکیشن نیومد، یعنی هنوز یکی از سه مورد بالا مشکل داره.", fontSize = 13.sp)
            }
            testError?.let { err ->
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("❌ همین الان کرش شد، متن خطا:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(err, fontSize = 13.sp)
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
            Text(
                "created by H03ei",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

@Composable
fun PermissionRow(title: String, ok: Boolean, onFix: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text((if (ok) "✅ " else "❌ ") + title)
            if (!ok) {
                Button(onClick = onFix) { Text("فعال‌سازی") }
            }
        }
    }
}
