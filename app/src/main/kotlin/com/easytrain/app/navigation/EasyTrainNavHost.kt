package com.easytrain.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.easytrain.app.ui.AuthPlaceholderScreen
import kotlinx.serialization.Serializable

@Serializable
data object AuthRoute

@Composable
fun EasyTrainNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = AuthRoute,
    ) {
        composable<AuthRoute> {
            AuthPlaceholderScreen()
        }
    }
}
