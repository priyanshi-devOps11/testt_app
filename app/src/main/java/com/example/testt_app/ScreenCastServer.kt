package com.example.testt_app

import android.content.Context
import android.media.projection.MediaProjection
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log

class ScreenCastServer(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {
    private var isStreaming = false

    fun start() {
        if (isStreaming) return

        Log.d(LOG_TAG, "ScreenCastServer starting on port $RTSP_PORT...")
        isStreaming = true

        // --- REAL-WORLD INTEGRATION POINT
        val streamUrl = "rtsp://${getIpAddress()}:$RTSP_PORT$STREAM_PATH"
        Log.i(LOG_TAG, "SERVER STARTED successfully!")
        Log.i(LOG_TAG, "Stream URL: $streamUrl. Use VLC/FFmpeg to view.")
    }

    fun stop() {
        if (!isStreaming) return

        Log.d(LOG_TAG, "ScreenCastServer stopping...")

        // --- REAL-WORLD INTEGRATION POINT: Stop streaming, shut down encoder, release resources ---
        mediaProjection.stop()
        isStreaming = false
        Log.i(LOG_TAG, "SERVER STOPPED.")
    }

    /**
     * Finds the device's local Wi-Fi IP address.
     */
    @Suppress("Deprecation")
    private fun getIpAddress(): String {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            val ipAddress = wifiManager.connectionInfo.ipAddress

            Formatter.formatIpAddress(ipAddress)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting IP address: ${e.message}")
            "127.0.0.1"
        }
    }

    companion object {
        private const val LOG_TAG = "ScreenCastServer"
        private const val RTSP_PORT = 8554
        private const val STREAM_PATH = "/screen"
    }
}