package com.corbraytechnologies.garudasentinel.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.corbraytechnologies.garudasentinel.ui.components.HairlineDivider
import com.corbraytechnologies.garudasentinel.ui.components.TabIcons
import com.corbraytechnologies.garudasentinel.ui.screens.AboutScreen
import com.corbraytechnologies.garudasentinel.ui.screens.AppScreen
import com.corbraytechnologies.garudasentinel.ui.screens.DeviceScreen
import com.corbraytechnologies.garudasentinel.ui.screens.EulaScreen
import com.corbraytechnologies.garudasentinel.ui.screens.ExploreScreen
import com.corbraytechnologies.garudasentinel.ui.screens.FaqScreen
import com.corbraytechnologies.garudasentinel.ui.screens.FilesScreen
import com.corbraytechnologies.garudasentinel.ui.screens.HelpScreen
import com.corbraytechnologies.garudasentinel.ui.screens.HistoryScreen
import com.corbraytechnologies.garudasentinel.ui.screens.ReportScreen
import com.corbraytechnologies.garudasentinel.ui.screens.MediaScreen
import com.corbraytechnologies.garudasentinel.ui.screens.PermissionsScreen
import com.corbraytechnologies.garudasentinel.ui.screens.UsageScreen
import com.corbraytechnologies.garudasentinel.ui.screens.WatchersScreen
import com.corbraytechnologies.garudasentinel.ui.theme.GarudaType
import com.corbraytechnologies.garudasentinel.ui.theme.Palette

/** The four bottom bar tabs. Each is the root of its own part of the app. */
enum class Tab(val route: String, val label: String, val icon: ImageVector) {
    REPORT("report", "Report", TabIcons.Shield),
    EXPLORE("explore", "Explore", TabIcons.Grid),
    CONTROLS("controls", "Controls", TabIcons.Sliders),
    HELP("help", "Help", TabIcons.Help),
    ;

    companion object {
        fun ofRoute(route: String?): Tab? = entries.firstOrNull { it.route == route }
    }
}

/** Screens that open from a tab, with a back arrow. [pattern] lists the optional arguments a screen accepts. */
enum class Dest(val route: String, val pattern: String = route) {
    APPS("apps", "apps?$ARG_PACKAGE={$ARG_PACKAGE}&$ARG_SORT={$ARG_SORT}"),
    USAGE("usage"),
    MEDIA("media", "media?$ARG_FILTER={$ARG_FILTER}"),
    FILES("files"),
    DEVICE("device"),
    WATCHERS("watchers"),
    HISTORY("history"),
    PERMISSIONS("permissions"),
    FAQ("faq"),
    ABOUT("about"),
}

const val ARG_PACKAGE = "package"
const val ARG_SORT = "sort"
const val ARG_FILTER = "filter"

/** Apps, scrolled to [packageName] and with its row open. */
fun appRoute(packageName: String) = "${Dest.APPS.route}?$ARG_PACKAGE=$packageName"

/** Apps, sorted with the apps that hold the most sensitive access first. */
fun appsByAccessRoute() = "${Dest.APPS.route}?$ARG_SORT=$SORT_MOST_ACCESS"

/** Photos and media, showing only photos that carry a location. */
fun locatedPhotosRoute() = "${Dest.MEDIA.route}?$ARG_FILTER=$FILTER_LOCATED"

const val SORT_MOST_ACCESS = "access"
const val FILTER_LOCATED = "located"

@Composable
fun GarudaNavHost() {
    val main = containerViewModel { MainViewModel(it) }
    val eulaAccepted by main.eulaAccepted.collectAsStateWithLifecycle()

    when (eulaAccepted) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        false -> EulaScreen(onAccept = { main.acceptEula() })
        true -> MainTabs(main)
    }
}

@Composable
private fun MainTabs(main: MainViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStack.collectAsStateWithLifecycle()
    // The selected tab is the last tab root on the back stack; screens opened from Report sit
    // directly on it, so the fallback is Report.
    val selected = backStack.asReversed().firstNotNullOfOrNull { Tab.ofRoute(it.destination.route) } ?: Tab.REPORT
    val go: (Dest) -> Unit = { nav.navigate(it.route) }
    val goRoute: (String) -> Unit = { nav.navigate(it) }
    val back: () -> Unit = { nav.popBackStack() }

    Scaffold(
        containerColor = Palette.Background,
        bottomBar = { BottomTabs(selected, onSelect = { nav.selectTab(it) }) },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.REPORT.route,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Tab.REPORT.route) { ReportScreen(main, onNavigate = go, onRoute = goRoute) }
            composable(Tab.EXPLORE.route) { ExploreScreen(main, onNavigate = go) }
            composable(Tab.CONTROLS.route) { HistoryScreen(onBack = null) }
            composable(Tab.HELP.route) { HelpScreen(onNavigate = go) }

            composable(
                Dest.APPS.pattern,
                arguments = listOf(
                    navArgument(ARG_PACKAGE) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(ARG_SORT) { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { entry ->
                AppScreen(
                    main,
                    onBack = back,
                    focusPackage = entry.arguments?.getString(ARG_PACKAGE),
                    sortByAccess = entry.arguments?.getString(ARG_SORT) == SORT_MOST_ACCESS,
                )
            }
            composable(
                Dest.MEDIA.pattern,
                arguments = listOf(navArgument(ARG_FILTER) { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                MediaScreen(
                    main,
                    onBack = back,
                    onNavigate = go,
                    locatedOnly = entry.arguments?.getString(ARG_FILTER) == FILTER_LOCATED,
                )
            }
            composable(Dest.USAGE.route) { UsageScreen(main, onBack = back, onNavigate = go) }
            composable(Dest.FILES.route) { FilesScreen(main, onBack = back) }
            composable(Dest.DEVICE.route) { DeviceScreen(onBack = back, onNavigate = go) }
            composable(Dest.WATCHERS.route) { WatchersScreen(main, onBack = back, onRoute = goRoute) }
            composable(Dest.HISTORY.route) { HistoryScreen(onBack = back) }
            composable(Dest.PERMISSIONS.route) { PermissionsScreen(onBack = back) }
            composable(Dest.FAQ.route) { FaqScreen(onBack = back) }
            composable(Dest.ABOUT.route) { AboutScreen(onBack = back) }
        }
    }
}

/**
 * Opens a tab at its root. Report is always at the bottom of the back stack, so back from
 * any other tab root returns to Report, and back from Report leaves the app.
 */
private fun NavHostController.selectTab(tab: Tab) {
    navigate(tab.route) {
        popUpTo(Tab.REPORT.route)
        launchSingleTop = true
    }
}

/** Four equal tabs under a hairline. The selected tab is gold; there is no pill indicator. */
@Composable
private fun BottomTabs(selected: Tab, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(Palette.Background)) {
        HairlineDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .selectableGroup(),
        ) {
            Tab.entries.forEach { tab ->
                val isSelected = tab == selected
                val color = if (isSelected) Palette.Accent else Palette.TextMuted
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(tab) })
                        // TalkBack reads the label in normal case rather than the capitals shown.
                        .clearAndSetSemantics { contentDescription = tab.label },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(tab.icon, contentDescription = null, tint = color)
                    Spacer(Modifier.height(4.dp))
                    Text(tab.label.toUpperCase(LocaleList.current), style = GarudaType.SectionLabel, color = color, maxLines = 1)
                }
            }
        }
    }
}
