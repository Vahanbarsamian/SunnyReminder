package com.vahan.sunnyreminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderText = intent.getStringExtra("reminder_text") ?: "Il est temps !"
        
        // WakeLock to ensure CPU stays awake during transition to Activity
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SunnyReminder:AlarmWakeLock"
        )
        wakeLock.acquire(10 * 1000L) // 10 seconds timeout

        showNotification(context, reminderText)
    }

    private fun showNotification(context: Context, reminderText: String) {
        val settingsManager = SettingsManager(context)
        val soundUriStr = settingsManager.getNotificationSound()
        val ledColor = settingsManager.getLedColor()
        
        // Dynamic channel ID to allow sound/light changes without re-installing
        val channelId = "sunny_channel_${settingsManager.getSettingsHash()}"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete old channels to keep it clean (optional but good practice)
            // notificationManager.notificationChannels.forEach { notificationManager.deleteNotificationChannel(it.id) }
            
            val channel = NotificationChannel(
                channelId,
                "Sunny Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders with custom sound and LED"
                enableLights(true)
                lightColor = ledColor
                
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .build()
                setSound(soundUriStr.toUri(), audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val fullScreenIntent = Intent(context, ReminderActivity::class.java).apply {
            putExtra("reminder_text", reminderText)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Sunny Reminder")
            .setContentText(reminderText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setLights(ledColor, 1000, 1000)
            .setSound(soundUriStr.toUri())
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
