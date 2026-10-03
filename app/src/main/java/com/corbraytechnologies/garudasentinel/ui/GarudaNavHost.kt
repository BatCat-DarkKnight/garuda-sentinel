package com.corbraytechnologies.garudasentinel.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.corbraytechnologies.garudasentinel.ui.screens.AboutScreen
import com.corbraytechnologies.garudasentinel.ui.screens.AppScreen
import com.corbraytechnologies.garudasentinel.ui.screens.DeviceScreen
import com.corbraytechnologies.garudasentinel.ui.screens.EulaScreen
import com.corbraytechnologies.garudasentinel.ui.screens.FaqScreen
import com.corbraytechnologies.garudasentinel.ui.screens.FilesScreen
import com.corbraytechnologies.garudasentinel.ui.screens.HistoryScreen
import com.corbraytechnologies.garudasentinel.ui.screens.HomeScreen
import com.corbraytechnologies.garudasentinel.ui.screens.MediaScreen
import com.corbraytechnologies.garudasentinel.ui.screens.PermissionsScreen
import com.corbraytechnologies.garudasentinel.ui.screens.UsageScreen
import com.corbraytechnologies.garudasentinel.ui.screens.WatchersScreen
import com.corbraytechnologies.garudasentinel.ui.screens.YourDataScreen
import kotlinx.coroutines.launch
import com.corbraytechnologies.garudasentinel.ui.theme.Palette
import com.corbraytechnologies.garudasentinel.ui.theme.SmallCorner

enum class Dest(val route: String, val title: String, val icon: ImageVector) {
    HOME("home", "Home", Icons.Default.Home),
    YOUR_DATA("your_data", "Your Data", Icons.Default.Insights),
    APPS("apps", "Apps", Icons.Default.Apps),
    USAGE("usage", "App Usage", Icons.Default.Timer),
    MEDIA("media", "Photos & Media", Icons.Default.PermMedia),
    FILES("files", "Files", Icons.Default.Folder),
    DEVICE("device", "Device & Network", Icons.Default.PhoneAndroid),
    WATCHERS("watchers", "Who can watch", Icons.Default.Visibility),
    HISTORY("history", "Scan History & Export", Icons.AutoMirrored.Filled.MenuBook),
    PERMISSIONS("permissions", "Permissions", Icons.Default.Security),
    FAQ("faq", "FAQ", Icons.AutoMirrored.Filled.Help),
    ABOUT("about", "About", Icons.Default.Info),
}

@Composable
fun GarudaNavHost() {
    val main = containerViewModel { MainViewModel(it) }
    val eulaAccepted by main.eulaAccepted.collectAsStateWithLifecycle()

    when (eulaAccepted) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        false -> EulaScreen(onAccept = { main.acceptEula() })
        true -> MainDrawer(main)
    }
}

@Composable
private fun MainDrawer(main: MainViewModel) {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by nav.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    val openMenu: () -> Unit = { scope.launch { drawerState.open() } }
    val go: (Dest) -> Unit = { dest -> nav.navigateTop(dest) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp), drawerContainerColor = Palette.Surface) {
                Box(Modifier.verticalScroll(rememberScrollState())) {
                    androidx.compose.foundation.layout.Column {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "GARUDA SENTINEL",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        )
                        Dest.entries.forEach { dest ->
                            NavigationDrawerItem(
                                icon = { Icon(dest.icon, contentDescription = null) },
                                label = { Text(dest.title) },
                                selected = current == dest.route,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    go(dest)
                                },
                                modifier = Modifier.padding(horizontal = 12.dp),
                                shape = SmallCorner,
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Palette.AccentTint,
                                    selectedIconColor = Palette.Accent,
                                    selectedTextColor = Palette.Accent,
                                    unselectedContainerColor = Palette.Surface,
                                    unselectedIconColor = Palette.TextDim,
                                    unselectedTextColor = Palette.Text,
                                ),
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        },
    ) {
        NavHost(navController = nav, startDestination = Dest.HOME.route) {
            composable(Dest.HOME.route) { HomeScreen(main, onMenuClick = openMenu, onNavigate = go) }
            composable(Dest.YOUR_DATA.route) { YourDataScreen(main, onMenuClick = openMenu, onNavigate = go) }
            composable(Dest.APPS.route) { AppScreen(main, onMenuClick = openMenu) }
            composable(Dest.USAGE.route) { UsageScreen(main, onMenuClick = openMenu, onNavigate = go) }
            composable(Dest.MEDIA.route) { MediaScreen(main, onMenuClick = openMenu, onNavigate = go) }
            composable(Dest.FILES.route) { FilesScreen(main, onMenuClick = openMenu) }
            composable(Dest.DEVICE.route) { DeviceScreen(onMenuClick = openMenu, onNavigate = go) }
            composable(Dest.WATCHERS.route) { WatchersScreen(main, onMenuClick = openMenu, onNavigate = go) }
            composable(Dest.HISTORY.route) { HistoryScreen(onMenuClick = openMenu) }
            composable(Dest.PERMISSIONS.route) { PermissionsScreen(onMenuClick = openMenu) }
            composable(Dest.FAQ.route) { FaqScreen(onMenuClick = openMenu) }
            composable(Dest.ABOUT.route) { AboutScreen(onMenuClick = openMenu) }
        }
    }
}

private fun NavHostController.navigateTop(dest: Dest) {
    navigate(dest.route) {
        popUpTo(Dest.HOME.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
