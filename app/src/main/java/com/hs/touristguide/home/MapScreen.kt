package com.hs.touristguide.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.net.URL
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Permissions
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var notificationPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) true
            else ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> locationPermissionGranted = isGranted }
    )

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> notificationPermissionGranted = isGranted }
    )

    LaunchedEffect(Unit) {
        if (!locationPermissionGranted)
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationPermissionGranted)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)

        // Notification Channel setup
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "places_channel",
                "Nearby Places Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifications for nearby places" }

            val notificationManager =
                ContextCompat.getSystemService(context, NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
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
    var searchQuery by remember { mutableStateOf("") }
    var autocompleteResults by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var googleMap by remember { mutableStateOf<com.google.android.gms.maps.GoogleMap?>(null) }
    var userInterest by remember { mutableStateOf<String?>(null) }
    var nearbyPlaces by remember { mutableStateOf<List<String>>(emptyList()) }

    val categories = listOf( "museum","lodging", "park", )
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    // Fetch user's interest
    LaunchedEffect(Unit) {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    userInterest = doc.getString("interest") ?: "tourist_attraction"
                }
                .addOnFailureListener {
                    userInterest = "tourist_attraction"
                }
        } else {
            userInterest = "tourist_attraction"
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Safe Places initialization
    val placesClient: PlacesClient = remember {
        val metaDataKey = try {
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            ).metaData?.getString("com.google.android.geo.API_KEY")
        } catch (e: Exception) {
            Log.e("MapScreen", "MetaData read error: ${e.message}")
            null
        }

        if (!Places.isInitialized()) {
            if (!metaDataKey.isNullOrEmpty()) {
                Places.initialize(context.applicationContext, metaDataKey)
            } else {
                Log.e("MapScreen", "API key missing — Places not initialized")
            }
        }

        Places.createClient(context)
    }

    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 180.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    coroutineScope.launch {
                        if (it.isNotBlank()) {
                            try {
                                val req = FindAutocompletePredictionsRequest.builder()
                                    .setQuery(it)
                                    .build()
                                val res = placesClient.findAutocompletePredictions(req).await()
                                autocompleteResults = res.autocompletePredictions
                            } catch (e: Exception) {
                                Log.e("MapScreen", "Autocomplete error: ${e.message}")
                                autocompleteResults = emptyList()
                            }
                        } else autocompleteResults = emptyList()
                    }
                },
                label = { Text("Search location") },
                textStyle = TextStyle(color = Color.White),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .background(Color.White)
            ) {
                items(autocompleteResults) { prediction ->
                    Text(
                        text = prediction.getFullText(null).toString(),
                        color = Color.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                searchQuery = prediction.getFullText(null).toString()
                                coroutineScope.launch {
                                    try {
                                        val req = FetchPlaceRequest.builder(
                                            prediction.placeId,
                                            listOf(Place.Field.LAT_LNG, Place.Field.NAME)
                                        ).build()
                                        val place = placesClient.fetchPlace(req).await().place
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
                                        Log.e("MapScreen", "Place fetch error: ${e.message}")
                                    }
                                }
                            }
                            .padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Buttons Row
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val locReq = CurrentLocationRequest.Builder()
                                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                                    .build()
                                val loc = fusedLocationClient.getCurrentLocation(locReq, null).await()
                                if (loc != null) {
                                    val latLng = LatLng(loc.latitude, loc.longitude)
                                    userLocation = latLng
                                    googleMap?.apply {
                                        clear()
                                        addMarker(MarkerOptions().position(latLng).title("You are here"))
                                        moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MapScreen", "Error getting location", e)
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
                                if (searchQuery.isBlank()) {
                                    Toast.makeText(context, "Enter a location", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }

                                val geocoder = Geocoder(context, Locale.getDefault())
                                val addr = withContext(Dispatchers.IO) {
                                    geocoder.getFromLocationName(searchQuery, 1)
                                }

                                if (!addr.isNullOrEmpty()) {
                                    val latLng = LatLng(addr[0].latitude, addr[0].longitude)
                                    googleMap?.apply {
                                        clear()
                                        addMarker(MarkerOptions().position(latLng).title(searchQuery))
                                        moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                                    }
                                } else {
                                    Toast.makeText(context, "Location not found", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Log.e("MapScreen", "Search error", e)
                                Toast.makeText(context, "Invalid location or no internet", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Search")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Buttons
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    Button(
                        onClick = { selectedCategory = cat },
                        colors = ButtonDefaults.buttonColors(
                            if (selectedCategory == cat) Color(0xFF1E88E5)
                            else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(cat.replace("_", " ").replaceFirstChar { it.uppercase() })
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nearby Places
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val location = userLocation
                            if (location == null) {
                                Toast.makeText(context, "Use current location first!", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            val type = selectedCategory ?: userInterest ?: "tourist_attraction"
                            val apiKey = context.packageManager
                                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                                .metaData?.getString("com.google.android.geo.API_KEY") ?: ""

                            val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                                    "?location=${location.latitude},${location.longitude}" +
                                    "&radius=5000&type=$type&key=$apiKey"

                            val result = withContext(Dispatchers.IO) { URL(url).readText() }
                            val json = JSONObject(result)
                            val resultsArray = json.getJSONArray("results")
                            val placesList = mutableListOf<String>()
                            googleMap?.clear()

                            for (i in 0 until resultsArray.length()) {
                                val place = resultsArray.getJSONObject(i)
                                val name = place.getString("name")
                                val geometry = place.getJSONObject("geometry").getJSONObject("location")
                                val lat = geometry.getDouble("lat")
                                val lng = geometry.getDouble("lng")
                                placesList.add(name)
                                googleMap?.addMarker(MarkerOptions().position(LatLng(lat, lng)).title(name))
                            }

                            nearbyPlaces = placesList

                            if (notificationPermissionGranted) {
                                val notification = NotificationCompat.Builder(context, "places_channel")
                                    .setSmallIcon(android.R.drawable.ic_dialog_map)
                                    .setContentTitle("Nearby ${type.replace("_", " ")} found!")
                                    .setContentText("Found ${placesList.size} places near you.")
                                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                                    .setAutoCancel(true)
                                    .build()
                                NotificationManagerCompat.from(context).notify(1, notification)
                            }

                            Toast.makeText(context, "Nearby $type shown!", Toast.LENGTH_SHORT).show()

                        } catch (e: Exception) {
                            Log.e("MapScreen", "Error fetching nearby", e)
                            Toast.makeText(context, "Error fetching nearby places", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Nearby Places Based on Your Interest")
            }

            if (nearbyPlaces.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Nearby Places:", style = MaterialTheme.typography.titleMedium, color = Color.White)
                LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                    items(nearbyPlaces) { place ->
                        Text(
                            text = place,
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
