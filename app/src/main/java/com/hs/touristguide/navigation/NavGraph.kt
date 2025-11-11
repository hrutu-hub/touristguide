package com.hs.touristguide.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hs.touristguide.auth.AuthViewModel
import com.hs.touristguide.auth.LoginScreen
import com.hs.touristguide.auth.SignUpScreen
import com.hs.touristguide.auth.ForgotPasswordScreen
import com.hs.touristguide.gallery.PhotoGalleryScreen
import com.hs.touristguide.home.HomeScreen
import com.hs.touristguide.home.ChatBotScreen
import com.hs.touristguide.screens.MapScreen
import com.hs.touristguide.profile.ProfileSetupScreen
import com.hs.touristguide.weather.WeatherScreen

@Composable
fun NavGraph(
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = "login") {

        // Login Screen
        composable("login") {
            LoginScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }

        // SignUp Screen
        composable("signup") {
            SignUpScreen(
                authViewModel = authViewModel,
                navController = navController
            )
        }

        // Forgot Password Screen
        composable("forgotPassword") {
            ForgotPasswordScreen(
                onBackToLogin = { navController.popBackStack() }
            )
        }

        // Home Screen
        composable("home") {
            HomeScreen(navController = navController)
        }
        composable("profileSetup") {
            ProfileSetupScreen(navController)
        }

        // Map Screen
        composable("map") {
            MapScreen()
        }

        // Chatbot Screen
        composable("chatbot") {
            ChatBotScreen()
        }
        composable("weather") {
            WeatherScreen(navController = navController)
        }
        // Gallery Screen
        composable("gallery") {
            PhotoGalleryScreen(navController = navController)
        }
    }
}

@Composable
fun ProfileSetupScreen(x0: NavHostController) {
    TODO("Not yet implemented")
}