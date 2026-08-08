package com.gsoft.opus

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import android.content.Intent
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.gsoft.opus.navigation.OpusNavHost
import com.gsoft.opus.notifications.NotificationHelper
import com.gsoft.opus.notifications.NotificationNavigationBus
import com.gsoft.opus.presentation.theme.ThemeViewModel
import com.gsoft.opus.ui.theme.OpusTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var notificationNavigationBus: NotificationNavigationBus

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            // Result is intentionally ignored — push delivery is best-effort.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        splashScreen.setKeepOnScreenCondition { false }

        requestPostNotificationsPermissionIfNeeded()
        handleNotificationIntent(intent)

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val themeState by themeViewModel.state.collectAsState()
            OpusTheme(
                themeMode = themeState.themeMode,
                colorPalette = themeState.colorPalette
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    OpusApp(navigationBus = notificationNavigationBus)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationIntent(intent)
    }

    /**
     * When the user taps a system notification, open the Notifications tab.
     *
     * Two intent shapes can arrive here:
     *  - Foreground pushes are displayed by [NotificationHelper], whose
     *    PendingIntent carries [NotificationHelper.EXTRA_CLICK_ACTION].
     *  - Background/killed pushes are displayed by the FCM system tray, which
     *    launches this activity with the message's data keys as plain extras
     *    (so the backend's "click_action" key is directly available).
     */
    private fun handleNotificationIntent(intent: Intent?) {
        val action = intent?.getStringExtra(NotificationHelper.EXTRA_CLICK_ACTION)
            ?: intent?.getStringExtra("click_action")
        if (action == NotificationHelper.ACTION_OPEN_NOTIFICATIONS) {
            notificationNavigationBus.requestOpenNotifications()
        }
    }

    /**
     * On Android 13+ (API 33+), POST_NOTIFICATIONS must be requested at runtime
     * for the app to display push notifications.
     */
    private fun requestPostNotificationsPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun OpusApp(navigationBus: NotificationNavigationBus) {
    val navController = rememberNavController()
    OpusNavHost(
        navController = navController,
        openNotificationsRequests = navigationBus.openRequests,
        onOpenNotificationsConsumed = navigationBus::consumeOpenRequest
    )
}