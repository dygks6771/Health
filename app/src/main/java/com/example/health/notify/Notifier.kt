package com.example.health.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.health.MainActivity
import com.example.health.R
import com.example.health.game.RunReward
import com.example.health.game.WeeklyInfo
import com.example.health.net.Session
import java.time.LocalDate
import java.time.LocalTime

/**
 * 백그라운드 동기화 결과 알림: 레벨 업 · 주간 원정 목표 달성 · 주말 저녁 목표 리마인더.
 * 같은 알림은 주/날마다 한 번만 보낸다.
 */
object Notifier {
    private const val CHANNEL_ID = "progress"
    private const val ID_LEVEL_UP = 1
    private const val ID_WEEKLY = 2

    /** 주말 리마인더: 이번 주 남은 날이 이 이하이고 저녁 이후일 때 */
    private const val REMINDER_DAYS_LEFT = 2
    private val REMINDER_AFTER: LocalTime = LocalTime.of(18, 0)

    fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** 동기화 결과와 이번 주 원정 상태로 알림 결정 */
    fun afterSync(context: Context, session: Session, reward: RunReward?, weekly: WeeklyInfo?) {
        if (!session.notificationsEnabled || !canNotify(context)) return
        if (reward != null && reward.leveledUp) {
            show(context, ID_LEVEL_UP, "🎉 레벨 업! Lv.${reward.levelAfter}", "러닝 %.1fkm 로 영웅이 강해졌어요. 능력치 포인트를 찍어 보세요!".format(reward.countedKm))
        }
        if (weekly == null || weekly.claimed) return
        if (weekly.achieved) {
            if (session.weeklyAchievedNotifiedFor != weekly.weekStart) {
                session.weeklyAchievedNotifiedFor = weekly.weekStart
                show(context, ID_WEEKLY, "🏆 이번 주 원정 목표 달성!", "%.1fkm 완주! 앱에서 보상 상자를 열어 보세요.".format(weekly.progressKm))
            }
            return
        }
        val today = LocalDate.now().toString()
        if (weekly.daysLeft <= REMINDER_DAYS_LEFT &&
            LocalTime.now().isAfter(REMINDER_AFTER) &&
            session.weeklyReminderNotifiedOn != today
        ) {
            session.weeklyReminderNotifiedOn = today
            val streak = if (weekly.streakBefore > 0) " ${weekly.streakBefore}주 연속 기록을 이어가요!" else ""
            show(
                context,
                ID_WEEKLY,
                "🏃 원정 목표까지 %.1fkm".format(weekly.remainingKm),
                "이번 주가 ${weekly.daysLeft}일 남았어요.$streak",
            )
        }
    }

    private fun show(context: Context, id: Int, title: String, text: String) {
        ensureChannel(context)
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_flag)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            // 알림 권한이 방금 꺼진 경우: 무시
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "원정 소식", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "레벨 업, 주간 원정 목표 달성과 리마인더"
            },
        )
    }
}
