package com.vahan.sunnyreminder

import android.app.AlertDialog
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.vahan.sunnyreminder.ui.BeachScene
import com.vahan.sunnyreminder.ui.theme.SunnyReminderTheme

class ReminderActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock screen support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val reminderText = intent.getStringExtra("reminder_text") ?: "Pause ensoleillée !"

        // Fetch all events for the next hour to create the "Parade"
        val repository = CalendarRepository(this)
        val allEvents = repository.getUpcomingEvents(hoursAhead = 1)
        
        // If we came from a specific alarm but sync hasn't run, 
        // ensure at least that specific event is in the list
        val displayEvents = if (allEvents.isEmpty()) {
            listOf(CalendarEvent(reminderText, System.currentTimeMillis()))
        } else {
            allEvents
        }

        startSound()

        setContent {
            SunnyReminderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BeachScene(
                        events = displayEvents,
                        onSunClick = { 
                            finish() 
                        },
                        onTowelClick = {
                            openCalendar()
                        },
                        onVendorClick = {
                            val sm = SettingsManager(this)
                            val scheduler = AlarmScheduler(this)
                            val snoozeTime = System.currentTimeMillis() + sm.getSnoozeDuration() * 60 * 1000
                            // Postpone for all events in the list (or just the first one)
                            displayEvents.forEach { event ->
                                scheduler.schedule(snoozeTime, event.title)
                            }
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun openCalendar() {
        val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
            data = android.provider.CalendarContract.Events.CONTENT_URI
            putExtra(android.provider.CalendarContract.Events.TITLE, "Nouveau rendez-vous")
            putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, System.currentTimeMillis())
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startSound() {
        // Note: You need to add sea_waves.mp3 in res/raw
        try {
            val resId = resources.getIdentifier("sea_waves", "raw", packageName)
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(this, resId).apply {
                    isLooping = true
                    start()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showIceCreamDialog() {
        AlertDialog.Builder(this)
            .setTitle("Vendeur de glaces")
            .setMessage("Voulez-vous un rappel pour une autre glace dans 1 minute ?")
            .setPositiveButton("Oh oui !") { _, _ ->
                val scheduler = AlarmScheduler(this)
                val time = System.currentTimeMillis() + 60 * 1000
                scheduler.schedule(time, "C'est l'heure de votre glace !")
                finish()
            }
            .setNegativeButton("Plus tard") { _, _ -> }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
