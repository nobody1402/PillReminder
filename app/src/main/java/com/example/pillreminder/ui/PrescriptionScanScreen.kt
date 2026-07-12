@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.pillreminder.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.example.pillreminder.data.FoodRelation
import com.example.pillreminder.data.Pill
import com.example.pillreminder.data.PillRepository
import com.example.pillreminder.util.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

private fun uriToBitmap(context: Context, uri: Uri): Bitmap? = runCatching {
    if (Build.VERSION.SDK_INT >= 28) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, _, _ -> decoder.isMutableRequired = true }
    } else {
        @Suppress("DEPRECATION")
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    }
}.getOrNull()

private fun createCameraOutputUri(context: Context): Uri {
    val dir = File(context.cacheDir, "prescriptions").apply { mkdirs() }
    val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

// ---------------------------------------------------------------------------------
// صفحه «اسکن نسخه»
// ---------------------------------------------------------------------------------
@Composable
fun PrescriptionScanScreen(nav: NavHostController, repo: PillRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var cameraTargetUri by remember { mutableStateOf<Uri?>(null) }

    fun runOcr(bitmap: Bitmap) {
        PrescriptionScanState.isProcessing = true
        PrescriptionScanState.errorMessage = null
        PrescriptionScanState.parsedItems = emptyList()
        PrescriptionScanState.addedItemIndices = emptySet()
        scope.launch {
            when (val result = PrescriptionOcrEngine.recognize(context, bitmap)) {
                is PrescriptionOcrEngine.OcrResult.Success -> {
                    PrescriptionScanState.ocrText = result.text
                    PrescriptionScanState.ocrWords = result.words
                    
                    // ====== استفاده از Regex Parser جدید ======
                    val regexItems = TableRegexParser.parse(result.text)
                    if (regexItems.isNotEmpty()) {
                        PrescriptionScanState.parsedItems = regexItems
                    } else {
                        val tableResult = TablePrescriptionParser.parse(result.words)
                        PrescriptionScanState.parsedItems = tableResult ?: PrescriptionParser.parse(result.text)
                    }
                    // ==========================================
                }
                is PrescriptionOcrEngine.OcrResult.MissingLanguageData -> {
                    PrescriptionScanState.errorMessage = result.message
                }
                is PrescriptionOcrEngine.OcrResult.Error -> {
                    PrescriptionScanState.errorMessage = result.message
                }
            }
            PrescriptionScanState.isProcessing = false
        }
    }

    fun onImagePicked(uri: Uri?) {
        if (uri == null) return
        val bmp = uriToBitmap(context, uri)
        PrescriptionScanState.imageBitmap = bmp
        PrescriptionScanState.ocrText = ""
        PrescriptionScanState.parsedItems = emptyList()
        PrescriptionScanState.addedItemIndices = emptySet()
        if (bmp != null) runOcr(bmp) else PrescriptionScanState.errorMessage = "خواندن عکس ممکن نشد."
    }

    // انتخاب از گالری
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> onImagePicked(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) onImagePicked(cameraTargetUri)
    }

    fun launchCamera() {
        val uri = createCameraOutputUri(context)
        cameraTargetUri = uri
        cameraLauncher.launch(uri)
    }

    // مجوز دوربین
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCamera() else PrescriptionScanState.errorMessage = "برای گرفتن عکس، لازمه اجازه‌ی دسترسی به دوربین رو بدی." }

    fun onCameraButtonClick() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // ====== تابع افزودن همه داروها ======
    fun addAllRemaining() {
        val itemsToAdd = PrescriptionScanState.parsedItems.withIndex()
            .filter { (index, _) -> index !in PrescriptionScanState.addedItemIndices }
        if (itemsToAdd.isEmpty()) return
        scope.launch {
            val allPills = repo.getAllPillsSnapshot()
            for ((index, item) in itemsToAdd) {
                val pill = Pill(
                    name = item.name,
                    doseAmount = item.suggestedDoseAmount,
                    foodRelation = item.recognizedRule?.foodRelation ?: FoodRelation.NO_RELATION,
                    waitAfterMinutes = item.recognizedRule?.waitAfterMinutes ?: 0,
                    timesOfDay = item.suggestedTimesOfDay.sorted().joinToString(",") { TimeParseUtils.formatTime(it) },
                    startDateEpochDay = LocalDate.now().toEpochDay(),
                    treatmentDurationDays = null,
                    inventoryCount = null,
                    lowStockThresholdDays = 3
                )
                repo.addOrUpdatePill(pill, allPills, emptyList())
                PrescriptionScanState.addedItemIndices = PrescriptionScanState.addedItemIndices + index
            }
        }
    }
    // ==================================

    Scaffold(topBar = { TopAppBar(title = { Text("افزودن دارو از روی عکس نسخه") }) }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "عکس نسخه رو انتخاب کن؛ متنش به‌صورت کاملاً آفلاین (بدون اینترنت) خونده می‌شه و داروهای " +
                    "قابل‌تشخیص به‌صورت پیش‌نویس نشون داده می‌شن. حتماً قبل از ذخیره، ساعت و دوز هر کدوم رو بررسی کن.",
                fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))

            Row {
                Button(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Text("🖼 انتخاب از گالری")
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { onCameraButtonClick() }, modifier = Modifier.weight(1f)) {
                    Text("📷 گرفتن عکس")
                }
            }

            PrescriptionScanState.imageBitmap?.let { bmp ->
                Spacer(Modifier.height(16.dp))
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "عکس نسخه",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
                )
            }

            if (PrescriptionScanState.isProcessing) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("در حال خواندن متن نسخه...", fontSize = 13.sp)
                }
            }

            PrescriptionScanState.errorMessage?.let { err ->
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("⚠️ خطا", fontWeight = FontWeight.Bold)
                        Text(err, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            if (PrescriptionScanState.ocrText.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                
                Text(
                    "متن خوانده‌شده (در صورت نیاز اصلاح کن)",
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = PrescriptionScanState.ocrText,
                    onValueChange = { PrescriptionScanState.ocrText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        // ====== استفاده از Regex Parser ======
                        val regexItems = TableRegexParser.parse(PrescriptionScanState.ocrText)
                        if (regexItems.isNotEmpty()) {
                            PrescriptionScanState.parsedItems = regexItems
                        } else {
                            val tableResult = TablePrescriptionParser.parse(PrescriptionScanState.ocrWords)
                            PrescriptionScanState.parsedItems = tableResult ?: PrescriptionParser.parse(PrescriptionScanState.ocrText)
                        }
                        PrescriptionScanState.addedItemIndices = emptySet()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🔍 تحلیل و استخراج داروها") }
            }

            if (PrescriptionScanState.parsedItems.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("داروهای پیشنهادی (پیش‌نویس)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                
                // ====== دکمه افزودن همه ======
                val remainingCount = PrescriptionScanState.parsedItems.size - PrescriptionScanState.addedItemIndices.size
                if (remainingCount > 1) {
                    Button(
                        onClick = { addAllRemaining() },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) { 
                        Text("➕ افزودن همه ($remainingCount مورد) با تنظیمات پیشنهادی") 
                    }
                    Text(
                        "این گزینه بدون بازبینی تک‌تک، همه رو با ساعت/دوز پیشنهادی ذخیره می‌کنه — بعداً از تب «داروها» می‌تونی هرکدوم رو ویرایش کنی.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                // ==============================
                
                PrescriptionScanState.parsedItems.forEachIndexed { index, item ->
                    val alreadyAdded = PrescriptionScanState.addedItemIndices.contains(index)
                    Card(
                        Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            DrugKnowledgeBase.englishNameFor(item.name)?.let { en ->
                                Text(en, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (item.formHint != null) {
                                Text("شکل دارو: ${item.formHint}", fontSize = 12.sp)
                            }
                            if (item.quantity != null) {
                                Text("تعداد تجویزشده: ${item.quantity}", fontSize = 12.sp)
                            }
                            Text(
                                "ساعت‌های پیشنهادی: ${item.suggestedTimesOfDay.joinToString("، ") { TimeParseUtils.formatTime(it) }}",
                                fontSize = 12.sp
                            )
                            item.recognizedRule?.let { rule ->
                                Spacer(Modifier.height(4.dp))
                                Text("🧠 ${rule.note}", fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    DrugPrefillState.pending = item
                                    PrescriptionScanState.addedItemIndices = PrescriptionScanState.addedItemIndices + index
                                    nav.navigate("addPill")
                                },
                                enabled = !alreadyAdded,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(if (alreadyAdded) "✅ اضافه شد" else "➕ افزودن به لیست داروها") }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "⚠️ این‌ها فقط پیش‌نویس هستن؛ ممکنه OCR اسم یا تعداد رو اشتباه خونده باشه. حتماً قبل از " +
                        "ذخیره‌ی نهایی هر دارو، ساعت‌ها و دوز رو در فرم بررسی و در صورت نیاز اصلاح کن.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { PrescriptionScanState.reset() }, modifier = Modifier.fillMaxWidth()) {
                    Text("🗑 شروع دوباره با عکس جدید")
                }
            }
        }
    }
}
