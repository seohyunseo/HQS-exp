package com.uvr.hqs_phone.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvr.hqs_phone.export.ExportManager
import com.uvr.hqs_phone.service.DataCollectionService
import kotlinx.coroutines.launch

private val BgDark = Color(0xFF0D1117)
private val SurfaceDark = Color(0xFF161B22)
private val CardDark = Color(0xFF1F2937)
private val AccentGreen = Color(0xFF34D399)
private val AccentBlue = Color(0xFF58A6FF)
private val TextPrimary = Color(0xFFF0F6FC)
private val TextSecondary = Color(0xFF8B949E)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val uuid by vm.userUuid.collectAsState()
    val unsyncedCount by vm.unsyncedCount.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    var exportStatus by remember { mutableStateOf("") }

    // ── One-time event collector ──────────────────────────────────────────
    // LaunchedEffect(Unit) runs once per composition lifetime, surviving
    // recompositions. Channel ensures each Toast fires exactly once,
    // regardless of screen rotation.
    LaunchedEffect(Unit) {
        vm.uiEvent.collect { event ->
            when (event) {
                is SyncUiEvent.ShowToast ->
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    ExportManager.exportAll(context, uri)
                    exportStatus = "✓ Export successful"
                } catch (e: Exception) {
                    exportStatus = "✗ Export failed: ${e.message}"
                }
            }
        }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    "Settings",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Participant info ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info, contentDescription = null,
                            tint = AccentGreen, modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Participant Info", color = AccentGreen,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    InfoRow("Participant ID", uuid)
                    InfoRow(
                        "Service Status",
                        if (DataCollectionService.isRunning) "● Running" else "○ Stopped"
                    )
                    InfoRow("Pending Sync", "$unsyncedCount record(s)")
                }
            }

            // ── Manual Firebase Sync ──────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle, contentDescription = null,
                            tint = AccentBlue, modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Firebase Sync", color = AccentBlue,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Upload all unsynced records to Firebase Storage now. " +
                                "The automatic daily sync runs at 01:00 AM.",
                        color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { vm.triggerManualSync() },
                        enabled = !isLoading,   // disabled while spinner is active
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isLoading) {
                            // Spinner shown inside the button during sync
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Syncing…", fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(
                                Icons.Default.CheckCircle, contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            val label = if (unsyncedCount > 0)
                                "Sync Now ($unsyncedCount pending)"
                            else
                                "Sync Now"
                            Text(label, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // ── Local CSV Export ──────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "Data Export", color = TextPrimary,
                        fontWeight = FontWeight.Bold, fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Export the complete study log as a local CSV file.",
                        color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                val filename = ExportManager.suggestedFilename(context)
                                createDocumentLauncher.launch(filename)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Share, contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Export Study Data", fontWeight = FontWeight.SemiBold)
                    }
                    if (exportStatus.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(exportStatus, color = AccentGreen, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
