package com.hs.touristguide.ml

import android.content.Context
import android.util.Log
import com.hs.touristguide.notifications.SmartNotificationManager
import com.hs.touristguide.weather.WeatherData

class WeatherMLAnalyzer(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("ml_prefs", Context.MODE_PRIVATE)
    private val notificationManager = SmartNotificationManager(context.applicationContext)

    // Analyze weather and return activity suitability scores
    fun analyzeAndNotify(weather: WeatherData) {
        val scores = analyzeWeatherSuitability(weather)
        Log.d("MLDebug", "Weather scores: $scores")

        if (shouldSendNotification()) {
            val activityType = scores.maxByOrNull { it.value }?.key ?: "outdoor"
            val score = scores[activityType] ?: 0f

            notificationManager.sendWeatherRecommendation(activityType, weather, score)
            markNotificationSent()
        } else {
            Log.d("MLDebug", "Skipping notification: sent recently")
        }
    }

    fun analyzeWeatherSuitability(weather: WeatherData): Map<String, Float> {
        val scores = mutableMapOf<String, Float>()
        val condition = weather.condition.lowercase()

        // Example scoring (simplified)
        scores["outdoor"] = if ("clear" in condition) 0.9f else 0.4f
        scores["beach"] = if ("sunny" in condition && weather.temperature > 25f) 0.8f else 0.3f
        scores["cultural"] = if ("rain" in condition) 0.9f else 0.5f
        scores["adventure"] = if ("clear" in condition && weather.temperature in 15f..30f) 0.7f else 0.3f

        return scores.mapValues { it.value.coerceIn(0f, 1f) }
    }

    fun shouldSendNotification(): Boolean {
        val lastNotificationTime = sharedPrefs.getLong("last_notification_time", 0)
        val currentTime = System.currentTimeMillis()
        val hoursSinceLast = (currentTime - lastNotificationTime) / (1000 * 60 * 60)
        return hoursSinceLast >= 3
    }

    fun markNotificationSent() {
        sharedPrefs.edit().putLong("last_notification_time", System.currentTimeMillis()).apply()
    }
}
