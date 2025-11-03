package com.hs.touristguide.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // 🔹 LOGIN
    fun onLoginClicked(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
                val userId = result.user?.uid ?: throw Exception("User ID missing")

                // Fetch profile from Firestore
                fetchUserProfile(userId)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true
                )
                Log.d("AuthDebug", "Login success ✅")
            } catch (e: Exception) {
                Log.e("AuthDebug", "Login failed: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Login failed. Please try again."
                )
            }
        }
    }

    // 🔹 SIGNUP
    fun onSignUpClicked(
        name: String,
        interest: String,
        email: String,
        password: String,
        confirmPassword: String
    ) {
        if (password != confirmPassword) {
            _uiState.value = _uiState.value.copy(error = "Passwords do not match")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
                val userId = result.user?.uid ?: throw Exception("User ID missing")

                val userData = mapOf(
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "interest" to interest.trim()
                )

                db.collection("users").document(userId)
                    .set(userData)
                    .addOnSuccessListener {
                        Log.d("AuthDebug", "Firestore save success ✅")
                    }
                    .addOnFailureListener { ex ->
                        Log.w("AuthDebug", "Firestore save failed ❌: ${ex.message}")
                    }

                // Immediately mark success for smooth navigation
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoggedIn = true,
                    userName = name.trim()
                )
                Log.d("AuthDebug", "Signup success ✅")
            } catch (e: Exception) {
                Log.e("AuthDebug", "Signup failed: ${e.message}")
                val message = when {
                    e.message?.contains("email address is already in use", true) == true ->
                        "This email is already registered. Please log in instead."
                    else -> e.message ?: "Sign-up failed. Please try again."
                }
                _uiState.value = _uiState.value.copy(isLoading = false, error = message)
            }
        }
    }

    // 🔹 FETCH USER PROFILE (used after login/signup)
    fun fetchUserProfile(uid: String? = auth.currentUser?.uid) {
        uid ?: return
        viewModelScope.launch {
            try {
                val snapshot = db.collection("users").document(uid).get().await()
                val name = snapshot.getString("name")
                val interest = snapshot.getString("interest")
                _uiState.value = _uiState.value.copy(
                    userName = name,
                    userInterest = interest,
                    error = null
                )
                Log.d("AuthDebug", "Fetched profile ✅: $name | $interest")
            } catch (e: Exception) {
                Log.e("AuthDebug", "Failed to fetch user profile ❌: ${e.message}")
            }
        }
    }

    // 🔹 FORGOT PASSWORD
    fun onForgotPassword(email: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                auth.sendPasswordResetEmail(email.trim()).await()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Password reset email sent successfully!"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to send password reset email."
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState()
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val userName: String? = null,
    val userInterest: String? = null
)
