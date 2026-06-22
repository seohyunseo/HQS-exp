package com.uvr.hqs_phone.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvr.hqs_phone.export.ExportManager
import com.uvr.hqs_phone.service.DataCollectionService
import kotlinx.coroutines.launch

private val BgDark       = Color(0xFF0D1117)
private val SurfaceDark  = Color(0xFF161B22)
private val CardDark     = Color(0xFF1F2937)
private val AccentGreen  = Color(0xFF34D399)
private val AccentBlue   = Color(0xFF58A6FF)
private val AccentCyan   = Color(0xFF22D3EE)
private val TextPrimary  = Color(0xFFF0F6FC)
private val TextSecondary = Color(0xFF8B949E)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    vm: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val rawParticipantId by vm.participantId.collectAsStateWithLifecycle()
    val formattedId by vm.formattedParticipantId.collectAsStateWithLifecycle()
    val unsyncedCount by vm.unsyncedCount.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    var exportStatus by remember { mutableStateOf("") }

    // Dialog state for editing the participant ID
    var showEditDialog by remember { mutableStateOf(false) }

    // ── One-time event collector ──────────────────────────────────────────
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

    // ── Edit Participant ID Dialog ─────────────────────────────────────────
    if (showEditDialog) {
        ParticipantIdEditDialog(
            currentRawId = rawParticipantId,
            onConfirm = { newId ->
                vm.updateParticipantId(newId)
                showEditDialog = false
            },
            onDismiss = { showEditDialog = false }
        )
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

                    // Participant ID row with inline Edit button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Participant ID", color = TextSecondary, fontSize = 13.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                formattedId,
                                color = AccentCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                        }
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit Participant ID",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
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
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        if (isLoading) {
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

// ── Edit Participant ID Dialog ─────────────────────────────────────────────────

@Composable
private fun ParticipantIdEditDialog(
    currentRawId: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(currentRawId) }
    val isValid = input.trim().isNotEmpty() && input.trim().toIntOrNull() != null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardDark),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Edit Participant ID",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { new ->
                        if (new.all { it.isDigit() } && new.length <= 4) input = new
                    },
                    label = { Text("Participant ID") },
                    placeholder = { Text("예: 01, 12") },
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
                    shape = RoundedCornerShape(10.dp)
                )
                if (isValid) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "→ P%02d".format(input.trim().toInt()),
                        color = AccentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { if (isValid) onConfirm(input.trim()) },
                        enabled = isValid,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentCyan)
                    ) {
                        Text("Save", color = Color(0xFF0D1117), fontWeight = FontWeight.Bold)
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
