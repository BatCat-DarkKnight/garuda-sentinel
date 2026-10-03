package com.corbraytechnologies.garudasentinel.ui.screens

import androidx.core.net.toUri
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.corbraytechnologies.garudasentinel.ui.components.ConfirmOpenLink
import com.corbraytechnologies.garudasentinel.ui.components.GarudaScaffold
import com.corbraytechnologies.garudasentinel.ui.theme.Palette

private const val NIST_METADATA_URL = "https://csrc.nist.gov/glossary/term/metadata"
private const val SUPPORT_EMAIL = "info@corbraytechnologies.com"

private val FAQS = listOf(
    "What is metadata, and why does it matter?" to
        "Metadata is information about your information: when and where a photo was taken, which camera took it, " +
        "which apps you use and for how long, which Wi-Fi network you are on. Each piece looks harmless. Combined, " +
        "it can reveal where you live and work, your routines, your health and your relationships.",
    "Does anything leave my phone?" to
        "No. Garuda Sentinel does not have the Android internet permission, so it cannot connect to any server. " +
        "Scan results stay in the app's private storage and are excluded from Google backups. The only way data leaves " +
        "is if you export a file and then share it yourself.",
    "What does Garuda Sentinel collect?" to
        "Only what you allow: your installed apps and their permissions, details of photos, videos and audio (not the " +
        "files themselves), app usage times, files in one folder you choose, device and network facts, and your location " +
        "only when you tap the button. It never reads messages, contacts, browsing history or what you type.",
    "Why are the permissions needed?" to
        "Android protects this information. Each permission unlocks one part of the scan, and every one is optional. " +
        "The Permissions screen explains each one.",
    "Why do some numbers look low or missing?" to
        "Android hides some data on purpose. For example, photo GPS needs \"photo location\" access, Wi-Fi names need " +
        "location access, and apps cannot list your files without you picking a folder. Phone makers also differ.",
    "Why is my screen time different from Digital Wellbeing?" to
        "Both read the same screen on and off records from Android, but they count them a little differently. " +
        "Garuda Sentinel starts the day at midnight and includes the time the screen has been on right now, " +
        "so the two figures can differ by a few minutes.",
    "Why does it not show the websites I visit?" to
        "Android does not allow apps to read browser history. The only workaround is an accessibility service that watches " +
        "your screen, which is far too invasive for a privacy tool. Garuda Sentinel shows how long you use each browser instead.",
    "How do I open an export I protected with a password?" to
        "The file is a standard ZIP encrypted with AES-256. On Windows use 7-Zip, on a Mac use Keka or The Unarchiver. " +
        "The zip tools built into Windows and macOS cannot open AES-encrypted ZIPs and may say the file is damaged or " +
        "ask for the password again. Inside is one file, garuda-export.json, which you can open in any text editor. " +
        "If you forget the password, nobody can open the file.",
    "How do I delete my data?" to
        "Open Scan History & Export and tap \"Delete all scan data\". Uninstalling the app also deletes everything it stored. " +
        "You can also turn on \"Forget results when I close the app\" there, which keeps results in memory only, so they disappear when " +
        "the app closes.",
    "Can I get paid for my data?" to
        "No. Garuda Sentinel does not sell or share data, and has no plans to in this version.",
)

@Composable
fun FaqScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var confirmLink by rememberSaveable { mutableStateOf(false) }

    GarudaScaffold(title = "FAQ", onBack = onBack) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(FAQS) { (question, answer) -> FaqCard(question, answer) }
            item {
                TextButton(onClick = { confirmLink = true }) { Text("Read NIST's definition of metadata") }
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO, "mailto:$SUPPORT_EMAIL".toUri())
                            .putExtra(Intent.EXTRA_SUBJECT, "Garuda Sentinel support")
                        try {
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            // No email app installed; the address is shown below as text.
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Contact support") }
                Text(SUPPORT_EMAIL, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    ConfirmOpenLink(url = NIST_METADATA_URL.takeIf { confirmLink }, onDismiss = { confirmLink = false })
}

@Composable
private fun FaqCard(question: String, answer: String) {
    var expanded by rememberSaveable(question) { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Palette.Surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(question, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(answer, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
