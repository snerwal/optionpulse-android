package com.optionpulse.scanner

import android.os.Bundle
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.messaging.FirebaseMessaging

private val Navy = Color(0xFF07111F)
private val Panel = Color(0xFF101E2F)
private val Green = Color(0xFF20D99A)
private val Red = Color(0xFFFF6376)
private val Muted = Color(0xFF8DA2B8)

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { DeviceRegistrar.register(it) }
        }
        setContent { OptionPulseApp() }
    }
}

@Composable fun OptionPulseApp(vm: ScannerViewModel = androidx.lifecycle.viewmodel.compose.viewModel()) {
    val state by vm.ui.collectAsStateWithLifecycle()
    MaterialTheme(colorScheme = darkColorScheme(primary = Green, background = Navy, surface = Panel)) {
        Surface(Modifier.fillMaxSize()) {
            if (state.selected != null) SignalDetail(state.selected!!, { vm.select(null) })
            else Dashboard(state, vm::refresh, vm::select, vm::toggleCalls)
        }
    }
}

@Composable private fun Dashboard(state: ScannerUiState, refresh: () -> Unit, select: (Signal) -> Unit, toggle: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Navy).statusBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Bolt, null, tint = Green)
            Column(Modifier.weight(1f).padding(start = 8.dp)) { Text("OPTIONPULSE", fontWeight = FontWeight.Black); Text("NSE F&O momentum scanner", color = Muted, style = MaterialTheme.typography.labelSmall) }
            IconButton(onClick = refresh) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        Spacer(Modifier.height(14.dp))
        StatusCard(state.status)
        state.error?.let { Text("Backend unavailable: $it", color = Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Live setups", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            FilterChip(selected = state.callsOnly, onClick = toggle, label = { Text(if (state.callsOnly) "Calls" else "All") })
        }
        Spacer(Modifier.height(8.dp))
        val shown = state.signals.filter { !state.callsOnly || it.direction == Direction.CALL }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(shown, key = { it.id }) { SignalCard(it) { select(it) } }
        }
    }
}

@Composable private fun StatusCard(s: MarketStatus) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(if (s.connected) Green else Red, RoundedCornerShape(9.dp)))
                Text(if (s.connected) "  MARKET FEED CONNECTED" else "  DISCONNECTED", color = if (s.connected) Green else Red, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f)); Text("${s.latencyMs} ms", color = Muted)
            }
            Spacer(Modifier.height(14.dp))
            Row { Metric("SCANNED", "${s.scanned}/${s.universe}", Modifier.weight(1f)); Metric("ALERTS", "${s.alertsToday}/10", Modifier.weight(1f)); Metric("INDIA VIX", "${s.vix}", Modifier.weight(1f)) }
            Spacer(Modifier.height(12.dp)); Text(s.mode, color = Color(0xFFFFC857), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun Metric(label: String, value: String, modifier: Modifier = Modifier) = Column(modifier) { Text(value, fontWeight = FontWeight.Bold); Text(label, color = Muted, style = MaterialTheme.typography.labelSmall) }

@Composable private fun SignalCard(s: Signal, click: () -> Unit) {
    val accent = if (s.direction == Direction.CALL) Green else Red
    Card(onClick = click, colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(s.symbol, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text(s.setup, color = accent, style = MaterialTheme.typography.labelMedium) }
                Surface(color = accent.copy(alpha = .16f), shape = RoundedCornerShape(20.dp)) { Text("${s.score}/100", color = accent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) }
            }
            Spacer(Modifier.height(12.dp)); Text(s.contract, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp)); Row { Metric("SPOT", "₹${s.spot}", Modifier.weight(1f)); Metric("PREMIUM", "₹${s.premium}", Modifier.weight(1f)); Metric("SPREAD", "${s.spreadPct}%", Modifier.weight(1f)) }
            Spacer(Modifier.height(10.dp)); Text("${s.timestamp}  •  ${s.optionVolume} volume", color = Muted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable private fun SignalDetail(s: Signal, back: () -> Unit) {
    val accent = if (s.direction == Direction.CALL) Green else Red
    LazyColumn(Modifier.fillMaxSize().background(Navy).statusBarsPadding(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, "Back") }; Column { Text(s.symbol, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(s.setup, color = accent) } } }
        item { Card(colors = CardDefaults.cardColors(containerColor = accent.copy(alpha=.12f))) { Column(Modifier.padding(18.dp)) { Text("RECOMMENDED CONTRACT", color = accent, style = MaterialTheme.typography.labelMedium); Text(s.contract, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Ask / premium ₹${s.premium}  •  Spread ${s.spreadPct}%", color = Muted) } } }
        item { Section("Gann Square of 9") { MetricRow("Reference pivot", money(s.pivotPrice)); MetricRow("45° protective level", money(s.gann45)); MetricRow("90° trigger", money(s.gann90)); MetricRow("180° target 1", money(s.gann180)); MetricRow("360° target 2", money(s.gann360)); MetricRow("Spot stop", money(s.spotStop)) } }
        item { Section("Option liquidity") { MetricRow("Open interest", "${s.optionOi}"); MetricRow("Daily volume", "${s.optionVolume}"); MetricRow("Premium stop", "₹${s.premiumStop}") } }
        item { Section("Validation layers") { s.checks.forEach { Row(Modifier.padding(vertical=6.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if(it.passed) Icons.Default.CheckCircle else Icons.Default.Cancel, null, tint=if(it.passed) Green else Red); Column(Modifier.padding(start=10.dp)) { Text(it.name, fontWeight=FontWeight.SemiBold); Text(it.detail, color=Muted, style=MaterialTheme.typography.bodySmall) } } } } }
        item { Text("Alert-only mode. Verify live prices before placing a limit order. Options can lose the entire premium.", color = Color(0xFFFFC857), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable private fun Section(title: String, body: @Composable ColumnScope.() -> Unit) { Card(colors=CardDefaults.cardColors(containerColor=Panel), shape=RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Text(title, fontWeight=FontWeight.Bold); Spacer(Modifier.height(8.dp)); body() } } }
@Composable private fun MetricRow(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical=5.dp)) { Text(label, color=Muted, modifier=Modifier.weight(1f)); Text(value, fontWeight=FontWeight.SemiBold) } }
private fun money(value: Double) = "₹%,.2f".format(value)
