package com.uvr.hqs_phone

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.uvr.hqs_phone.data.UserPreferences
import com.uvr.hqs_phone.ui.theme.HQSphoneTheme
import kotlinx.coroutines.launch

private val BgDark = Color(0xFF0D1117)
private val CardDark = Color(0xFF1F2937)
private val AccentBlue = Color(0xFF58A6FF)
private val AccentCyan = Color(0xFF22D3EE)
private val TextPrimary = Color(0xFFF0F6FC)
private val TextSecondary = Color(0xFF8B949E)

class OnboardingActivity : ComponentActivity() {

    private val activityRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* continue regardless — user can deny and re-launch later */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HQSphoneTheme {
                var step by remember { mutableIntStateOf(0) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BgDark)
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "onboarding_step"
                    ) { currentStep ->
                        OnboardingPage(
                            step = currentStep,
                            onNext = {
                                when (currentStep) {
                                    0 -> step++
                                    1 -> {
                                        activityRecognitionLauncher.launch(
                                            Manifest.permission.ACTIVITY_RECOGNITION
                                        )
                                        step++
                                    }
                                    2 -> {
                                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                        step++
                                    }
                                    3 -> {
                                        val pm = getSystemService(PowerManager::class.java)
                                        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                                            startActivity(
                                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                    data = Uri.parse("package:$packageName")
                                                }
                                            )
                                        }
                                        step++
                                    }
                                    4 -> {
                                        lifecycleScope.launch {
                                            UserPreferences.setOnboardingDone(applicationContext)
                                            startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
                                            finish()
                                        }
                                    }
                                }
                            }
                        )
                    }

                    // Step indicator dots
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(5) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (step == index) 10.dp else 6.dp)
                                    .background(
                                        if (step == index) AccentCyan else TextSecondary.copy(alpha = 0.4f),
                                        CircleShape
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(step: Int, onNext: () -> Unit) {
    val pages = listOf(
        OnboardingData(
            emoji = "🔬",
            title = "HQS Field Study",
            description = "Welcome! This app runs quietly in the background to log your physical activity and app usage during the 1–2 week study period. No personal content is accessed — only activity type and app usage time.",
            buttonText = "Get Started"
        ),
        OnboardingData(
            emoji = "🏃",
            title = "Physical Activity",
            description = "We use the Activity Recognition API to detect walking, running, cycling, and vehicle rides. This requires the Activity Recognition permission.",
            buttonText = "Grant Permission"
        ),
        OnboardingData(
            emoji = "📱",
            title = "App Usage Access",
            description = "To log which apps you use and for how long, the app needs Usage Access permission. You'll be taken to Settings — please enable HQS Lifelog.",
            buttonText = "Open Settings"
        ),
        OnboardingData(
            emoji = "🔋",
            title = "Battery Optimization",
            description = "To ensure continuous data collection, please exempt HQS Lifelog from battery optimization. This prevents the system from stopping the tracking service.",
            buttonText = "Disable Optimization"
        ),
        OnboardingData(
            emoji = "✅",
            title = "You're All Set!",
            description = "Setup is complete. The app will run silently in the background. You'll see a persistent notification — this is normal and required for reliable tracking.",
            buttonText = "Start Logging"
        )
    )

    val page = pages[step]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(page.emoji, fontSize = 72.sp)
        Spacer(Modifier.height(32.dp))
        Text(
            page.title, color = TextPrimary,
            fontWeight = FontWeight.Bold, fontSize = 26.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            page.description, color = TextSecondary,
            fontSize = 15.sp, lineHeight = 22.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(page.buttonText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

private data class OnboardingData(
    val emoji: String,
    val title: String,
    val description: String,
    val buttonText: String
)
