package com.easytrain.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.easytrain.app.navigation.EasyTrainNavHost
import com.easytrain.core.designsystem.theme.EasyTrainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Hold the splash while the persisted session is being restored, so the app never flashes
        // the sign-in screen at someone who is already signed in.
        splashScreen.setKeepOnScreenCondition { viewModel.state.value is AppUiState.Loading }

        setContent {
            val appState by viewModel.state.collectAsStateWithLifecycle()

            EasyTrainTheme {
                EasyTrainNavHost(appState = appState, onSignOut = viewModel::onSignOut)
            }
        }
    }
}
