package com.example.antimarcipan.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.antimarcipan.advisor.AdvisorScreen
import com.example.antimarcipan.collection.CollectionScreen
import com.example.antimarcipan.decks.DeckScreen
import com.example.antimarcipan.scan.ScanScreen
import com.example.antimarcipan.settings.SettingsScreen
import com.example.antimarcipan.synergy.SynergyScreen

private const val SETTINGS_ROUTE = "settings"

private enum class Dest(val route: String, val label: String, val icon: ImageVector) {
    Scan("scan", "Sken", Icons.Filled.CameraAlt),
    Collection("collection", "Sbírka", Icons.Filled.ViewModule),
    Decks("decks", "Balíky", Icons.Filled.Style),
    Synergy("synergy", "Synergie", Icons.Filled.Hub),
    Advisor("advisor", "Anti Marcipán", Icons.Filled.AutoAwesome),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntimarcipanApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val route = current?.route
    val onSettings = route == SETTINGS_ROUTE

    val title = when {
        onSettings -> "Nastavení"
        else -> Dest.entries.firstOrNull { d ->
            current?.hierarchy?.any { it.route == d.route } == true
        }?.label ?: "Antimarcipán"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (onSettings) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Zpět",
                            )
                        }
                    }
                },
                actions = {
                    if (!onSettings) {
                        IconButton(onClick = { navController.navigate(SETTINGS_ROUTE) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Nastavení")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!onSettings) {
                NavigationBar {
                    Dest.entries.forEach { dest ->
                        val selected = current?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label, maxLines = 1) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Scan.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Dest.Scan.route) { ScanScreen() }
            composable(Dest.Collection.route) { CollectionScreen() }
            composable(Dest.Decks.route) { DeckScreen() }
            composable(Dest.Synergy.route) { SynergyScreen() }
            composable(Dest.Advisor.route) { AdvisorScreen() }
            composable(SETTINGS_ROUTE) { SettingsScreen() }
        }
    }
}
