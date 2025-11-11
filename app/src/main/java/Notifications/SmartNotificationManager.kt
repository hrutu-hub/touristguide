package com.hs.touristguide.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.hs.touristguide.MainActivity
import com.hs.touristguide.R
import com.hs.touristguide.weather.WeatherData

class SmartNotificationManager(private val context: Context) {

    companion object {
        const val WEATHER_CHANNEL_ID = "weather_recommendations"
        const val WEATHER_NOTIFICATION_ID = 2001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                WEATHER_CHANNEL_ID,
                "Weather-based Recommendations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Smart recommendations based on current weather conditions"
                enableLights(true)
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
            Log.d("NotifDebug", "Notification channel created")
        }
    }

    fun sendWeatherRecommendation(activityType: String, weather: WeatherData, score: Float) {
        val (title, message, emoji) = generateContent(activityType, weather, score)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, WEATHER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // make sure this icon exists
            .setContentTitle("$emoji $title")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = NotificationManagerCompat.from(context)

        // Android 13+ permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("NotifDebug", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        manager.notify(WEATHER_NOTIFICATION_ID, notification)
        Log.d("NotifDebug", "Notification sent: $title")
    }

    private fun generateContent(
        activityType: String,
        weather: WeatherData,
        score: Float
    ): Triple<String, String, String> {
        val temp = weather.temperature.toInt()
        val condition = weather.condition.lowercase()

        return when (activityType) {
            "outdoor" -> Triple(
                "Perfect for Exploring Outdoors!",
                "It's $temp°C and $condition — great time to go out! (Confidence ${(score * 100).toInt()}%)",
                "☀️"
            )
            "beach" -> Triple("Beach Day Alert!", "Sunny and $temp°C — ideal for the beach! 🏖️", "🌊")
            "cultural" -> Triple("Cultural Vibes", "Rainy or cloudy? Perfect for museums & galleries!", "🏛️")
            "adventure" -> Triple("Adventure Time!", "Weather’s great for hiking or adventures!", "🏔️")
            else -> Triple("Discover More", "Check new places nearby!", "🗺️")
        }
    }
}
