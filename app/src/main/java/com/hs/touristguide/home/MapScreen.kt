package com.hs.touristguide.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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

    if (!locationPermissionGranted) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Location permission is required to use the map.")
        }
        return
    }

    val mapView = remember { MapView(context) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    // Initialize PlacesClient
    val placesClient: PlacesClient = remember {
        if (!Places.isInitialized()) {
            Places.initialize(
                context.applicationContext,
                "YOUR_GOOGLE_API_KEY_HERE" // replace with your key
            )
        }
        Places.createClient(context)
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var autocompleteResults by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }

    var googleMap by remember { mutableStateOf<com.google.android.gms.maps.GoogleMap?>(null) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        // Shift map down to avoid overlapping UI
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { gMap ->
                        googleMap = gMap
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            gMap.isMyLocationEnabled = true
                            gMap.uiSettings.isMyLocationButtonEnabled = true
                        }
                        val india = LatLng(20.5937, 78.9629)
                        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 5f))
                        gMap.addMarker(MarkerOptions().position(india).title("India"))
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 180.dp) // <-- Map shifted down to leave space for UI
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp), // Keep top spacing
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Search bar with visible black text
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    coroutineScope.launch {
                        try {
                            if (it.isNotBlank()) {
                                val request = FindAutocompletePredictionsRequest.builder()
                                    .setQuery(it)
                                    .build()
                                val response = placesClient.findAutocompletePredictions(request).await()
                                autocompleteResults = response.autocompletePredictions
                            } else {
                                autocompleteResults = emptyList()
                            }
                        } catch (e: Exception) {
                            Log.e("MapScreen", "Autocomplete error", e)
                        }
                    }
                },
                label = { Text("Search location") },
                textStyle = TextStyle(color = Color.Black), // Typed text visible
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                singleLine = true
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {
                items(autocompleteResults) { prediction ->
                    Text(
                        text = prediction.getFullText(null).toString(),
                        color = Color.Black, // Autocomplete visible
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                searchQuery = prediction.getFullText(null).toString()
                                coroutineScope.launch {
                                    try {
                                        val placeRequest = FetchPlaceRequest.builder(
                                            prediction.placeId,
                                            listOf(Place.Field.LAT_LNG, Place.Field.NAME)
                                        ).build()
                                        val place = placesClient.fetchPlace(placeRequest).await().place
                                        val latLng = place.latLng
                                        if (latLng != null) {
                                            googleMap?.apply {
                                                clear()
                                                addMarker(MarkerOptions().position(latLng).title(place.name))
                                                moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                                            }
                                        }
                                        autocompleteResults = emptyList()
                                    } catch (e: Exception) {
                                        Log.e("MapScreen", "Error fetching place", e)
                                    }
                                }
                            }
                            .padding(8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val locationRequest = CurrentLocationRequest.Builder()
                                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                                    .setMaxUpdateAgeMillis(0)
                                    .build()
                                val location = fusedLocationClient.getCurrentLocation(locationRequest, null).await()
                                if (location != null) {
                                    val latLng = LatLng(location.latitude, location.longitude)
                                    userLocation = latLng
                                    googleMap?.apply {
                                        clear()
                                        addMarker(MarkerOptions().position(latLng).title("You are here"))
                                        moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MapScreen", "Error using current location", e)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Use Current Location")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                val addresses = geocoder.getFromLocationName(searchQuery, 1)
                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]
                                    val latLng = LatLng(address.latitude, address.longitude)
                                    googleMap?.apply {
                                        clear()
                                        addMarker(MarkerOptions().position(latLng).title(searchQuery))
                                        moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                                    }
                                } else {
                                    Log.e("MapScreen", "No results found for $searchQuery")
                                }
                            } catch (e: Exception) {
                                Log.e("MapScreen", "Error searching location", e)
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Search")
                }
            }
        }
    }
}
