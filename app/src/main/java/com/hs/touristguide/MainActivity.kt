package com.hs.touristguide

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.google.android.libraries.places.api.Places
import com.google.firebase.FirebaseApp
import com.hs.touristguide.auth.AuthViewModel
import com.hs.touristguide.navigation.NavGraph
import com.hs.touristguide.notifications.SmartNotificationManager
import com.hs.touristguide.ui.theme.TouristGuideTheme
import com.hs.touristguide.weather.WeatherData
import com.hs.touristguide.workers.WeatherCheckWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase SDK
        FirebaseApp.initializeApp(this)

        // Initialize Google Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyCOeD8tEAQh3uWHErD6-OgvqfQqmxz7Tds")
        }

        // Create notification channels
        createNotificationChannels()

        // Schedule periodic weather check Worker
        scheduleWeatherCheck()

        // Trigger test notification immediately
        testWeatherNotification()

        // Compose UI
        setContent {
            TouristGuideTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavGraph(authViewModel = authViewModel)
                    }
                }
            }
        }
    }

    // Create notification channels (for Android 8+)
    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    "places_channel",
                    "Nearby Places",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications for nearby places based on your interests"
                },
                NotificationChannel(
                    SmartNotificationManager.WEATHER_CHANNEL_ID,
                    "Weather Recommendations",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Smart recommendations based on weather conditions"
                    enableLights(true)
                    enableVibration(true)
                }
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            channels.forEach { notificationManager.createNotificationChannel(it) }
        }
    }

    // Schedule periodic Worker to check weather every 3 hours
    private fun scheduleWeatherCheck() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val weatherCheckRequest = PeriodicWorkRequestBuilder<WeatherCheckWorker>(
            3, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(30, TimeUnit.SECONDS)
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                15, TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "weather_ml_check",
            ExistingPeriodicWorkPolicy.KEEP,
            weatherCheckRequest
        )
    }

    // -------------------------
    // Test ML Notification
    // -------------------------
    private fun testWeatherNotification() {
        // For Android 13+ (API 33) check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permission not granted, skip sending
                println("⚠️ POST_NOTIFICATIONS permission not granted!")
                return
            }
        }

        // Directly send notification using SmartNotificationManager
        val notificationManager = SmartNotificationManager(applicationContext)

        // Hardcode dummy weather for Delhi with timestamp
        val dummyWeather = WeatherData(
            temperature = 28f,
            humidity = 50f,
            windSpeed = 5f,
            condition = "Clear",
            timestamp = System.currentTimeMillis()  // <-- added
        )

        // Send notification with custom message
        notificationManager.sendWeatherRecommendation(
            activityType = "outdoor",
            weather = dummyWeather,
            score = 1.0f
        )
    }}
