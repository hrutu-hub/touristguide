package com.hs.touristguide.Homescreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hs.touristguide.R
import com.hs.touristguide.camera.CameraCaptureScreen
import com.hs.touristguide.ui.camera.CameraPermissionRequest

@Composable
fun HomeScreen(navController: NavHostController) {
    var showCamera by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.homescreen),
            contentDescription = "Home Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Overlay
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

        if (showCamera) {
            CameraPermissionRequest {
                CameraCaptureScreen(
                    onImageCaptured = { /* No need to handle here */ showCamera = false },
                    onError = { showCamera = false }
                )
            }
        } else {
            // Main UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🏠 Welcome to Home Screen!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(40.dp))

                Button(
                    onClick = { navController.navigate("map") },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("Open Maps")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { navController.navigate("chatbot") },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("Open AI Chatbot")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showCamera = true },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("Take Photo")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigate to Gallery Screen
                Button(
                    onClick = { navController.navigate("gallery") },
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("View My Photos")
                }
            }
        }
    }
}


