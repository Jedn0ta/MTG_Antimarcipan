package com.example.antimarcipan.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.antimarcipan.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.antimarcipan.advisor.AdvisorScreen
import com.example.antimarcipan.collection.CollectionScreen
import com.example.antimarcipan.decks.DeckScreen
import com.example.antimarcipan.manual.ManualAddScreen
import com.example.antimarcipan.scan.ScanScreen
import com.example.antimarcipan.settings.SettingsScreen
import com.example.antimarcipan.synergy.SynergyScreen

private const val SETTINGS_ROUTE = "settings"
private const val MANUAL_ROUTE = "manual_add"

private enum class Dest(
    val route: String,
    val label: String,
    @androidx.annotation.DrawableRes val icon: Int,
) {
    Scan("scan", "Sken", R.drawable.ic_nav_scan),
    Collection("collection", "Sbírka", R.drawable.ic_nav_collection),
    Decks("decks", "Balíky", R.drawable.ic_nav_decks),
    Synergy("synergy", "Synergie", R.drawable.ic_nav_synergy),
    Advisor("advisor", "Anti Marcipán", R.drawable.ic_nav_advisor),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntimarcipanApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val route = current?.route
    val onSettings = route == SETTINGS_ROUTE
    val onManual = route == MANUAL_ROUTE
    val isSubScreen = onSettings || onManual

    val title = when {
        onSettings -> "Nastavení"
        onManual -> "Přidat ručně"
        else -> Dest.entries.firstOrNull { d ->
            current?.hierarchy?.any { it.route == d.route } == true
        }?.label ?: "Antimarcipán"
    }

    Scaffold(
        containerColor = Color.Transparent,
        // Bez tohoto spadne barva textu na černou (průhledné pozadí) a na
        // tmavém pozadí není vidět. Držíme světlý obsah.
        contentColor = MaterialTheme.colorScheme.onSurface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xCC14102A),
                    scrolledContainerColor = Color(0xCC14102A),
                ),
                title = { Text(title) },
                navigationIcon = {
                    if (isSubScreen) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Zpět",
                            )
                        }
                    }
                },
                actions = {
                    if (!isSubScreen) {
                        IconButton(onClick = { navController.navigate(MANUAL_ROUTE) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Přidat ručně")
                        }
                        IconButton(onClick = { navController.navigate(SETTINGS_ROUTE) }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Nastavení")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!isSubScreen) {
                NavigationBar(containerColor = Color(0xCC14102A)) {
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
                            icon = {
                                Icon(
                                    painter = painterResource(dest.icon),
                                    contentDescription = dest.label,
                                    modifier = Modifier.size(24.dp),
                                )
                            },
                            label = { Text(dest.label, maxLines = 1) },
                            colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            ),
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
            composable(MANUAL_ROUTE) { ManualAddScreen() }
        }
    }
}
