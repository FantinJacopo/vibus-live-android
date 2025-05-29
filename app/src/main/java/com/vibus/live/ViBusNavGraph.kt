package com.svt.vibuslive.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vibus.live.ui.screens.dashboard.DashboardScreen

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
            DashboardScreen()
        }
    }
}