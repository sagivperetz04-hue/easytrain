package com.easytrain.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.easytrain.app.AppUiState
import com.easytrain.app.ui.HomePlaceholderScreen
import com.easytrain.app.ui.LoadingScreen
import com.easytrain.core.model.UserRole
import com.easytrain.feature.onboarding.ChooseRoleDestination
import com.easytrain.feature.onboarding.ProfileSetupDestination
import com.easytrain.feature.onboarding.SignInDestination
import com.easytrain.feature.onboarding.chooseRoleScreen
import com.easytrain.feature.onboarding.enterInviteCodeScreen
import com.easytrain.feature.onboarding.forgotPasswordScreen
import com.easytrain.feature.onboarding.navigateToEnterInviteCode
import com.easytrain.feature.onboarding.navigateToForgotPassword
import com.easytrain.feature.onboarding.navigateToSignIn
import com.easytrain.feature.onboarding.navigateToSignUp
import com.easytrain.feature.onboarding.profileSetupScreen
import com.easytrain.feature.onboarding.signInScreen
import com.easytrain.feature.onboarding.signUpScreen
import kotlinx.serialization.Serializable

@Serializable
data object CoachHomeDestination

@Serializable
data object TraineeHomeDestination

/**
 * The session and the profile decide where the app starts; screens never navigate between these
 * stages themselves. Each stage gets its own NavHost so back cannot walk into a signed-out graph.
 */
@Composable
fun EasyTrainNavHost(
    appState: AppUiState,
    onSignOut: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    when (appState) {
        AppUiState.Loading -> LoadingScreen()

        AppUiState.SignedOut ->
            NavHost(navController = navController, startDestination = SignInDestination) {
                signInScreen(
                    onSignUp = navController::navigateToSignUp,
                    onForgotPassword = navController::navigateToForgotPassword,
                )
                signUpScreen(onSignIn = navController::navigateToSignIn)
                forgotPasswordScreen(onBack = navController::navigateUp)
            }

        AppUiState.NeedsRole ->
            NavHost(navController = navController, startDestination = ChooseRoleDestination) {
                chooseRoleScreen()
            }

        AppUiState.NeedsProfile ->
            NavHost(navController = navController, startDestination = ProfileSetupDestination) {
                profileSetupScreen(onDone = navController::navigateToEnterInviteCode)
                enterInviteCodeScreen(onSkip = navController::navigateUp)
            }

        is AppUiState.Ready -> {
            val start =
                when (appState.role) {
                    UserRole.COACH -> CoachHomeDestination
                    UserRole.TRAINEE -> TraineeHomeDestination
                }
            NavHost(navController = navController, startDestination = start) {
                coachHomeScreen(onSignOut = onSignOut)
                traineeHomeScreen(onSignOut = onSignOut)
            }
        }
    }
}

private fun NavGraphBuilder.coachHomeScreen(onSignOut: () -> Unit) {
    composable<CoachHomeDestination> {
        HomePlaceholderScreen(role = UserRole.COACH, onSignOut = onSignOut)
    }
}

private fun NavGraphBuilder.traineeHomeScreen(onSignOut: () -> Unit) {
    composable<TraineeHomeDestination> {
        HomePlaceholderScreen(role = UserRole.TRAINEE, onSignOut = onSignOut)
    }
}
