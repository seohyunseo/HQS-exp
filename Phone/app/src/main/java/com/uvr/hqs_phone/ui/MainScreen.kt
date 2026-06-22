package com.uvr.hqs_phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uvr.hqs_phone.data.db.DurationSummary
import com.uvr.hqs_phone.data.db.LifelogEntity
import com.uvr.hqs_phone.util.KstTimeUtils

// ── Colour tokens ──────────────────────────────────────────────────────────
private val BgDark = Color(0xFF0D1117)
private val SurfaceDark = Color(0xFF161B22)
private val CardDark = Color(0xFF1F2937)
private val AccentPhysical = Color(0xFF22D3EE)   // cyan
private val AccentDigital = Color(0xFFA78BFA)    // purple
private val TextPrimary = Color(0xFFF0F6FC)
private val TextSecondary = Color(0xFF8B949E)

@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    vm: MainViewModel = viewModel()
) {
    val physicalEvents by vm.physicalEvents.collectAsStateWithLifecycle()
    val digitalEvents by vm.digitalEvents.collectAsStateWithLifecycle()
    val physicalSummary by vm.physicalSummary.collectAsStateWithLifecycle()
    val digitalSummary by vm.digitalSummary.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("🏃 Physical", "📱 Digital")

    Scaffold(
        containerColor = BgDark,
        topBar = {
            TopAppBarRow(
                today = KstTimeUtils.todayKstString(),
                onSettings = onNavigateToSettings
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Tab row ──────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = SurfaceDark,
                contentColor = TextPrimary,
                indicator = { tabPositions ->
                    val accent = if (selectedTab == 0) AccentPhysical else AccentDigital
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp)
                            .background(accent, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                        selectedContentColor = if (index == 0) AccentPhysical else AccentDigital,
                        unselectedContentColor = TextSecondary
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────
            if (selectedTab == 0) {
                PhysicalTab(summary = physicalSummary, events = physicalEvents)
            } else {
                DigitalTab(summary = digitalSummary, events = digitalEvents)
            }
        }
    }
}

@Composable
private fun TopAppBarRow(today: String, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("HQS Lifelog", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(today, color = TextSecondary, fontSize = 13.sp)
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextSecondary)
        }
    }
}

// ── Physical Tab ──────────────────────────────────────────────────────────

@Composable
private fun PhysicalTab(summary: List<DurationSummary>, events: List<LifelogEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { PhysicalSummaryCard(summary) }
        if (events.isEmpty()) {
            item { EmptyState("No physical activity logged today") }
        } else {
            items(events, key = { it.id }) { event ->
                PhysicalEventCard(event)
            }
        }
    }
}

@Composable
private fun PhysicalSummaryCard(summary: List<DurationSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Today's Activity",
                color = AccentPhysical,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(12.dp))
            if (summary.isEmpty()) {
                Text("No data yet", color = TextSecondary, fontSize = 14.sp)
            } else {
                summary.forEach { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            s.name.replace("_", " ").lowercase()
                                .replaceFirstChar { it.uppercase() },
                            color = TextPrimary, fontSize = 14.sp
                        )
                        Text(
                            KstTimeUtils.formatDurationMs(s.totalDuration),
                            color = AccentPhysical, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhysicalEventCard(event: LifelogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentPhysical.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(activityEmoji(event.name), fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    event.name.replace("_", " "),
                    color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp
                )
                Text(
                    "${KstTimeUtils.formatEpochToReadable(event.startTime)}  →  " +
                            KstTimeUtils.formatEpochToReadable(event.endTime),
                    color = TextSecondary, fontSize = 12.sp
                )
            }
            Text(
                KstTimeUtils.formatDurationMs(event.duration),
                color = AccentPhysical, fontWeight = FontWeight.Bold, fontSize = 13.sp
            )
        }
    }
}

private fun activityEmoji(name: String) = when (name) {
    "WALKING" -> "🚶"
    "RUNNING" -> "🏃"
    "ON_BICYCLE" -> "🚴"
    "IN_VEHICLE" -> "🚗"
    else -> "⚡"
}

// ── Digital Tab ───────────────────────────────────────────────────────────

@Composable
private fun DigitalTab(summary: List<DurationSummary>, events: List<LifelogEntity>) {
    val totalScreenTime = summary.sumOf { it.totalDuration }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { DigitalSummaryCard(totalScreenTime, summary.take(5)) }
        if (events.isEmpty()) {
            item { EmptyState("No app usage logged today") }
        } else {
            items(events, key = { it.id }) { event ->
                DigitalEventCard(event)
            }
        }
    }
}

@Composable
private fun DigitalSummaryCard(totalMs: Long, topApps: List<DurationSummary>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Screen Time Today", color = AccentDigital, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                KstTimeUtils.formatDurationMs(totalMs),
                color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 28.sp
            )
            if (topApps.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Top Apps", color = TextSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                topApps.forEach { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(s.name, color = TextPrimary, fontSize = 13.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            KstTimeUtils.formatDurationMs(s.totalDuration),
                            color = AccentDigital, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitalEventCard(event: LifelogEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(AccentDigital.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📱", fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(event.name, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${KstTimeUtils.formatEpochToReadable(event.startTime)}  →  " +
                            KstTimeUtils.formatEpochToReadable(event.endTime),
                    color = TextSecondary, fontSize = 12.sp
                )
            }
            Text(
                KstTimeUtils.formatDurationMs(event.duration),
                color = AccentDigital, fontWeight = FontWeight.Bold, fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = TextSecondary, fontSize = 14.sp)
    }
}
