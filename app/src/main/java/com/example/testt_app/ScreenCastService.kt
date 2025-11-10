package com.example.test_app

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.testt_app.R
import java.util.Random

class ScreenCastService : Service() {

    // Properties (nullable/lateinit for service components)
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var screenCastServer: ScreenCastServer? = null

    override fun onCreate() {
        super.onCreate()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        when (action) {
            ACTION_START -> {
                // Start logic (only executed once)
                if (screenCastServer == null) {
                    val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
                    val data = getDataIntentFromServiceIntent(intent)

                    if (resultCode != Activity.RESULT_OK || data == null) {
                        Log.e(LOG_TAG, "Invalid MediaProjection result. Result Code: $resultCode")
                        Toast.makeText(this, "Casting failed: Missing permission data.", Toast.LENGTH_LONG).show()
                        stopSelf()
                        return START_NOT_STICKY
                    }

                    // 1. Start as Foreground Service
                    startForeground(NOTIFICATION_ID, createNotification(), getForegroundServiceType())

                    // 2. Get MediaProjection instance
                    mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)

                    // 3. Start the Placeholder Server
                    mediaProjection?.let { projection ->
                        screenCastServer = ScreenCastServer(this, projection)
                        screenCastServer?.start()
                    } ?: run {
                        Log.e(LOG_TAG, "MediaProjection failed to start.")
                        Toast.makeText(this, "Casting failed: MediaProjection error.", Toast.LENGTH_LONG).show()
                        stopSelf()
                    }
                }
            }
            ACTION_STOP -> {
                stopCasting()
                stopSelf()
            }
        }
        return START_STICKY
    }

    /**
     * Handles the modern way of retrieving Parcelable Intent (data) from the service intent,
     * safely supporting older API levels.
     */
    private fun getDataIntentFromServiceIntent(intent: Intent): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_INTENT, Intent::class.java)
        } else {
            // Deprecated method for older APIs
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT_INTENT)
        }
    }

    override fun onDestroy() {
        stopCasting()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null // Not a bound service
    }

    // --- Private Methods ---

    private fun stopCasting() {
        screenCastServer?.stop()
        screenCastServer = null

        mediaProjection?.stop()
        mediaProjection = null

        // Ensure the foreground service is correctly stopped
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_DETACH)
        } else {
            // STOP_FOREGROUND_DETACH behavior approximated for older APIs
            @Suppress("DEPRECATION")
            stopForeground(false)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name), // Assuming this exists in R.string
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        // Intent to open the MainActivity when the notification is clicked
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Build the notification
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("${getString(R.string.app_name)} Active") // Assuming R.string.app_name exists
            .setContentText("Your screen is being cast. Tap to stop.")
            .setSmallIcon(android.R.drawable.ic_menu_rotate)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_delete, "Stop Casting",
                getStopPendingIntent()
            )
            .build()
    }

    private fun getStopPendingIntent(): PendingIntent {
        // Intent to send ACTION_STOP command to this service
        val stopIntent = Intent(this, ScreenCastService::class.java).apply {
            action = ACTION_STOP
        }

        return PendingIntent.getService(
            this, Random().nextInt(), stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    companion object {
        private const val LOG_TAG = "ScreenCastService"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "screencast_channel"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_INTENT = "extra_result_intent"
        const val ACTION_START = "action_start"
        const val ACTION_STOP = "action_stop"
    }
}