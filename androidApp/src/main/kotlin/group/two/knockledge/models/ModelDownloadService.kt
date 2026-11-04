package group.two.knockledge.models

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import group.two.knockledge.MainActivity
import group.two.knockledge.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Keeps the process alive while AI model files download and mirrors progress into a
 * low-importance shade notification. [ModelStore] reports progress through [progress];
 * [sync] starts this service on the first non-null value and stops it on null. Downloads
 * only start from the Settings screen, so the app is always in the foreground when
 * `startForegroundService` runs.
 */
class ModelDownloadService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val notifications by lazy { getSystemService(NotificationManager::class.java) }

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
        scope.launch {
            progress.collect { notifications.notify(NOTIFICATION_ID, notification(it)) }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = notification(progress.value)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Creates the low-importance channel once; harmless on repeated calls. */
    private fun ensureChannel() {
        notifications.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_models_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = getString(R.string.notification_channel_models_desc) },
        )
    }

    /** Determinate progress notification that opens the app when tapped. */
    private fun notification(fraction: Float): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            REQUEST_OPEN_APP,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_knockledge)
            .setContentTitle(getString(R.string.notification_title_model_download))
            .setProgress(100, (fraction * 100).toInt(), false)
            .setOngoing(true)
            .setContentIntent(openApp)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "knockledge_model_downloads"
        private const val NOTIFICATION_ID = 3
        private const val REQUEST_OPEN_APP = 5

        /** Overall download progress 0..1 while a session is active. */
        val progress = MutableStateFlow(0f)

        /**
         * Starts the service while [p] is non-null and stops it on null. A rejected
         * `startForegroundService` only loses process protection, not the download.
         */
        fun sync(context: Context, p: Float?) {
            val intent = Intent(context, ModelDownloadService::class.java)
            if (p == null) {
                progress.value = 0f
                context.stopService(intent)
            } else {
                progress.value = p
                runCatching { context.startForegroundService(intent) }
            }
        }
    }
}
