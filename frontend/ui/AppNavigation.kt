package com.example.eventfinder.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.eventfinder.ui.screens.EventDetailsScreen
import com.example.eventfinder.ui.screens.HomeScreen
import com.example.eventfinder.ui.screens.SearchScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Search : Screen("search")
    object EventDetails : Screen("event_details/{eventId}") {
        fun createRoute(eventId: String) = "event_details/$eventId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                onNavigateToDetails = { eventId ->
                    navController.navigate(Screen.EventDetails.createRoute(eventId))
                }
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { eventId ->
                    navController.navigate(Screen.EventDetails.createRoute(eventId))
                }
            )
        }

        composable(
            route = Screen.EventDetails.route,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { backStackEntry ->
            val eventId = backStackEntry.arguments?.getString("eventId") ?: ""
            EventDetailsScreen(
                eventId = eventId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}