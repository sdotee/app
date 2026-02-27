package s.how.see

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.video.VideoFrameDecoder
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SEEApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun newImageLoader(context: android.content.Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    private fun createNotificationChannels() {
        val channel = NotificationChannel(
            CHANNEL_ID_GENERAL,
            "General",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for link creation and file uploads"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID_GENERAL = "see_general"
    }
}
