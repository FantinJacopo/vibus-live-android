package com.vibus.live

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibus.live.ui.screens.dashboard.DashboardScreen
import com.vibus.live.ui.screens.map.FullscreenMapScreen

@Composable
fun ViBusNavGraph(
    modifier: Modifier = Modifier,
    startDestination: String = "dashboard"
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToMap = { busId ->
                    if (busId != null) {
                        navController.navigate("map/$busId")
                    } else {
                        navController.navigate("map")
                    }
                }
            )
        }

        composable("map") {
            FullscreenMapScreen(
                selectedBusId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable("map/{busId}") { backStackEntry ->
            val busId = backStackEntry.arguments?.getString("busId")
            FullscreenMapScreen(
                selectedBusId = busId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}