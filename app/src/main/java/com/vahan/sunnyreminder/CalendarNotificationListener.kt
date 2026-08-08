package com.vahan.sunnyreminder

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class CalendarNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        Log.d("CalendarListener", "Notification from: $packageName")

        // Look for Google Calendar or System Calendar
        if (packageName == "com.google.android.calendar" || packageName == "com.android.calendar") {
            Log.d("CalendarListener", "Calendar notification detected! Triggering sync...")
            
            val syncRequest = OneTimeWorkRequestBuilder<CalendarSyncWorker>().build()
            WorkManager.getInstance(applicationContext).enqueue(syncRequest)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("CalendarListener", "Service connected")
    }
}
