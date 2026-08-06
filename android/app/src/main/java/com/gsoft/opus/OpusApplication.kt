package com.gsoft.opus

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.gsoft.opus.notifications.FcmTokenManager
import com.gsoft.opus.notifications.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class OpusApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    override fun onCreate() {
        super.onCreate()

        // Register notification channels (required for Android 8+)
        notificationHelper.createChannels()

        // Fetch the FCM token and register it with the backend if the user is logged in.
        // This runs on every app start so the token stays in sync even after reinstall
        // or token rotation.
        fcmTokenManager.fetchAndRegisterToken()
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
    }
}
