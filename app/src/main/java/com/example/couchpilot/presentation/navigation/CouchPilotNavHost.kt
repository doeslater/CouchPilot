package com.example.couchpilot.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.couchpilot.discover.presentation.DiscoverScreen
import com.example.couchpilot.onboarding.presentation.OnboardingScreen
import com.example.couchpilot.presentation.MainViewModel
import com.example.couchpilot.profile.presentation.ProfileScreen
import com.example.couchpilot.settings.presentation.SettingsScreen
import com.example.couchpilot.showdetail.presentation.ShowDetailScreen
import com.example.couchpilot.tonight.presentation.TonightScreen
import com.example.couchpilot.watchmode.presentation.SearchScreen
import com.example.couchpilot.watchmode.presentation.StreamingSourcesScreen

private data class TopLevelTab(val route: Route, val label: String, val icon: ImageVector)

private val topLevelTabs = listOf(
    TopLevelTab(Route.Tonight, "Tonight", Icons.Filled.Home),
    TopLevelTab(Route.Discover, "Discover", Icons.Filled.Star),
    TopLevelTab(Route.Search, "Search", Icons.Filled.Search),
    TopLevelTab(Route.Settings, "Settings", Icons.Filled.Settings),
)

@Composable
fun CouchPilotNavHost(
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val hasCompletedOnboarding by mainViewModel.hasCompletedOnboarding.collectAsState()

    LaunchedEffect(hasCompletedOnboarding) {
        if (hasCompletedOnboarding == false) {
            navController.navigate(Route.Onboarding) {
                popUpTo(0)
            }
        } else if (hasCompletedOnboarding == true) {
            if (navController.currentDestination?.hasRoute(Route.Onboarding::class) == true) {
                navController.navigate(Route.Tonight) {
                    popUpTo(Route.Onboarding) { inclusive = true }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            val currentDestination = navController.currentBackStackEntryAsState().value?.destination
            val showBottomBar = currentDestination?.hasRoute(Route.Onboarding::class) == false
            
            if (showBottomBar) {
                NavigationBar {
                    topLevelTabs.forEach { tab ->
                        val selected = currentDestination.hierarchy.any { it.hasRoute(tab.route::class) }
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
            }
        },
        contentWindowInsets = WindowInsets.navigationBars
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Tonight,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            composable<Route.Tonight> {
                TonightScreen(onShowClick = { id -> navController.navigate(Route.ShowDetail(id)) })
            }
            composable<Route.Discover> {
                DiscoverScreen(onShowClick = { id, originProviderName ->
                    navController.navigate(Route.ShowDetail(id, originProviderName))
                })
            }
            composable<Route.Search> {
                SearchScreen(
                    // Watchmode resolved this hit to a TMDB show - route through ShowDetail so it
                    // gets the same taste-scoring/vote/dwell-time treatment as any other show,
                    // instead of skipping straight to the granular streaming-sources screen.
                    onNavigateToShowDetail = { tmdbId -> navController.navigate(Route.ShowDetail(tmdbId)) },
                    onNavigateToStreamingSources = { titleId, name ->
                        navController.navigate(Route.StreamingSources(titleId, name))
                    }
                )
            }
            composable<Route.Settings> {
                SettingsScreen(onViewProfile = { navController.navigate(Route.Profile) })
            }
            composable<Route.Profile> {
                ProfileScreen(onBack = { navController.popBackStack() })
            }
            composable<Route.Onboarding> {
                OnboardingScreen(onShowInfo = { id -> navController.navigate(Route.ShowDetail(id)) }) 
            }
            composable<Route.ShowDetail> { 
                ShowDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToStreamingSources = { id, name ->
                        navController.navigate(Route.StreamingSources(id, name))
                    }
                ) 
            }
            composable<Route.StreamingSources> { backStackEntry ->
                val route = backStackEntry.toRoute<Route.StreamingSources>()
                StreamingSourcesScreen(
                    showName = route.showName,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
