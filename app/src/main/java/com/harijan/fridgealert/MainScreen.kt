package com.harijan.fridgealert

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// 7. 메인 화면 레이아웃
// ==========================================
@Composable
fun MainFridgeScreen(
    viewModel: FridgeViewModel = viewModel(),
    initialOpenStored: Boolean = false,
    onResetInitialOpen: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val activeFoods by viewModel.activeFoods.collectAsState()
    val archivedFoods by viewModel.archivedFoods.collectAsState()
    val shoppingItems by viewModel.shoppingItems.collectAsState()

    var isOcrLoading by remember { mutableStateOf(false) }

    var showSearchDialog by remember { mutableStateOf(false) }
    var showStoredFoodsDialog by remember { mutableStateOf(false) }
    var showArchivedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialOpenStored) {
        if (initialOpenStored) {
            showStoredFoodsDialog = true
            onResetInitialOpen()
        }
    }

    // 알림 권한은 카메라와 별개로 첫 실행 때 요청 (카메라를 안 쓰면 Android 13+에서 알림이 영영 안 오던 문제)
    // 알림이 실제로 켜져 있는지 (휴대폰 설정에서 끄고 돌아올 수도 있으니 화면에 돌아올 때마다 다시 확인)
    var notificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    LifecycleResumeEffect(Unit) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }
    var alertTimeLabel by remember {
        mutableStateOf(String.format(Locale.KOREA, "%d:%02d", viewModel.getAlertHour(), viewModel.getAlertMinute()))
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled() }
    var notificationAsked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (notificationAsked) return@LaunchedEffect
        notificationAsked = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val scannedItems = remember { mutableStateListOf<ParsedItem>() }
    var detectedPurchaseDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showScanConfirmDialog by remember { mutableStateOf(false) }
    var showManualAddDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showRecipeDialog by remember { mutableStateOf(false) }

    var targetFoodForStatus by remember { mutableStateOf<FoodItem?>(null) }
    var editingFoodItem by remember { mutableStateOf<FoodItem?>(null) }
    var pendingDeleteFood by remember { mutableStateOf<FoodItem?>(null) }
    var openingFood by remember { mutableStateOf<FoodItem?>(null) }
    var showShoppingDialog by remember { mutableStateOf(false) }

    var tempCameraImageUri by rememberSaveable { mutableStateOf<Uri?>(null) } // 카메라 앱으로 갔다 오며 화면이 재생성돼도 경로 유지

    // fromFilePath는 EXIF 회전을 반영하고 백그라운드에서 디코딩한다 (세로로 찍은 영수증이 옆으로 누워 인식률이 떨어지던 문제)
    fun runOcrOnUri(uri: Uri, onFinished: () -> Unit = {}) {
        isOcrLoading = true
        coroutineScope.launch {
            val image = try {
                withContext(Dispatchers.IO) { InputImage.fromFilePath(context, uri) }
            } catch (e: Exception) {
                isOcrLoading = false
                onFinished()
                Toast.makeText(context, "이미지 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

            recognizer.process(image)
                .addOnCompleteListener {
                    recognizer.close()
                    onFinished()
                }
                .addOnSuccessListener { visionText ->
                    isOcrLoading = false
                    val parseResult = ReceiptParser.parse(visionText.text)
                    if (parseResult.items.isEmpty()) {
                        Toast.makeText(context, "식재료를 찾지 못했습니다. 직접 등록을 이용해 주세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        detectedPurchaseDate = parseResult.purchaseDateMillis
                        scannedItems.clear()
                        coroutineScope.launch {
                            val updatedList = parseResult.items.map { item ->
                                val custom = viewModel.resolvePreset(item.name)
                                if (custom != null) {
                                    item.copy(shelfDays = custom.shelfDays, storageType = custom.storageType)
                                } else {
                                    item
                                }
                            }
                            scannedItems.addAll(updatedList)
                            showScanConfirmDialog = true
                        }
                    }
                }
                .addOnFailureListener { e ->
                    isOcrLoading = false
                    Toast.makeText(context, "OCR 실패: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    val highResCameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = tempCameraImageUri
        // 촬영한 임시 사진은 인식 후 지워서 캐시에 쌓이지 않게 한다
        val cleanUp = { uri?.let { context.contentResolver.delete(it, null, null) }; tempCameraImageUri = null }
        if (success && uri != null) {
            runOcrOnUri(uri, onFinished = { cleanUp() })
        } else {
            cleanUp()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions[Manifest.permission.CAMERA] == true) {
            try {
                val tempFile = File.createTempFile("receipt_capture_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                tempCameraImageUri = uri
                highResCameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "카메라 초기화 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    fun checkAndLaunchCamera() {
        // 알림 권한은 시작 시 따로 요청하므로 여기선 카메라만 본다 (알림을 거부하면 촬영할 때마다 권한 창이 뜨던 문제)
        val permissions = listOf(Manifest.permission.CAMERA)
        if (permissions.all { ActivityCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            try {
                val tempFile = File.createTempFile("receipt_capture_", ".jpg", context.cacheDir)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempFile)
                tempCameraImageUri = uri
                highResCameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "카메라 준비 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { runOcrOnUri(it) }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let {
            coroutineScope.launch {
                val ok = viewModel.exportToCsv(context, it)
                Toast.makeText(context, if (ok) "CSV 백업 완료!" else "백업 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val importCsvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            coroutineScope.launch {
                val count = viewModel.importFromCsv(context, it)
                if (count >= 0) {
                    Toast.makeText(context, "${count}개 식재료 복원 성공!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "복원 실패", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val urgentFoods = activeFoods.filter { calculateDDay(it.expiryDate) <= 3 }
    val urgentCount = urgentFoods.size
    // 레시피는 아직 먹을 수 있는 임박 재료만 (기한 지난 재료로 요리를 권하지 않도록)
    val recipeFoods = urgentFoods.filter { calculateDDay(it.expiryDate) >= 0 }
    val freshRate = if (activeFoods.isNotEmpty()) {
        ((activeFoods.size - urgentCount).toFloat() / activeFoods.size.toFloat() * 100).toInt()
    } else 100

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        bottomBar = {
            ZeroShiftAdmobContainer()
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = statusBarPadding + 8.dp,
                    bottom = 20.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 상단 헤더
                item {
                    FridgeHeader(
                        onSearchClick = { showSearchDialog = true },
                        onStoredFoodsClick = { showStoredFoodsDialog = true },
                        onArchivedClick = { showArchivedDialog = true },
                        onRecipeClick = { showRecipeDialog = true },
                        onReportClick = { showReportDialog = true },
                        onSettingsClick = { showSettingsDialog = true },
                        onDirectAddClick = { showManualAddDialog = true }
                    )
                }

                // 신선도 게이지 히어로 카드
                item {
                    FridgeHeroCard(
                        freshnessRate = freshRate,
                        urgentCount = urgentCount,
                        notificationsOn = notificationsOn,
                        alertTimeLabel = alertTimeLabel,
                        onNotificationOffClick = {
                            // 이미 한 번 거부하면 권한 창이 다시 안 뜨므로 앱 알림 설정 화면으로 보낸다
                            val intent = Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                            try {
                                context.startActivity(intent)
                            } catch (_: android.content.ActivityNotFoundException) {
                                Toast.makeText(context, "휴대폰 설정 > 앱 > 냉장고 알리미에서 알림을 허용해 주세요.", Toast.LENGTH_LONG).show()
                            }
                        }
                    )
                }

                // 영수증 스캔 퀵 액션
                item {
                    QuickActionSection(
                        onCameraScan = { checkAndLaunchCamera() },
                        onGalleryScan = { galleryLauncher.launch("image/*") },
                        onSampleTest = {
                            val sampleReceipt = """
                                [이마트 역삼점]
                                2026-09-24 18:30:12 POS: 02
                                신선란 30구     6,980원 1개
                                서울우유 1L     2,850원 2개
                                찌개용 두부 300g 1,450원 1개
                                한돈 삼겹살 600g 14,500원 1개
                                비비고 왕교자 만두 9,200원 1개
                                신라면 멀티팩   4,300원 1개
                                사과 1봉        8,900원 1개
                                합계금액: 48,180원
                            """.trimIndent()
                            val parseResult = ReceiptParser.parse(sampleReceipt)
                            detectedPurchaseDate = parseResult.purchaseDateMillis
                            scannedItems.clear()
                            scannedItems.addAll(parseResult.items)
                            showScanConfirmDialog = true
                        }
                    )
                }

                // 요약 통계 카드
                item {
                    FridgeStatGrid(
                        activeCount = activeFoods.size,
                        urgentCount = urgentCount,
                        shoppingCount = shoppingItems.count { !it.checked },
                        onActiveClick = { showStoredFoodsDialog = true },
                        onUrgentClick = { showStoredFoodsDialog = true },
                        onShoppingClick = { showShoppingDialog = true }
                    )
                }

                // 냉장고 파먹기 배너
                if (recipeFoods.isNotEmpty()) {
                    item {
                        RecipeBanner(urgentFoods = recipeFoods, onClick = { showRecipeDialog = true })
                    }
                }

                // 메인 화면 임박 품목 섹션
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🚨 소비 임박 식품 (3일 이내)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "내용물 전체보기 🧊",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { showStoredFoodsDialog = true }
                        )
                    }
                }

                if (urgentFoods.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(22.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "✨ 임박한 식재료가 없습니다!\n모든 품목은 위의 '내용물' 버튼에서 확인하세요.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                } else {
                    items(urgentFoods, key = { it.id }) { food ->
                        FoodItemCard(
                            food = food,
                            onDecreaseQty = {
                                viewModel.decreaseQuantity(food, onZeroReached = {
                                    targetFoodForStatus = food
                                })
                            },
                            onIncreaseQty = { viewModel.increaseQuantity(food) },
                            onOpenStatusModal = { targetFoodForStatus = food },
                            onEdit = { editingFoodItem = food },
                            onDelete = { pendingDeleteFood = food },
                            onOpen = { openingFood = food }
                        )
                    }
                }
            }

            if (isOcrLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("영수증 원본 해상도 1초 분석 중...", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    // 🔍 [넓이 제한 해제] 검색 팝업
    if (showSearchDialog) {
        SearchFoodsDialog(
            allFoods = activeFoods,
            onDismiss = { showSearchDialog = false },
            onDecreaseQty = { food ->
                viewModel.decreaseQuantity(food, onZeroReached = { targetFoodForStatus = food })
            },
            onIncreaseQty = { food -> viewModel.increaseQuantity(food) },
            onOpenStatusModal = { food -> targetFoodForStatus = food },
            onEdit = { food -> editingFoodItem = food },
            onDelete = { food -> pendingDeleteFood = food },
            onOpen = { food -> openingFood = food }
        )
    }

    // 🧊 [넓이 제한 해제 & 상세 정보] 품목별 구분 내용물 팝업
    if (showStoredFoodsDialog) {
        StoredFoodsCategorizedDialog(
            activeFoods = activeFoods,
            onDismiss = { showStoredFoodsDialog = false },
            onDecreaseQty = { food ->
                viewModel.decreaseQuantity(food, onZeroReached = { targetFoodForStatus = food })
            },
            onIncreaseQty = { food -> viewModel.increaseQuantity(food) },
            onOpenStatusModal = { food -> targetFoodForStatus = food },
            onEdit = { food -> editingFoodItem = food },
            onDelete = { food -> pendingDeleteFood = food },
            onOpen = { food -> openingFood = food }
        )
    }

    // 📜 [넓이 제한 해제] 기록 팝업
    if (showArchivedDialog) {
        ArchivedRecordsDialog(
            archivedFoods = archivedFoods,
            onDismiss = { showArchivedDialog = false },
            onDelete = { food -> pendingDeleteFood = food }
        )
    }

    // [넓이 제한 해제] 스캔 확인 팝업
    if (showScanConfirmDialog) {
        ScanConfirmDialog(
            items = scannedItems,
            purchaseDateMillis = detectedPurchaseDate,
            onDismiss = { showScanConfirmDialog = false },
            onConfirmAdd = { selectedItems, baseDate ->
                val foodEntities = selectedItems.map { item ->
                    val expiryMillis = calculateExpiryTimestamp(baseDate, item.shelfDays)
                    FoodItem(
                        name = item.name,
                        category = item.category,
                        storageType = item.storageType,
                        quantity = item.quantity,
                        purchaseDate = baseDate,
                        expiryDate = expiryMillis,
                        status = "ACTIVE",
                        price = item.price
                    )
                }
                viewModel.addFoodItems(foodEntities)
                showScanConfirmDialog = false
                Toast.makeText(context, "${foodEntities.size}개 품목이 냉장고에 등록되었습니다!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 수동 등록 팝업
    if (showManualAddDialog) {
        ManualAddDialog(
            resolvePreset = { viewModel.resolvePreset(it) },
            lookupBarcode = { viewModel.lookupBarcode(it) },
            onDismiss = { showManualAddDialog = false },
            onAdd = { entity, barcode ->
                viewModel.addFoodItems(listOf(entity))
                // 바코드는 사용자가 확정한 이름·보관 정보로 기억해 두고 다음부터 바로 채운다
                if (barcode != null) {
                    viewModel.rememberBarcode(
                        BarcodeProduct(
                            barcode = barcode,
                            name = entity.name,
                            category = entity.category,
                            storageType = entity.storageType,
                            shelfDays = calculateDDay(entity.expiryDate).toInt().coerceAtLeast(1)
                        )
                    )
                }
                showManualAddDialog = false
            }
        )
    }

    // 수정 팝업
    editingFoodItem?.let { target ->
        EditFoodItemDialog(
            item = target,
            onDismiss = { editingFoodItem = null },
            onSave = { updated ->
                viewModel.updateFoodItem(updated)
                editingFoodItem = null
            }
        )
    }

    // 소비 vs 폐기 선택 팝업
    targetFoodForStatus?.let { target ->
        ConsumeOrWasteDialog(
            item = target,
            onDismiss = { targetFoodForStatus = null },
            onSelect = { status, addToShopping ->
                viewModel.markFoodStatus(target, status, addToShopping)
                targetFoodForStatus = null
            }
        )
    }

    // 개봉 팝업
    openingFood?.let { target ->
        OpenFoodDialog(
            item = target,
            onDismiss = { openingFood = null },
            onConfirm = { days ->
                viewModel.openFood(target, days)
                openingFood = null
            }
        )
    }

    // 장보기 목록 팝업
    if (showShoppingDialog) {
        ShoppingListDialog(
            items = shoppingItems,
            onDismiss = { showShoppingDialog = false },
            onAdd = { viewModel.addShopping(it) },
            onToggle = { viewModel.toggleShopping(it) },
            onDelete = { viewModel.deleteShopping(it) },
            onClearChecked = { viewModel.clearCheckedShopping() }
        )
    }

    // 삭제 확인 팝업 (검색·내용물·기록 팝업보다 뒤에 그려야 그 위에 뜬다)
    pendingDeleteFood?.let { target ->
        DeleteConfirmDialog(
            item = target,
            onDismiss = { pendingDeleteFood = null },
            onConfirm = {
                viewModel.deleteItem(target)
                pendingDeleteFood = null
            }
        )
    }

    // 레시피 추천 팝업
    if (showRecipeDialog) {
        RecipeRecommendationDialog(
            urgentFoods = recipeFoods,
            onDismiss = { showRecipeDialog = false },
            onSearch = { query ->
                val encoded = URLEncoder.encode(query, "UTF-8")
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://m.10000recipe.com/recipe/list.html?q=$encoded"))
                try {
                    context.startActivity(intent)
                } catch (_: android.content.ActivityNotFoundException) {
                    Toast.makeText(context, "웹 브라우저를 열 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
                showRecipeDialog = false
            }
        )
    }

    // 절약 리포트 팝업
    if (showReportDialog) {
        SavingsReportDialog(archivedFoods = archivedFoods, onDismiss = { showReportDialog = false })
    }

    // 설정 팝업
    if (showSettingsDialog) {
        SettingsDialog(
            currentHour = viewModel.getAlertHour(),
            currentMinute = viewModel.getAlertMinute(),
            onDismiss = { showSettingsDialog = false },
            onSaveAlertTime = { h, m ->
                viewModel.setAlertTime(h, m)
                alertTimeLabel = String.format(Locale.KOREA, "%d:%02d", h, m)
            },
            onTestNotification = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(context, "알림 권한이 꺼져 있습니다. 휴대폰 설정 > 앱 > 냉장고 알리미에서 허용해 주세요.", Toast.LENGTH_LONG).show()
                } else {
                    viewModel.triggerTestNotification(context)
                }
            },
            onExportCsv = { exportCsvLauncher.launch("fridge_backup_${SimpleDateFormat("yyyyMMdd", Locale.KOREA).format(Date())}.csv") },
            onImportCsv = { importCsvLauncher.launch(arrayOf("text/*", "application/vnd.ms-excel", "*/*")) }
        )
    }
}
