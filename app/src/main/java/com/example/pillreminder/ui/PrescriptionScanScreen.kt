package com.example.pillreminder.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.example.pillreminder.util.DrugPrefillState
import com.example.pillreminder.util.ParsedPrescriptionItem
import com.example.pillreminder.util.PrescriptionOcrEngine
import com.example.pillreminder.util.PrescriptionParser
import kotlinx.coroutines.launch
import java.io.File

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
// صفحه «اسکن نسخه»: کاربر عکس نسخه رو از گالری/دوربین انتخاب می‌کنه، متن فارسی‌اش
// به‌صورت کاملاً آفلاین (روی خودِ گوشی) خونده می‌شه، و برای هر دارویی که تشخیص داده
// شد یک کارت پیش‌نویس نمایش داده می‌شه. هیچ آلارمی خودکار و بدون تایید نهایی کاربر
// در فرم افزودن دارو ساخته نمی‌شه — چون OCR و تشخیص نام دارو می‌تونن اشتباه کنن.
// ---------------------------------------------------------------------------------
@Composable
fun PrescriptionScanScreen(nav: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var ocrText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var parsedItems by remember { mutableStateOf<List<ParsedPrescriptionItem>>(emptyList()) }
    var cameraTargetUri by remember { mutableStateOf<Uri?>(null) }

    fun runOcr(bitmap: Bitmap) {
        isProcessing = true
        errorMessage = null
        parsedItems = emptyList()
        scope.launch {
            when (val result = PrescriptionOcrEngine.recognize(context, bitmap)) {
                is PrescriptionOcrEngine.OcrResult.Success -> {
                    ocrText = result.text
                }
                is PrescriptionOcrEngine.OcrResult.MissingLanguageData -> {
                    errorMessage = result.message
                }
                is PrescriptionOcrEngine.OcrResult.Error -> {
                    errorMessage = result.message
                }
            }
            isProcessing = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bmp = uriToBitmap(context, uri)
            imageBitmap = bmp
            ocrText = ""
            parsedItems = emptyList()
            if (bmp != null) runOcr(bmp) else errorMessage = "خواندن عکس ممکن نشد."
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = cameraTargetUri
        if (success && uri != null) {
            val bmp = uriToBitmap(context, uri)
            imageBitmap = bmp
            ocrText = ""
            parsedItems = emptyList()
            if (bmp != null) runOcr(bmp) else errorMessage = "خواندن عکس ممکن نشد."
        }
    }

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
                Button(
                    onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.weight(1f)
                ) { Text("🖼 انتخاب از گالری") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val uri = createCameraOutputUri(context)
                        cameraTargetUri = uri
                        cameraLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("📷 گرفتن عکس") }
            }

            imageBitmap?.let { bmp ->
                Spacer(Modifier.height(16.dp))
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "عکس نسخه",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp)
                )
            }

            if (isProcessing) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("در حال خواندن متن نسخه...", fontSize = 13.sp)
                }
            }

            errorMessage?.let { err ->
                Spacer(Modifier.height(16.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("⚠️ خطا", fontWeight = FontWeight.Bold)
                        Text(err, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }

            if (ocrText.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Text("متن خوانده‌شده (در صورت نیاز اصلاح کن)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = ocrText,
                    onValueChange = { ocrText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 5
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { parsedItems = PrescriptionParser.parse(ocrText) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🔍 تحلیل و استخراج داروها") }
            }

            if (parsedItems.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("داروهای پیشنهادی (پیش‌نویس)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                parsedItems.forEach { item ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            if (item.formHint != null) {
                                Text("شکل دارو: ${item.formHint}", fontSize = 12.sp)
                            }
                            if (item.quantity != null) {
                                Text("تعداد تجویزشده: ${item.quantity}", fontSize = 12.sp)
                            }
                            Text(
                                "ساعت‌های پیشنهادی: ${item.suggestedTimesOfDay.joinToString("، ") { com.example.pillreminder.util.TimeParseUtils.formatTime(it) }}",
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
                                    nav.navigate("addPill")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("➕ افزودن به لیست داروها") }
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
            }
        }
    }
}
