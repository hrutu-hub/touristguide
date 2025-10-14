package com.hs.touristguide

import android.os.Bundle
import androidx.activity.ComponentActivity
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

        // Initialize Firebase SDK
        FirebaseApp.initializeApp(this)

        // Initialize the Places SDK with your API key (replace YOUR_API_KEY)
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyCOeD8tEAQh3uWHErD6-OgvqfQqmxz7Tds")
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
