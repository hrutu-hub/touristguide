package com.hs.touristguide.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun ProfileSetupScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    var name by remember { mutableStateOf("") }
    val interestsList = listOf("Nature", "Temples", "History", "Adventure", "Food")
    val selectedInterests = remember { mutableStateListOf<String>() }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF001F3F))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Complete Your Profile", color = Color.White, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text("Select Your Interests", color = Color.White)

            Spacer(modifier = Modifier.height(8.dp))
            Column {
                interestsList.forEach { interest ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp)
                            .toggleable(
                                value = selectedInterests.contains(interest),
                                onValueChange = {
                                    if (it) selectedInterests.add(interest)
                                    else selectedInterests.remove(interest)
                                }
                            )
                    ) {
                        Checkbox(
                            checked = selectedInterests.contains(interest),
                            onCheckedChange = null
                        )
                        Text(interest, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            errorMessage?.let {
                Text(it, color = Color.Red)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val user = auth.currentUser
                    if (user == null) {
                        errorMessage = "User not logged in."
                        return@Button
                    }

                    if (name.isBlank() || selectedInterests.isEmpty()) {
                        errorMessage = "Please fill all details."
                        return@Button
                    }

                    isSaving = true
                    val userData = mapOf(
                        "name" to name,
                        "interests" to selectedInterests
                    )
                    firestore.collection("users").document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            navController.navigate("home") {
                                popUpTo("profilesetup") { inclusive = true }
                            }
                        }
                        .addOnFailureListener {
                            errorMessage = "Failed to save data."
                        }
                        .addOnCompleteListener {
                            isSaving = false
                        }
                },
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSaving)
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else
                    Text("Save and Continue")
            }
        }
    }
}
