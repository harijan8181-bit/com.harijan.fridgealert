package com.yeoncompany.fridgealert

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ==========================================
// 2. WorkManager 알림 스케줄러
// ==========================================
const val NOTIFICATION_CHANNEL_ID = "fridge_expiry_channel"
private const val GROUP_KEY = "fridge_expiry_group"
private const val SUMMARY_ID = 1001
private const val ITEM_ID_BASE = 20_000
// 품목별 알림은 이 개수까지만 (나머지는 요약 알림에 포함)
private const val MAX_ITEM_NOTIFICATIONS = 5

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "냉장고 소비기한 알림"
        val descriptionText = "소비기한 임박 및 당일 만료 식품을 안내합니다."
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

class ExpiryCheckWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            FridgeWidget.refresh(context) // 날짜가 바뀌었으니 위젯의 D-day도 갱신
            val now = System.currentTimeMillis()
            val foods = AppDatabase.getDatabase(context).foodDao().getActiveFoodsOnce()
                .filter { it.notifyEnabled && (it.snoozedUntil == null || it.snoozedUntil < now) }

            // D-1·당일·만료는 품목마다 따로 알리고 바로 처리할 버튼을 단다
            val critical = foods.filter { calculateDDay(it.expiryDate) <= 1 }
            // D-2~3은 한 줄 요약
            val soon = foods.filter { calculateDDay(it.expiryDate) in 2..3 }

            val individual = critical.take(MAX_ITEM_NOTIFICATIONS)
            individual.forEach { sendItemNotification(context, it) }

            val rest = critical.drop(MAX_ITEM_NOTIFICATIONS) + soon
            if (rest.isNotEmpty() || individual.size > 1) {
                val message = if (rest.isEmpty()) {
                    "오늘 정리할 식재료 ${individual.size}개"
                } else {
                    "⚠️ 곧 만료: " + rest.take(3).joinToString(", ") { "${it.name}(${dDayLabel(it.expiryDate)})" } +
                            if (rest.size > 3) " 외 ${rest.size - 3}건" else ""
                }
                sendNotification(context, "냉장고 소비기한 알림", message, asGroupSummary = individual.isNotEmpty())
            } else if (individual.isEmpty() && inputData.getBoolean(KEY_TEST, false)) {
                sendNotification(context, "소비기한 알리미 테스트", "곧 만료되는 식재료가 없어요. (보관 중 ${foods.size}개)")
            }
            Result.success()
        } catch (_: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_TEST = "test"

        private fun canNotify(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

        private fun openAppIntent(context: Context): PendingIntent {
            val notifyIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("open_stored_dialog", true)
            }
            return PendingIntent.getActivity(
                context, SUMMARY_ID, notifyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun dDayLabel(expiry: Long): String {
            val d = calculateDDay(expiry)
            return when {
                d < 0 -> "D+${-d}"
                d == 0L -> "오늘"
                else -> "D-$d"
            }
        }

        fun itemNotificationId(foodId: Long) = ITEM_ID_BASE + (foodId % 100_000).toInt()

        fun sendNotification(context: Context, title: String, content: String, asGroupSummary: Boolean = false) {
            if (!canNotify(context)) return
            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openAppIntent(context))
                .setAutoCancel(true)
            if (asGroupSummary) builder.setGroup(GROUP_KEY).setGroupSummary(true)
            NotificationManagerCompat.from(context).notify(SUMMARY_ID, builder.build())
        }

        fun sendItemNotification(context: Context, food: FoodItem) {
            if (!canNotify(context)) return
            val d = calculateDDay(food.expiryDate)
            val title = when {
                d < 0 -> "🚨 ${food.name} 기한이 ${-d}일 지났어요"
                d == 0L -> "⏰ ${food.name} 오늘까지예요"
                else -> "⚠️ ${food.name} 내일까지예요"
            }
            val text = "${food.storageType} · ${food.quantity}개 · 소비기한 ${formatDate(food.expiryDate)}"

            fun action(label: String, action: String, slot: Int) = NotificationCompat.Action(
                0, label,
                PendingIntent.getBroadcast(
                    context,
                    // 품목·버튼마다 다른 요청 코드여야 서로 덮어쓰지 않는다
                    ((food.id % 100_000) * 10 + slot).toInt(),
                    Intent(context, NotificationActionReceiver::class.java)
                        .setAction(action)
                        .putExtra(NotificationActionReceiver.EXTRA_FOOD_ID, food.id),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )

            val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openAppIntent(context))
                .setAutoCancel(true)
                .setGroup(GROUP_KEY)
                .addAction(action("먹었어요 😋", NotificationActionReceiver.ACTION_CONSUMED, 0))
                .addAction(action("버렸어요", NotificationActionReceiver.ACTION_WASTED, 1))
                .addAction(action("내일 다시", NotificationActionReceiver.ACTION_SNOOZE, 2))
            NotificationManagerCompat.from(context).notify(itemNotificationId(food.id), builder.build())
        }
    }
}

// 알림의 버튼을 누르면 앱을 열지 않고 바로 처리
class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CONSUMED = "com.yeoncompany.fridgealert.action.CONSUMED"
        const val ACTION_WASTED = "com.yeoncompany.fridgealert.action.WASTED"
        const val ACTION_SNOOZE = "com.yeoncompany.fridgealert.action.SNOOZE"
        const val EXTRA_FOOD_ID = "food_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val foodId = intent.getLongExtra(EXTRA_FOOD_ID, -1)
        if (foodId < 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getDatabase(context).foodDao()
                val now = System.currentTimeMillis()
                when (intent.action) {
                    ACTION_CONSUMED -> dao.setStatus(foodId, "CONSUMED", now)
                    ACTION_WASTED -> dao.setStatus(foodId, "WASTED", now)
                    // 다음 날 알림 시각 전에 풀리도록 20시간 뒤까지 쉬기
                    ACTION_SNOOZE -> dao.setSnooze(foodId, now + 20L * 60 * 60 * 1000)
                }
                NotificationManagerCompat.from(context).cancel(ExpiryCheckWorker.itemNotificationId(foodId))
                FridgeWidget.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }
}
