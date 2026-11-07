package com.example.antimarcipan.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.antimarcipan.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.background
import com.example.antimarcipan.collection.CollectionScreen
import com.example.antimarcipan.decks.DeckScreen
import com.example.antimarcipan.manual.ManualAddScreen
import com.example.antimarcipan.scan.ScanScreen
import com.example.antimarcipan.settings.SettingsScreen
import com.example.antimarcipan.synergy.SynergyScreen

private const val SETTINGS_ROUTE = "settings"
private const val MANUAL_ROUTE = "manual_add"
private const val LIFE_ROUTE = "life_counter"

private enum class Dest(
    val route: String,
    val label: String,
    @androidx.annotation.DrawableRes val icon: Int,
) {
    Scan("scan", "Sken", R.drawable.ic_nav_scan),
    Collection("collection", "Sbírka", R.drawable.ic_nav_collection),
    Decks("decks", "Balíky", R.drawable.ic_nav_decks),
    Synergy("synergy", "Rádce", R.drawable.ic_nav_advisor),
    Settings(SETTINGS_ROUTE, "Nastavení", R.drawable.ic_nav_synergy),
}

/** Jemný gradient lišt (nahoře mírně světlejší fialová). */
private val BarGradient = androidx.compose.ui.graphics.Brush.verticalGradient(
    listOf(Color(0xF22B2252), Color(0xF2140F2A)),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntimarcipanApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination
    val route = current?.route
    val onManual = route == MANUAL_ROUTE
    val onLife = route == LIFE_ROUTE
    val isSubScreen = onManual || onLife

    val title = when {
        onManual -> "Přidat ručně"
        onLife -> "Počítadlo životů"
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
          androidx.compose.foundation.layout.Column {
            TopAppBar(
                modifier = Modifier.background(BarGradient),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent,
                ),
                title = {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                listOf(Color(0xFFF6DE9A), Color(0xFFE9C46A), Color(0xFFB98A35)),
                            ),
                        ),
                    )
                },
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
                        IconButton(onClick = { navController.navigate(LIFE_ROUTE) { launchSingleTop = true } }) {
                            Icon(Icons.Filled.Favorite, contentDescription = "Počítadlo životů")
                        }
                        IconButton(onClick = { navController.navigate(MANUAL_ROUTE) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Přidat ručně")
                        }
                    }
                },
            )
            // tenká zlatá linka pod lištou
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().height(1.dp).background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xAAE9C46A), Color.Transparent),
                    )
                )
            )
          }
        },
        bottomBar = {
            if (!isSubScreen) {
                NavigationBar(
                    containerColor = Color.Transparent,
                    modifier = Modifier.background(BarGradient),
                ) {
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
                                // Pružné „vyskočení“ aktivní ikony
                                val scale by androidx.compose.animation.core.animateFloatAsState(
                                    targetValue = if (selected) 1.18f else 1f,
                                    animationSpec = androidx.compose.animation.core.spring(
                                        dampingRatio = 0.45f, stiffness = 380f,
                                    ),
                                    label = "navIconScale",
                                )
                                val iconMod = Modifier.size(24.dp).scale(scale)
                                if (dest == Dest.Settings) {
                                    Icon(Icons.Filled.Settings, contentDescription = dest.label, modifier = iconMod)
                                } else {
                                    Icon(
                                        painter = painterResource(dest.icon),
                                        contentDescription = dest.label,
                                        modifier = iconMod,
                                    )
                                }
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
            // Jemné prolnutí + lehký posun mezi obrazovkami
            enterTransition = {
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260)) +
                    androidx.compose.animation.scaleIn(
                        androidx.compose.animation.core.tween(260), initialScale = 0.97f,
                    )
            },
            exitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180)) },
            popEnterTransition = { androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260)) },
            popExitTransition = { androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180)) },
        ) {
            composable(Dest.Scan.route) { ScanScreen() }
            composable(Dest.Collection.route) { CollectionScreen() }
            composable(Dest.Decks.route) { DeckScreen() }
            composable(Dest.Synergy.route) { SynergyScreen() }
            composable(Dest.Settings.route) { SettingsScreen() }
            composable(MANUAL_ROUTE) { ManualAddScreen() }
            composable(LIFE_ROUTE) { com.example.antimarcipan.life.LifeCounterScreen() }
        }
    }
}
