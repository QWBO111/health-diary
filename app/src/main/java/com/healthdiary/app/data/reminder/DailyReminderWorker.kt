package com.healthdiary.app.data.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthdiary.app.HealthDiaryApp
import com.healthdiary.app.MainActivity
import com.healthdiary.app.R
import com.healthdiary.app.util.Dates

class DailyReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as HealthDiaryApp
        val today = Dates.today()
        val hasDiary = app.container.diaryRepository.hasEntry(today)
        val hasWorkout = app.container.workoutRepository.latestSessionOn(today) != null
        val hasWeight = app.container.bodyRepository.getMetricByDate(today)?.weightKg != null
        if (!hasDiary || !hasWorkout || !hasWeight) {
            showReminderNotification()
        }
        return Result.success()
    }

    private fun showReminderNotification() {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            applicationContext.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = applicationContext.getString(R.string.notification_channel_desc)
        }
        manager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setContentTitle("今天的记录还没完成哦")
            .setContentText("记得记录训练、体重和心情日记")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(1001, notification)
    }

    companion object {
        private const val CHANNEL_ID = "daily_reminder"
    }
}
