package com.vahan.sunnyreminder

import android.content.Context
import androidx.compose.runtime.*
import java.util.Calendar

enum class WeatherState {
    SUNNY, CLOUDY, RAINY, STORM
}

class WeatherManager(private val context: Context) {
    
    companion object {
        var currentWeather by mutableStateOf(WeatherState.SUNNY)
        var isManualMode by mutableStateOf(false)
        var temperature by mutableIntStateOf(28)
        var windSpeed by mutableIntStateOf(12)
    }

    fun setWeather(state: WeatherState, manual: Boolean = true) {
        currentWeather = state
        isManualMode = manual
        // Adjust temp/wind based on state for realism
        when(state) {
            WeatherState.SUNNY -> { temperature = 30; windSpeed = 5 }
            WeatherState.CLOUDY -> { temperature = 24; windSpeed = 15 }
            WeatherState.RAINY -> { temperature = 20; windSpeed = 25 }
            WeatherState.STORM -> { temperature = 18; windSpeed = 45 }
        }
    }

    // In a real app, this would use FusedLocationProvider and an API call
    fun updateWeatherAuto() {
        if (isManualMode) return
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // Dummy logic: sunny day, cloudy night
        currentWeather = if (hour in 7..19) WeatherState.SUNNY else WeatherState.CLOUDY
    }
}
