package com.hs.touristguide.ui.camera

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

@Composable
fun RequestCameraPermission(onPermissionGranted: () -> Unit) {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val permissionStatus = ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        )
        hasPermission = permissionStatus == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.CAMERA),
                0
            )
        }
    }

    if (hasPermission) {
        onPermissionGranted()
    } else {
        Text("Camera permission required to use this feature")
    }
}
