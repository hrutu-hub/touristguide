package com.hs.touristguide.gallery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.hs.touristguide.home.FullScreenImageActivity
import java.io.File

@Composable
fun PhotoGalleryScreen(navController: NavHostController) { // Removed navController since we don't need it for Activity
    val context = LocalContext.current
    val photosDir = File(context.getExternalFilesDir(null), "CameraPhotos")

    var photoList by remember { mutableStateOf(listOf<Uri>()) }

    // Load photos from directory
    LaunchedEffect(Unit) {
        if (photosDir.exists()) {
            photoList = photosDir.listFiles()?.map { Uri.fromFile(it) } ?: emptyList()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Your Photos") }) },
        content = { paddingValues ->
            if (photoList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No photos available.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(photoList) { uri ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.1f))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Photo thumbnail clickable
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Photo",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clickable {
                                        // Open FullScreenImageActivity
                                        val intent = Intent(context, FullScreenImageActivity::class.java)
                                        intent.putExtra("imageUri", uri.toString())
                                        context.startActivity(intent)
                                    }
                            )

                            // Delete button
                            Button(onClick = {
                                val file = File(uri.path!!)
                                if (file.exists()) file.delete()
                                photoList = photoList.filter { it != uri }
                            }) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    )
}
