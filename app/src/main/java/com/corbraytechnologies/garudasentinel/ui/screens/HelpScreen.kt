package com.corbraytechnologies.garudasentinel.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.corbraytechnologies.garudasentinel.ui.Dest
import com.corbraytechnologies.garudasentinel.ui.components.HairlineDivider
import com.corbraytechnologies.garudasentinel.ui.components.ListRow
import com.corbraytechnologies.garudasentinel.ui.components.ScreenTitle

/** The Help tab: permissions, questions and answers, and version details. */
@Composable
fun HelpScreen(onNavigate: (Dest) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ScreenTitle("Help")
        Spacer(Modifier.height(16.dp))
        HairlineDivider()
        ListRow("Permissions", supporting = "What each one unlocks, and how to allow it", showChevron = true, onClick = { onNavigate(Dest.PERMISSIONS) })
        HairlineDivider(inset = true)
        ListRow("FAQ", supporting = "Common questions", showChevron = true, onClick = { onNavigate(Dest.FAQ) })
        HairlineDivider(inset = true)
        ListRow("About", supporting = "Version and how the app protects you", showChevron = true, onClick = { onNavigate(Dest.ABOUT) })
        HairlineDivider()
        Spacer(Modifier.height(24.dp))
    }
}
