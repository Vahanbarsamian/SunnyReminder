package com.vahan.sunnyreminder

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.ui.graphics.toArgb

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("sunny_settings", Context.MODE_PRIVATE)

    fun saveNotificationSound(uri: String) {
        prefs.edit().putString("sound_uri", uri).apply()
    }

    fun getNotificationSound(): String {
        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString()
        return prefs.getString("sound_uri", defaultSound) ?: defaultSound
    }

    fun saveLedColor(color: Int) {
        prefs.edit().putInt("led_color", color).apply()
    }

    fun getLedColor(): Int {
        // Default to Sunny Yellow
        return prefs.getInt("led_color", android.graphics.Color.YELLOW)
    }
    
    fun getSettingsHash(): Int {
        return (getNotificationSound() + getLedColor().toString() + getSnoozeDuration().toString()).hashCode()
    }

    fun saveSnoozeDuration(minutes: Int) {
        prefs.edit().putInt("snooze_duration", minutes).apply()
    }

    fun getSnoozeDuration(): Int {
        return prefs.getInt("snooze_duration", 10) // Default 10 min
    }

    fun setHideMegaphone(hide: Boolean) {
        prefs.edit().putBoolean("hide_megaphone", hide).apply()
    }

    fun shouldHideMegaphone(): Boolean {
        return prefs.getBoolean("hide_megaphone", false)
    }
}
