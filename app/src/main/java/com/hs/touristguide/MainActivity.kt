package com.hs.touristguide

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.android.libraries.places.api.Places
import com.hs.touristguide.auth.AuthViewModel
import com.hs.touristguide.navigation.NavGraph
import com.hs.touristguide.ui.theme.TouristGuideTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Initialize Firebase SDK
        FirebaseApp.initializeApp(this)

        // ✅ Initialize the Google Places SDK
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "YOUR_GOOGLE_MAPS_API_KEY_HERE")
        }

        // ✅ Create Notification Channel (for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "places_channel",
                "Nearby Places",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for nearby places based on your interests"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        setContent {
            TouristGuideTheme {
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
