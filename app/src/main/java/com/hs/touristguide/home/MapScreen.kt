package com.hs.touristguide.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.tasks.await

@Composable
fun MapScreen() {
    val context = LocalContext.current

    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> locationPermissionGranted = isGranted }
    )

    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    if (locationPermissionGranted) {
        val mapView = remember { MapView(context) }
        var userLocation by remember { mutableStateOf<LatLng?>(null) }
        var nearbyPlaces by remember { mutableStateOf<List<com.google.android.libraries.places.api.model.PlaceLikelihood>>(emptyList()) }

        // Initialize PlacesClient
        val placesClient: PlacesClient = remember {
            if (!Places.isInitialized()) {
                Places.initialize(
                    context.applicationContext,
                    "AIzaSyCOeD8tEAQh3uWHErD6-OgvqfQqmxz7Tds" // Replace with your key
                )
            }
            Places.createClient(context)
        }

        // Request actual current location instead of lastLocation
        LaunchedEffect(Unit) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                val locationRequest = CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdateAgeMillis(0)
                    .build()

                val location = fusedLocationClient.getCurrentLocation(locationRequest, null).await()

                Log.d("MapScreen", "Fetched current location: $location")

                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    userLocation = latLng

                    // Get nearby places
                    val placeFields = listOf(
                        Place.Field.NAME,
                        Place.Field.LAT_LNG,
                        Place.Field.TYPES
                    )

                    val request = FindCurrentPlaceRequest.newInstance(placeFields)
                    val response = placesClient.findCurrentPlace(request).await()

                    val filteredPlaces = response.placeLikelihoods.filter { likelihood ->
                        val types = likelihood.place.types ?: emptyList()
                        types.contains(Place.Type.TOURIST_ATTRACTION) ||
                                types.contains(Place.Type.PARK) ||
                                types.contains(Place.Type.MUSEUM)
                    }

                    nearbyPlaces = filteredPlaces
                } else {
                    Log.e("MapScreen", "Location is null!")
                }
            } catch (e: Exception) {
                Log.e("MapScreen", "Error fetching current location", e)
            }
        }

        // Map lifecycle
        DisposableEffect(mapView) {
            mapView.onCreate(Bundle())
            mapView.onStart()
            mapView.onResume()

            onDispose {
                mapView.onPause()
                mapView.onStop()
                mapView.onDestroy()
            }
        }

        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { googleMap ->

                        if (
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            googleMap.isMyLocationEnabled = true
                            googleMap.uiSettings.isMyLocationButtonEnabled = true
                        }

                        userLocation?.let { loc ->
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(loc, 14f))
                            googleMap.addMarker(
                                MarkerOptions().position(loc).title("You are here")
                            )

                            nearbyPlaces.forEach { likelihood ->
                                val place = likelihood.place
                                place.latLng?.let { latLng ->
                                    googleMap.addMarker(
                                        MarkerOptions()
                                            .position(latLng)
                                            .title(place.name ?: "Tourist Spot")
                                    )
                                }
                            }
                        } ?: run {
                            val india = LatLng(20.5937, 78.9629)
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 5f))
                            googleMap.addMarker(
                                MarkerOptions().position(india).title("India")
                            )
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { it.onResume() }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(text = "Location permission is required to show your current location.")
        }
    }
}
