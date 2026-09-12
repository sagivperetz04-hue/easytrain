package com.easytrain.feature.onboarding

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.easytrain.feature.onboarding.chooserole.ChooseRoleRoute
import com.easytrain.feature.onboarding.forgotpassword.ForgotPasswordRoute
import com.easytrain.feature.onboarding.invite.EnterInviteCodeRoute
import com.easytrain.feature.onboarding.profilesetup.ProfileSetupRoute
import com.easytrain.feature.onboarding.signin.SignInRoute
import com.easytrain.feature.onboarding.signup.SignUpRoute
import kotlinx.serialization.Serializable

@Serializable
data object SignInDestination

@Serializable
data object SignUpDestination

@Serializable
data object ForgotPasswordDestination

@Serializable
data object ChooseRoleDestination

@Serializable
data object ProfileSetupDestination

@Serializable
data object EnterInviteCodeDestination

fun NavController.navigateToSignIn(navOptions: NavOptions? = null) = navigate(SignInDestination, navOptions)

fun NavController.navigateToSignUp(navOptions: NavOptions? = null) = navigate(SignUpDestination, navOptions)

fun NavController.navigateToForgotPassword(navOptions: NavOptions? = null) =
    navigate(ForgotPasswordDestination, navOptions)

fun NavController.navigateToEnterInviteCode(navOptions: NavOptions? = null) =
    navigate(EnterInviteCodeDestination, navOptions)

fun NavGraphBuilder.signInScreen(
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    composable<SignInDestination> {
        SignInRoute(onSignUp = onSignUp, onForgotPassword = onForgotPassword)
    }
}

fun NavGraphBuilder.signUpScreen(onSignIn: () -> Unit) {
    composable<SignUpDestination> { SignUpRoute(onSignIn = onSignIn) }
}

fun NavGraphBuilder.forgotPasswordScreen(onBack: () -> Unit) {
    composable<ForgotPasswordDestination> { ForgotPasswordRoute(onBack = onBack) }
}

fun NavGraphBuilder.chooseRoleScreen() {
    composable<ChooseRoleDestination> { ChooseRoleRoute() }
}

fun NavGraphBuilder.profileSetupScreen(onDone: () -> Unit) {
    composable<ProfileSetupDestination> { ProfileSetupRoute(onDone = onDone) }
}

fun NavGraphBuilder.enterInviteCodeScreen(onSkip: () -> Unit) {
    composable<EnterInviteCodeDestination> { EnterInviteCodeRoute(onSkip = onSkip) }
}
