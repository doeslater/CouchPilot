package com.example.couchpilot.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.couchpilot.discover.presentation.DiscoverScreen
import com.example.couchpilot.tonight.presentation.TonightScreen

private data class TopLevelTab(val route: Route, val label: String, val icon: ImageVector)

private val topLevelTabs = listOf(
    TopLevelTab(Route.Tonight, "Tonight", Icons.Filled.Home),
    TopLevelTab(Route.Discover, "Discover", Icons.Filled.Star),
)

@Composable
fun CouchPilotNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val currentDestination = navController.currentBackStackEntryAsState().value?.destination
            NavigationBar {
                topLevelTabs.forEach { tab ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.route::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Tonight,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Route.Tonight> { TonightScreen() }
            composable<Route.Discover> { DiscoverScreen() }
            // Route.ShowDetail's destination arrives in roadmap Phase 6, once ShowDetailScreen exists.
        }
    }
}
