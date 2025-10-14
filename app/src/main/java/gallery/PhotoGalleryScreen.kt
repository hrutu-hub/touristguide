package com.hs.touristguide.gallery

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import java.io.File

@Composable
fun PhotoGalleryScreen(navController: NavHostController) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val photosDir = File(context.getExternalFilesDir(null), "CameraPhotos")

    // List of photos
    var photoList by remember { mutableStateOf(listOf<Uri>()) }

    LaunchedEffect(Unit) {
        if (photosDir.exists()) {
            photoList = photosDir.listFiles()?.map { Uri.fromFile(it) } ?: emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your Photos") })
        },
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
                            Image(
                                painter = rememberAsyncImagePainter(uri),
                                contentDescription = "Photo",
                                modifier = Modifier
                                    .size(120.dp)
                            )

                            Button(onClick = {
                                // Delete the photo file
                                val file = File(uri.path!!)
                                if (file.exists()) file.delete()
                                // Update UI
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
