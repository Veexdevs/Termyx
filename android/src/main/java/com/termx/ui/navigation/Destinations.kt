package com.termx.ui.navigation

/** Navigation destinations for TERMYX. */
sealed class Screen(val route: String, val label: String) {
    // Bottom navigation (primary)
    object Home : Screen("home", "HOME")
    object Terminal : Screen("terminal", "TERMINAL")
    object Systems : Screen("systems", "SYSTEMS")
    object Servers : Screen("servers", "SERVERS")
    object Ia : Screen("ia", "/IA")

    // Side menu
    object Files : Screen("files", "Files")
    object CodeEditor : Screen("editor", "Code Editor")
    object DeviceAccess : Screen("device", "Device Access")
    object Applications : Screen("apps", "Applications")
    object Downloads : Screen("downloads", "Downloads")
    object AiProviders : Screen("providers", "AI Providers")
    object Settings : Screen("settings", "Settings")
    object About : Screen("about", "About")

    companion object {
        val bottomItems = listOf(Home, Terminal, Systems, Servers, Ia)
        val drawerItems = listOf(
            Files, CodeEditor, DeviceAccess, Applications, Downloads, AiProviders, Settings, About
        )
    }
}
