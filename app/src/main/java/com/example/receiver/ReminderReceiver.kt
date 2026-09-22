package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.alarm.AlarmScheduler
import com.example.data.ReminderDatabase
import com.example.model.ReminderItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "smart_reminders_channel"
        const val CHANNEL_NAME = "Smart Reminders"
        const val ACTION_TRIGGER_REMINDER = "com.example.smartreminder.ACTION_TRIGGER_REMINDER"
        const val ACTION_COMPLETE_REMINDER = "com.example.smartreminder.ACTION_COMPLETE_REMINDER"
        const val ACTION_SNOOZE_REMINDER = "com.example.smartreminder.ACTION_SNOOZE_REMINDER"

        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_CATEGORY = "extra_reminder_category"
        const val EXTRA_REMINDER_PRIORITY = "extra_reminder_priority"
        const val EXTRA_REMINDER_DESC = "extra_reminder_desc"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE) ?: "Smart Reminder"
        val category = intent.getStringExtra(EXTRA_REMINDER_CATEGORY) ?: "GENERAL"
        val priority = intent.getStringExtra(EXTRA_REMINDER_PRIORITY) ?: "MEDIUM"
        val desc = intent.getStringExtra(EXTRA_REMINDER_DESC) ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_TRIGGER_REMINDER -> {
                showNotification(context, notificationManager, reminderId, title, category, priority, desc)
            }
            ACTION_COMPLETE_REMINDER -> {
                notificationManager.cancel(reminderId.toInt())
                if (reminderId > 0) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = ReminderDatabase.getDatabase(context)
                            db.reminderDao().setCompleted(reminderId, true)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_SNOOZE_REMINDER -> {
                notificationManager.cancel(reminderId.toInt())
                if (reminderId > 0) {
                    val snoozeTime = System.currentTimeMillis() + (10 * 60 * 1000) // 10 minutes
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = ReminderDatabase.getDatabase(context)
                            db.reminderDao().snoozeReminder(reminderId, snoozeTime)
                            val reminder = db.reminderDao().getReminderById(reminderId)
                            if (reminder != null) {
                                AlarmScheduler(context).schedule(reminder)
                            }
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    private fun showNotification(
        context: Context,
        notificationManager: NotificationManager,
        reminderId: Long,
        title: String,
        category: String,
        priority: String,
        desc: String
    ) {
        createNotificationChannel(notificationManager)

        // Open app intent
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Complete action
        val completeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_COMPLETE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 1).toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Snooze action
        val snoozeIntent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_REMINDER
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            (reminderId * 10 + 2).toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val contentBody = if (desc.isNotBlank()) desc else "Category: $category • Priority: $priority"

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("⏰ $title")
            .setContentText(contentBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentBody))
            .setPriority(
                if (priority == "URGENT" || priority == "HIGH")
                    NotificationCompat.PRIORITY_MAX
                else
                    NotificationCompat.PRIORITY_HIGH
            )
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setAutoCancel(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.checkbox_on_background, "Mark Done", completePendingIntent)
            .addAction(android.R.drawable.ic_lock_idle_alarm, "Snooze 10m", snoozePendingIntent)

        notificationManager.notify(reminderId.toInt(), notificationBuilder.build())
    }

    private fun createNotificationChannel(notificationManager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications and alarms for scheduled reminders"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
