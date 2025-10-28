package com.hs.touristguide.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hs.touristguide.home.HomeScreen
import com.hs.touristguide.auth.AuthViewModel
import com.hs.touristguide.auth.LoginScreen
import com.hs.touristguide.auth.SignUpScreen
import com.hs.touristguide.gallery.PhotoGalleryScreen
import com.hs.touristguide.home.ChatBotScreen // ✅ Assuming HomeScreen is here
import com.hs.touristguide.screens.MapScreen
      // Placeholder or real one

@Composable
fun NavGraph(
    authViewModel: AuthViewModel,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(authViewModel = authViewModel, navController = navController)
        }

        composable("signup") {
            SignUpScreen(authViewModel = authViewModel, navController = navController)
        }

        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("map") {
            MapScreen()
        }

        composable("chatbot") {
            ChatBotScreen() // Create this screen if not yet made
        }
            composable("gallery") {
                PhotoGalleryScreen(navController = navController)
            }

        }
    }

