package com.hs.touristguide.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hs.touristguide.R
import com.hs.touristguide.camera.CameraCaptureScreen
import com.hs.touristguide.ui.camera.CameraPermissionRequest
import kotlinx.coroutines.tasks.await
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.hs.touristguide.workers.WeatherCheckWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

@Composable
fun HomeScreen(navController: NavHostController) {
    var showCamera by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf<String?>(null) }
    var interest by remember { mutableStateOf<String?>(null) }
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current

    // ✅ Load Firestore user data safely
    LaunchedEffect(Unit) {
        val user = auth.currentUser
        if (user != null) {
            try {
                val snapshot = db.collection("users").document(user.uid).get().await()
                name = snapshot.getString("name") ?: user.displayName ?: user.email
                interest = snapshot.getString("interest")
            } catch (e: Exception) {
                name = user.displayName ?: user.email
            }
        }
    }

    // ✅ All home features including Test ML Notification
    val items = listOf(
        HomeItem("Open Map", Icons.Filled.LocationOn) { navController.navigate("map") },
        HomeItem("AI Chatbot", Icons.Filled.SmartToy) { navController.navigate("chatbot") },
        HomeItem("Take Photo", Icons.Filled.CameraAlt) { showCamera = true },
        HomeItem("View Gallery", Icons.Filled.PhotoLibrary) { navController.navigate("gallery") },
        HomeItem("Weather", Icons.Filled.Cloud) { navController.navigate("weather") },
        // ✅ NEW: Test ML Notification
        HomeItem("Test ML\nNotification", Icons.Filled.NotificationsActive) {
            val workRequest = OneTimeWorkRequestBuilder<WeatherCheckWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
            Toast.makeText(context, "Testing ML Notification...", Toast.LENGTH_SHORT).show()
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.homescreen),
            contentDescription = "Home Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        if (showCamera) {
            CameraPermissionRequest {
                CameraCaptureScreen(
                    onImageCaptured = { showCamera = false },
                    onError = { showCamera = false }
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ✅ Welcome Message
                Text(
                    text = "🌍 Welcome, ${name ?: "Traveler"}!",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )

                interest?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Based on your interest in $it, explore great places nearby!",
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // ✅ Updated Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(40.dp),
                    horizontalArrangement = Arrangement.spacedBy(36.dp),
                    contentPadding = PaddingValues(
                        top = 20.dp, bottom = 100.dp, start = 8.dp, end = 8.dp
                    )
                ) {
                    items(items) { item ->
                        HomeFeatureCard(item)
                    }
                }
            }
        }
    }
}

data class HomeItem(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@Composable
fun HomeFeatureCard(item: HomeItem) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF1565C0))
            .clickable { item.onClick() }
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = item.title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
