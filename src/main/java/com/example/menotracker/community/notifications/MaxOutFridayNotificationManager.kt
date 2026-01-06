package com.example.menotracker.community.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.menotracker.MainActivity
import com.example.menotracker.R
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.TemporalAdjusters
import java.util.concurrent.TimeUnit

/**
 * Manages notifications for Max Out Friday challenge.
 */
object MaxOutFridayNotificationManager {

    private const val CHANNEL_ID = "max_out_friday"
    private const val CHANNEL_NAME = "Max Out Friday"
    private const val FRIDAY_REMINDER_WORK = "max_out_friday_reminder"
    private const val FRIDAY_START_WORK = "max_out_friday_start"
    private const val NOTIFICATION_ID_REMINDER = 1001
    private const val NOTIFICATION_ID_START = 1002
    private const val NOTIFICATION_ID_ENDING_SOON = 1003

    /**
     * Creates notification channel (required for Android O+).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for Max Out Friday weekly challenges"
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules weekly notifications for Max Out Friday.
     */
    fun scheduleWeeklyNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)

        // Cancel existing work
        workManager.cancelUniqueWork(FRIDAY_REMINDER_WORK)
        workManager.cancelUniqueWork(FRIDAY_START_WORK)

        // Schedule reminder for Thursday evening (day before)
        val thursdayReminderDelay = calculateDelayUntil(DayOfWeek.THURSDAY, 18, 0) // 6 PM Thursday
        val reminderRequest = OneTimeWorkRequestBuilder<MaxOutFridayReminderWorker>()
            .setInitialDelay(thursdayReminderDelay, TimeUnit.MILLISECONDS)
            .addTag("max_out_friday")
            .build()

        workManager.enqueueUniqueWork(
            FRIDAY_REMINDER_WORK,
            ExistingWorkPolicy.REPLACE,
            reminderRequest
        )

        // Schedule notification for Friday at 6 AM (challenge starts)
        val fridayStartDelay = calculateDelayUntil(DayOfWeek.FRIDAY, 6, 0) // 6 AM Friday
        val startRequest = OneTimeWorkRequestBuilder<MaxOutFridayStartWorker>()
            .setInitialDelay(fridayStartDelay, TimeUnit.MILLISECONDS)
            .addTag("max_out_friday")
            .build()

        workManager.enqueueUniqueWork(
            FRIDAY_START_WORK,
            ExistingWorkPolicy.REPLACE,
            startRequest
        )
    }

    /**
     * Calculates delay in milliseconds until next occurrence of given day/time.
     */
    private fun calculateDelayUntil(dayOfWeek: DayOfWeek, hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.with(TemporalAdjusters.nextOrSame(dayOfWeek))
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        // If today is the target day but time has passed, go to next week
        if (target.isBefore(now) || target.isEqual(now)) {
            target = target.plusWeeks(1)
        }

        return java.time.Duration.between(now, target).toMillis()
    }

    /**
     * Shows a notification.
     */
    fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        deepLink: String = "community/max_out_friday"
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("deep_link", deepLink)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Cancels all Max Out Friday notifications.
     */
    fun cancelAllNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("max_out_friday")

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID_REMINDER)
        notificationManager.cancel(NOTIFICATION_ID_START)
        notificationManager.cancel(NOTIFICATION_ID_ENDING_SOON)
    }
}

/**
 * Worker for Thursday evening reminder.
 */
class MaxOutFridayReminderWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        MaxOutFridayNotificationManager.showNotification(
            context = applicationContext,
            notificationId = 1001,
            title = "Max Out Friday Tomorrow!",
            message = "Get ready to compete! Tomorrow's challenge exercise will be revealed at 6 AM."
        )

        // Schedule next week's reminder
        MaxOutFridayNotificationManager.scheduleWeeklyNotifications(applicationContext)

        return Result.success()
    }
}

/**
 * Worker for Friday morning (challenge starts).
 */
class MaxOutFridayStartWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        MaxOutFridayNotificationManager.showNotification(
            context = applicationContext,
            notificationId = 1002,
            title = "Max Out Friday is LIVE!",
            message = "The challenge has begun! Tap to see this week's exercise and compete for the leaderboard."
        )

        return Result.success()
    }
}