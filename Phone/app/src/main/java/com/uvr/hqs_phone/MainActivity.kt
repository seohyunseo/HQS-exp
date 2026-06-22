package com.uvr.hqs_phone

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.uvr.hqs_phone.data.UserPreferences
import com.uvr.hqs_phone.service.DataCollectionService
import com.uvr.hqs_phone.ui.MainScreen
import com.uvr.hqs_phone.ui.SettingsScreen
import com.uvr.hqs_phone.ui.theme.HQSphoneTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check onboarding once (blocking is fast — DataStore in-process cache)
        val onboardingDone = runBlocking {
            UserPreferences.isOnboardingDone(applicationContext)
        }

        if (!onboardingDone) {
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
            return
        }

        // Start the foreground tracking service
        startForegroundService(Intent(this, DataCollectionService::class.java))

        enableEdgeToEdge()
        setContent {
            HQSphoneTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}