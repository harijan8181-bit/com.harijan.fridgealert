package com.yeoncompany.fridgealert

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar

// 매일 알림을 설정한 시각에 맞춰 울리게 하는 알람 예약.
// WorkManager 주기 작업은 절전 때문에 수십 분~몇 시간 밀릴 수 있어서 알람으로 바꿨다.
// "정확한 알람" 권한이 있으면 정각에, 없으면 시스템 판단에 따라 최대 1시간 정도 늦게 울릴 수 있다.
object DailyAlertScheduler {
    private const val PREFS = "fridge_prefs"
    private const val REQUEST_CODE = 4001
    // 예전 버전에서 쓰던 WorkManager 주기 작업 이름 (중복 알림을 막기 위해 취소)
    private const val LEGACY_PERIODIC_WORK = "DailyExpiryCheckWork"

    fun canUseExact(context: Context): Boolean {
        val am = context.getSystemService(AlarmManager::class.java)
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
    }

    fun schedule(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val hour = prefs.getInt("alert_hour", 9)
        val minute = prefs.getInt("alert_minute", 0)

        WorkManager.getInstance(app).cancelUniqueWork(LEGACY_PERIODIC_WORK)

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
        }

        val am = app.getSystemService(AlarmManager::class.java)
        val pi = pendingIntent(app)
        if (canUseExact(app)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target.timeInMillis, pi)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, REQUEST_CODE,
        Intent(context, DailyAlertReceiver::class.java).setAction(DailyAlertReceiver.ACTION_FIRE),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

// 알람이 울리면 알림 작업을 돌리고 다음 날 알람을 다시 건다.
// 재부팅·앱 업데이트·시간대 변경·정확한 알람 권한 변경 때도 알람을 다시 건다 (알람은 재부팅하면 사라짐).
class DailyAlertReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_FIRE = "com.yeoncompany.fridgealert.action.DAILY_ALERT"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_FIRE) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<ExpiryCheckWorker>().build())
        }
        DailyAlertScheduler.schedule(context)
    }
}
