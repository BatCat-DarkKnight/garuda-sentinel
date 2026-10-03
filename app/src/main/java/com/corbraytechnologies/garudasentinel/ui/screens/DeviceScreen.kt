package com.corbraytechnologies.garudasentinel.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.corbraytechnologies.garudasentinel.permissions.Permissions
import com.corbraytechnologies.garudasentinel.ui.DeviceViewModel
import com.corbraytechnologies.garudasentinel.ui.Dest
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.components.KeyValueRow
import com.corbraytechnologies.garudasentinel.ui.components.SectionCard
import com.corbraytechnologies.garudasentinel.ui.containerViewModel
import com.corbraytechnologies.garudasentinel.utils.formatDate
import com.corbraytechnologies.garudasentinel.utils.formatDuration
import java.util.Locale

@Composable
fun DeviceScreen(onBack: () -> Unit, onNavigate: (Dest) -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val vm = containerViewModel { DeviceViewModel(it) }
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val locations by vm.locations.collectAsStateWithLifecycle()
    val locationStatus by vm.locationStatus.collectAsStateWithLifecycle()
    val reading by vm.readingLocation.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        vm.refresh()
        onPauseOrDispose { }
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) vm.readLocation()
    }

    GarudaScaffold(
        title = "Device & Network",
        onBack = onBack,
        actions = { IconButton(onClick = { vm.refresh() }) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") } },
    ) { padding ->
        val s = snapshot
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (s == null) {
                item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                return@LazyColumn
            }
            item {
                SectionCard(
                    title = "This phone",
                    subtitle = "Any app can read these without asking. Together they make a phone easy to recognize again (a \"fingerprint\").",
                ) {
                    KeyValueRow("Device name", s.device.deviceName)
                    KeyValueRow("Maker and model", "${s.device.manufacturer} ${s.device.model}")
                    KeyValueRow("Android", "${s.device.androidVersion} (API ${s.device.sdkInt})")
                    KeyValueRow("Security patch", s.device.securityPatch)
                    KeyValueRow("Screen", "${s.device.screenWidthPx} x ${s.device.screenHeightPx}, ${s.device.screenDensityDpi} dpi")
                    KeyValueRow("Language", s.device.locale)
                    KeyValueRow("Time zone", s.device.timeZone)
                    KeyValueRow("Last restart", "${formatDate(s.device.lastBootAt)} (up ${formatDuration(s.device.uptimeMs)})")
                    KeyValueRow("Build fingerprint", s.device.fingerprint)
                }
            }
            item {
                SectionCard(title = "Battery") {
                    KeyValueRow("Level", s.battery.levelPercent?.let { "$it%" })
                    KeyValueRow("Status", s.battery.status)
                    KeyValueRow("Plugged into", s.battery.pluggedInto ?: "Not plugged in")
                    KeyValueRow("Health", s.battery.health)
                    KeyValueRow("Temperature", s.battery.temperatureCelsius?.let { String.format(locale, "%.1f C", it) })
                    KeyValueRow("Charge cycles", s.battery.cycleCount?.toString() ?: "Not reported")
                    KeyValueRow("Full in", s.battery.chargeTimeRemainingMs?.let { formatDuration(it) })
                }
            }
            item {
                val n = s.network
                SectionCard(
                    title = "Network",
                    subtitle = "Read from this phone only. Garuda Sentinel has no internet permission and never contacts a server.",
                ) {
                    KeyValueRow("Connected", if (n.connected) n.transports.joinToString().ifBlank { "Yes" } else "No")
                    KeyValueRow("Internet verified", if (n.validatedInternet) "Yes" else "No")
                    KeyValueRow("Metered", if (n.metered) "Yes" else "No")
                    KeyValueRow("VPN", if (n.vpnActive) "On" else "Off")
                    n.wifi?.let { w ->
                        KeyValueRow("Wi-Fi name", w.ssid ?: "Hidden by Android (needs location access)")
                        KeyValueRow("Router ID (BSSID)", w.bssid)
                        KeyValueRow("Signal", w.rssiDbm?.let { "$it dBm" })
                        KeyValueRow("Band", w.frequencyMhz?.let { if (it >= 5000) "5 GHz or higher ($it MHz)" else "2.4 GHz ($it MHz)" })
                    }
                    KeyValueRow("Local addresses", n.ipAddresses.joinToString().ifBlank { null })
                    KeyValueRow("DNS", (n.privateDnsServer?.let { listOf("$it (private DNS)") } ?: n.dnsServers).joinToString().ifBlank { null })
                }
            }
            item {
                SectionCard(
                    title = "Keyboards",
                    subtitle = "Keyboard settings only. Garuda Sentinel never sees what you type.",
                ) {
                    s.input.keyboards.forEach { k ->
                        KeyValueRow(
                            if (k.isDefault) "${k.label} (in use)" else k.label,
                            k.languages.joinToString().ifBlank { "No languages listed" },
                        )
                    }
                    KeyValueRow("Voice typing", if (s.input.speechRecognitionAvailable) "Available" else "Not available")
                }
            }
            item {
                SectionCard(
                    title = "Location",
                    subtitle = "Taken only when you tap the button. Saved readings stay on this phone until you delete them.",
                ) {
                    val latest = locations.firstOrNull()
                    KeyValueRow(
                        "Latest reading",
                        latest?.let {
                            String.format(Locale.US, "%.5f, %.5f (within %.0f m)", it.latitude, it.longitude, it.accuracy)
                        },
                    )
                    latest?.let { KeyValueRow("Taken", "${formatDate(it.timestamp)} via ${it.provider}") }
                    KeyValueRow("Saved readings", locations.size.toString())
                    locationStatus?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !reading,
                            onClick = {
                                if (Permissions.hasAnyLocation(context)) vm.readLocation()
                                else locationPermission.launch(Permissions.locationPermissions)
                            },
                        ) { Text("Read my location") }
                        if (locations.isNotEmpty()) {
                            OutlinedButton(onClick = { vm.clearLocations() }) { Text("Delete readings") }
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = { onNavigate(Dest.PERMISSIONS) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Review permissions")
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
