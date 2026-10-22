package com.harijan.fridgealert

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.google.android.gms.ads.MobileAds

// ==========================================
// 5. 메인 액티비티
// ==========================================
class MainActivity : ComponentActivity() {
    private var openStoredOnStart = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // 화면 회전 등 재생성 때는 같은 인텐트가 다시 들어오므로 최초 실행일 때만 반영
        if (savedInstanceState == null) {
            openStoredOnStart.value = intent.getBooleanExtra("open_stored_dialog", false)
        }

        createNotificationChannel(this)
        MobileAds.initialize(this) {}

        setContent {
            PremiumAdaptiveTheme {
                MainFridgeScreen(
                    initialOpenStored = openStoredOnStart.value,
                    onResetInitialOpen = { openStoredOnStart.value = false }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("open_stored_dialog", false)) {
            openStoredOnStart.value = true
        }
    }
}

// ==========================================
// 6. 반응형 테마
// ==========================================
@Composable
fun PremiumAdaptiveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            surface = Color(0xFF090D16),
            onSurface = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFF94A3B8),
            surfaceContainer = Color(0xFF131B2E),
            surfaceContainerHigh = Color(0xFF1E293B),
            primary = Color(0xFF6366F1),
            outlineVariant = Color(0xFF1E293B)
        )
    } else {
        lightColorScheme(
            surface = Color(0xFFF8FAFC),
            onSurface = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF64748B),
            surfaceContainer = Color(0xFFFFFFFF),
            surfaceContainerHigh = Color(0xFFF1F5F9),
            primary = Color(0xFF4F46E5),
            outlineVariant = Color(0xFFE2E8F0)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
