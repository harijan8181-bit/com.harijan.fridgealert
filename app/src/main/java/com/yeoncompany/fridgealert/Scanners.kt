package com.yeoncompany.fridgealert

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

// 포장지의 소비기한을 찍어서 날짜를 읽는다.
// 반환값: 누르면 촬영을 시작하는 함수. 결과는 onResult(소비기한 millis 또는 null), 진행 중엔 onBusy(true)
@Composable
fun rememberExpiryDateScanner(onBusy: (Boolean) -> Unit, onResult: (Long?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = photoUri
        photoUri = null
        if (!success || uri == null) {
            uri?.let { context.contentResolver.delete(it, null, null) }
            return@rememberLauncherForActivityResult
        }
        onBusy(true)
        scope.launch {
            val text = try {
                recognizeText(context, uri)
            } catch (_: Exception) {
                null
            } finally {
                context.contentResolver.delete(uri, null, null)
            }
            onBusy(false)
            val date = text?.let { ExpiryDateParser.parse(it) }
            if (date == null) {
                Toast.makeText(context, "날짜를 찾지 못했어요. 소비기한 글자가 크게 나오게 다시 찍어 주세요.", Toast.LENGTH_LONG).show()
            }
            onResult(date)
        }
    }

    fun launchCamera() {
        try {
            val file = File.createTempFile("label_capture_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            photoUri = uri
            takePicture.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "카메라 준비 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    return {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            permission.launch(Manifest.permission.CAMERA)
        }
    }
}

private suspend fun recognizeText(context: Context, uri: Uri): String {
    val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(context, uri) }
    val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    return try {
        kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { cont.resume(it.text) { _, _, _ -> } }
                .addOnFailureListener { cont.resume("") { _, _, _ -> } }
        }
    } finally {
        recognizer.close()
    }
}

// 구글 Play 서비스의 바코드 스캔 화면을 띄운다 (카메라 권한 불필요)
fun scanBarcode(context: Context, onResult: (String) -> Unit) {
    val options = GmsBarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
        .build()
    GmsBarcodeScanning.getClient(context, options).startScan()
        .addOnSuccessListener { barcode -> barcode.rawValue?.let(onResult) }
        .addOnFailureListener {
            Toast.makeText(context, "바코드 스캐너를 열 수 없습니다. 잠시 후 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
        }
}
