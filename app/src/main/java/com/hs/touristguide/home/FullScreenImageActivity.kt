package com.hs.touristguide.home

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.rememberAsyncImagePainter

class FullScreenImageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imageUriString = intent.getStringExtra("imageUri")

        setContent {
            imageUriString?.let { uriString ->
                FullScreenImageScreen(uri = Uri.parse(uriString)) {
                    finish() // Close activity when tapped
                }
            }
        }
    }
}

@Composable
fun FullScreenImageScreen(uri: Uri, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { onClose() }, // Close on tap
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = uri),
            contentDescription = "Full Screen Photo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop // Fill the screen nicely
        )
    }
}
