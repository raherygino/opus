package com.gsoft.opus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.gsoft.opus.navigation.OpusNavHost
import com.gsoft.opus.presentation.theme.ThemeViewModel
import com.gsoft.opus.ui.theme.OpusTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { false }

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeState by themeViewModel.state.collectAsState()
            OpusTheme(
                themeMode = themeState.themeMode,
                colorPalette = themeState.colorPalette
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OpusApp()
                }
            }
        }
    }
}

@Composable
private fun OpusApp() {
    val navController = rememberNavController()
    OpusNavHost(navController = navController)
}