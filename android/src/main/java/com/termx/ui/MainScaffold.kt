package com.termx.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.termx.ui.navigation.Screen
import com.termx.ui.screens.AboutScreen
import com.termx.ui.screens.AiProvidersScreen
import com.termx.ui.screens.ApplicationsScreen
import com.termx.ui.screens.CodeEditorScreen
import com.termx.ui.screens.DeviceAccessScreen
import com.termx.ui.screens.DownloadsScreen
import com.termx.ui.screens.FilesScreen
import com.termx.ui.screens.HomeScreen
import com.termx.ui.screens.IaScreen
import com.termx.ui.screens.ServersScreen
import com.termx.ui.screens.SettingsScreen
import com.termx.ui.screens.SystemsScreen
import com.termx.ui.screens.TerminalScreen
import kotlinx.coroutines.launch

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Screen.drawerItems.forEach { screen ->
                val icon = drawerIcon(screen)
                NavigationDrawerItem(
                    label = { Text(screen.label) },
                    selected = currentRoute == screen.route,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(screen.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = icon?.let { { Icon(it, contentDescription = screen.label) } }
                )
            }
        }
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    Screen.bottomItems.forEach { screen ->
                        val icon = bottomIcon(screen)
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = screen.label) },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) { HomeScreen(navController, padding) }
                composable(Screen.Terminal.route) { TerminalScreen(navController, padding) }
                composable(Screen.Systems.route) { SystemsScreen(navController, padding) }
                composable(Screen.Servers.route) { ServersScreen(navController, padding) }
                composable(Screen.Ia.route) { IaScreen(navController, padding) }
                composable(Screen.Files.route) { FilesScreen(navController, padding) }
                composable(Screen.CodeEditor.route) { CodeEditorScreen(navController, padding) }
                composable(Screen.DeviceAccess.route) { DeviceAccessScreen(navController, padding) }
                composable(Screen.Applications.route) { ApplicationsScreen(navController, padding) }
                composable(Screen.Downloads.route) { DownloadsScreen(navController, padding) }
                composable(Screen.AiProviders.route) { AiProvidersScreen(navController, padding) }
                composable(Screen.Settings.route) { SettingsScreen(navController, padding) }
                composable(Screen.About.route) { AboutScreen(navController, padding) }
            }
        }
    }
}

private fun bottomIcon(screen: Screen): ImageVector = when (screen) {
    Screen.Home -> Icons.Filled.Home
    Screen.Terminal -> Icons.Filled.Terminal
    Screen.Systems -> Icons.Filled.Computer
    Screen.Servers -> Icons.Filled.Dns
    Screen.Ia -> Icons.Filled.Psychology
    else -> Icons.Filled.Info
}

private fun drawerIcon(screen: Screen): ImageVector? = when (screen) {
    Screen.Files -> Icons.Filled.Folder
    Screen.CodeEditor -> Icons.Filled.Code
    Screen.DeviceAccess -> Icons.Filled.Shield
    Screen.Applications -> Icons.Filled.Apps
    Screen.Downloads -> Icons.Filled.Download
    Screen.AiProviders -> Icons.Filled.Api
    Screen.Settings -> Icons.Filled.Settings
    Screen.About -> Icons.Filled.Info
    else -> null
}
