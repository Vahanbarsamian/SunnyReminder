package com.vahan.sunnyreminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class CalendarSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("CalendarSyncWorker", "Starting sync...")
        
        val permissionManager = PermissionManager(applicationContext)
        if (!permissionManager.hasCalendarPermission()) {
            Log.e("CalendarSyncWorker", "Missing calendar permission")
            return Result.failure()
        }

        val repository = CalendarRepository(applicationContext)
        val scheduler = AlarmScheduler(applicationContext)
        
        val events = repository.getUpcomingEvents()
        Log.d("CalendarSyncWorker", "Found ${events.size} events to sync")

        val now = System.currentTimeMillis()
        for (event in events) {
            // Only schedule if it's in the future
            if (event.startTime > now) {
                scheduler.schedule(event.startTime, event.title)
            }
        }

        return Result.success()
    }
}
