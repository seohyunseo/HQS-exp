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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.uvr.hqs_phone.data.UserPreferences
import com.uvr.hqs_phone.ui.theme.HQSphoneTheme
import kotlinx.coroutines.launch

private val BgDark       = Color(0xFF0D1117)
private val CardDark     = Color(0xFF1F2937)
private val AccentBlue   = Color(0xFF58A6FF)
private val AccentCyan   = Color(0xFF22D3EE)
private val TextPrimary  = Color(0xFFF0F6FC)
private val TextSecondary = Color(0xFF8B949E)

class OnboardingActivity : ComponentActivity() {

    private val activityRecognitionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* continue regardless — user can deny and re-launch later */ }

    private val callPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* continue regardless — social tracking gracefully disabled if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HQSphoneTheme {
                // step 0 = Participant ID entry
                // step 1 = Welcome
                // step 2 = Physical (ACTIVITY_RECOGNITION)
                // step 3 = App Usage
                // step 4 = Social (call permissions)
                // step 5 = Battery optimisation
                // step 6 = All set
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
                        if (currentStep == 0) {
                            // ── Special step: Participant ID entry ───────────────
                            ParticipantIdPage(
                                onSave = { rawId ->
                                    lifecycleScope.launch {
                                        UserPreferences.setParticipantId(applicationContext, rawId)
                                        step++
                                    }
                                }
                            )
                        } else {
                            // ── Standard info / permission steps ─────────────────
                            OnboardingPage(
                                step = currentStep,
                                onNext = {
                                    when (currentStep) {
                                        1 -> step++
                                        2 -> {
                                            activityRecognitionLauncher.launch(
                                                Manifest.permission.ACTIVITY_RECOGNITION
                                            )
                                            step++
                                        }
                                        3 -> {
                                            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                                            step++
                                        }
                                        4 -> {
                                            callPermissionsLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_PHONE_STATE,
                                                    Manifest.permission.READ_CALL_LOG,
                                                    Manifest.permission.READ_CONTACTS
                                                )
                                            )
                                            step++
                                        }
                                        5 -> {
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
                                        6 -> {
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
                    }

                    // Step indicator dots (7 total)
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(7) { index ->
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

// ── Step 0: Participant ID Entry ──────────────────────────────────────────────

@Composable
private fun ParticipantIdPage(onSave: (String) -> Unit) {
    var inputId by remember { mutableStateOf("") }
    val isValid = inputId.trim().isNotEmpty() && inputId.trim().toIntOrNull() != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📋", fontSize = 72.sp)
        Spacer(Modifier.height(32.dp))
        Text(
            "참가자 번호 입력",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 26.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Participant ID",
            color = TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "연구자로부터 받은 참가자 번호를 입력해 주세요.\nEnter the participant ID provided by the researcher.",
            color = TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = inputId,
            onValueChange = { new ->
                // Accept only digits, max 4 characters
                if (new.all { it.isDigit() } && new.length <= 4) inputId = new
            },
            label = { Text("참가자 번호 / Participant ID") },
            placeholder = { Text("예: 01, 12, 100") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedBorderColor = AccentCyan,
                unfocusedBorderColor = TextSecondary,
                focusedLabelColor = AccentCyan,
                unfocusedLabelColor = TextSecondary,
                cursorColor = AccentCyan
            ),
            shape = RoundedCornerShape(12.dp)
        )

        // Live preview of the formatted ID
        if (isValid) {
            Spacer(Modifier.height(12.dp))
            Text(
                "→ 저장될 ID: P%02d".format(inputId.trim().toInt()),
                color = AccentCyan,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(40.dp))
        Button(
            onClick = { if (isValid) onSave(inputId.trim()) },
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentBlue,
                disabledContainerColor = AccentBlue.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("저장 후 계속 / Save & Continue", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

// ── Steps 1–6: Standard info / permission pages ───────────────────────────────

@Composable
private fun OnboardingPage(step: Int, onNext: () -> Unit) {
    // step index 1–6 maps to pages list index 0–5
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
            emoji = "📞",
            title = "Social Interaction",
            description = "To measure your social interaction duration, the app reads call log entries after each call ends. Only the contact name and call duration are recorded — no audio or conversation content is ever accessed or stored.",
            buttonText = "Grant Permission"
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

    val page = pages[step - 1]  // step 1→index 0, step 6→index 5

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
