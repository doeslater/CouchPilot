package com.example.couchpilot.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
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
import com.example.couchpilot.discover.presentation.DiscoverScreen
import com.example.couchpilot.onboarding.presentation.OnboardingScreen
import com.example.couchpilot.presentation.MainViewModel
import com.example.couchpilot.profile.presentation.ProfileScreen
import com.example.couchpilot.settings.presentation.SettingsScreen
import com.example.couchpilot.showdetail.presentation.ShowDetailScreen
import com.example.couchpilot.tonight.presentation.TonightScreen

private data class TopLevelTab(val route: Route, val label: String, val icon: ImageVector)

private val topLevelTabs = listOf(
    TopLevelTab(Route.Tonight, "Tonight", Icons.Filled.Home),
    TopLevelTab(Route.Discover, "Discover", Icons.Filled.Star),
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
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Route.Tonight,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<Route.Tonight> {
                TonightScreen(onShowClick = { id -> navController.navigate(Route.ShowDetail(id)) })
            }
            composable<Route.Discover> {
                DiscoverScreen(onShowClick = { id, originProviderName ->
                    navController.navigate(Route.ShowDetail(id, originProviderName))
                })
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
                ShowDetailScreen(onBack = { navController.popBackStack() }) 
            }
        }
    }
}
