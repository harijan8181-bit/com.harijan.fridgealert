package com.yeoncompany.fridgealert

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// 홈 화면 위젯: 기한이 가까운 식재료 5개를 보여준다
class FridgeWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                render(context, manager, ids)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val MAX_LINES = 5

        // 앱·알림·작업에서 데이터가 바뀌면 호출. 위젯이 없으면 아무 일도 안 한다.
        fun refresh(context: Context) {
            val app = context.applicationContext
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, FridgeWidget::class.java))
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch { render(app, manager, ids) }
        }

        private suspend fun render(context: Context, manager: AppWidgetManager, ids: IntArray) {
            val foods = AppDatabase.getDatabase(context).foodDao().getActiveFoodsOnce()
            val urgent = foods.count { calculateDDay(it.expiryDate) <= 3 }
            val title = if (urgent > 0) "🧊 임박 ${urgent}개 · 보관 ${foods.size}개" else "🧊 보관 ${foods.size}개 · 모두 여유"
            val body = if (foods.isEmpty()) {
                "냉장고가 비어 있어요.\n눌러서 식재료를 등록하세요."
            } else {
                foods.take(MAX_LINES).joinToString("\n") { "${ExpiryCheckWorker.dDayLabel(it.expiryDate).padEnd(5)}  ${it.name}" } +
                        if (foods.size > MAX_LINES) "\n외 ${foods.size - MAX_LINES}개" else ""
            }

            val open = PendingIntent.getActivity(
                context, 3001,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val views = RemoteViews(context.packageName, R.layout.widget_fridge).apply {
                setTextViewText(R.id.widget_title, title)
                setTextViewText(R.id.widget_body, body)
                setOnClickPendingIntent(R.id.widget_root, open)
            }
            ids.forEach { manager.updateAppWidget(it, views) }
        }
    }
}
