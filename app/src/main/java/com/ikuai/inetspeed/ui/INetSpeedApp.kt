package com.ikuai.inetspeed.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ikuai.inetspeed.core.data.prefs.ThemeChoice
import com.ikuai.inetspeed.core.data.prefs.ThemePreferences
import com.ikuai.inetspeed.core.designsystem.components.TopLevelDestination
import com.ikuai.inetspeed.core.designsystem.theme.INetSpeedTheme
import com.ikuai.inetspeed.core.designsystem.theme.ThemeMode
import com.ikuai.inetspeed.feature.history.HistoryScreen
import com.ikuai.inetspeed.feature.report.ReportScreen
import com.ikuai.inetspeed.feature.servers.ServerSelectionScreen
import com.ikuai.inetspeed.feature.settings.SettingsScreen
import com.ikuai.inetspeed.feature.speedtest.SpeedTestScreen
import com.ikuai.inetspeed.feature.tools.ToolsScreen

@Composable
fun INetSpeedApp() {
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val themeMode = when (themePreferences.getTheme()) {
        ThemeChoice.SYSTEM -> ThemeMode.SYSTEM
        ThemeChoice.LIGHT -> ThemeMode.LIGHT
        ThemeChoice.DARK -> ThemeMode.DARK
    }

    INetSpeedTheme(themeMode = themeMode) {
        val navController = rememberNavController()
        val destinations = TopLevelDestination.entries

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    destinations.forEach { destination ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == destination.route
                        } == true

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (selected) {
                                        destination.selectedIcon
                                    } else {
                                        destination.unselectedIcon
                                    },
                                    contentDescription = destination.label,
                                )
                            },
                            label = { Text(destination.label) },
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = TopLevelDestination.SPEEDTEST.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(TopLevelDestination.SPEEDTEST.route) {
                    SpeedTestScreen()
                }
                composable(TopLevelDestination.TOOLS.route) {
                    ToolsScreen()
                }
                composable(TopLevelDestination.HISTORY.route) {
                    HistoryScreen(onNavigateToDetail = { })
                }
                composable(TopLevelDestination.REPORT.route) {
                    ReportScreen()
                }
                composable(TopLevelDestination.SETTINGS.route) {
                    SettingsScreen(
                        onNavigateToServers = { navController.navigate("servers") },
                        onNavigateToLicenses = { },
                        onNavigateToAbout = { },
                    )
                }
                composable("servers") {
                    ServerSelectionScreen(
                        onServerSelected = { navController.popBackStack() },
                        onNavigateToAdd = { },
                    )
                }
            }
        }
    }
}
