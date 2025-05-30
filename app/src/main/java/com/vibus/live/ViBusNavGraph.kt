package com.vibus.live

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.*
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
        modifier = modifier,
        // Aggiungi transizioni animate tra schermate
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(300))
        }
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

        composable(
            "map",
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            FullscreenMapScreen(
                selectedBusId = null,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            "map/{busId}",
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) { backStackEntry ->
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