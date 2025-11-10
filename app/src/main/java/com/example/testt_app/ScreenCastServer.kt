package com.example.test_app

import android.content.Context
import android.media.projection.MediaProjection
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log

/**
 * PLACEHOLDER for the RTSP Server:
 * This class simulates the highly complex integration of a streaming library.
 * In a real app, this is where you would connect MediaProjection's output
 * to an H.264 encoder and stream it over a third-party RTSP server library.
 */
class ScreenCastServer(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {
    // --- Properties ---
    private var isStreaming = false

    // --- Methods ---

    fun start() {
        if (isStreaming) return

        Log.d(LOG_TAG, "ScreenCastServer starting on port $RTSP_PORT...")
        isStreaming = true

        // --- REAL-WORLD INTEGRATION POINT: Connect MediaProjection to encoder and start streaming ---
        val streamUrl = "rtsp://${getIpAddress()}:$RTSP_PORT$STREAM_PATH"
        Log.i(LOG_TAG, "SERVER STARTED successfully!")
        Log.i(LOG_TAG, "Stream URL: $streamUrl. Use VLC/FFmpeg to view.")
    }

    fun stop() {
        if (!isStreaming) return

        Log.d(LOG_TAG, "ScreenCastServer stopping...")

        // --- REAL-WORLD INTEGRATION POINT: Stop streaming, shut down encoder, release resources ---
        mediaProjection.stop() // Release MediaProjection resources
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
            // Suppress deprecation warning for older API levels, though the modern alternative is complex.
            val ipAddress = wifiManager.connectionInfo.ipAddress
            // The Formatter function now accepts an Int directly
            Formatter.formatIpAddress(ipAddress)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting IP address: ${e.message}")
            "127.0.0.1"
        }
    }

    // --- Companion Object for Static Fields/Constants ---
    companion object {
        private const val LOG_TAG = "ScreenCastServer"
        private const val RTSP_PORT = 8554
        private const val STREAM_PATH = "/screen"
    }
}