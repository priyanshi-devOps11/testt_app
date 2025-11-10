package com.example.test_app

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.testt_app.R

class MainActivity : AppCompatActivity() {

    private val LOG_TAG = "MainActivity"

    // Initialize UI components using lazy delegation (or standard findViewById if preferred)
    private val btnStartCast: Button by lazy { findViewById(R.id.btnStartCast) }
    private val btnStopCast: Button by lazy { findViewById(R.id.btnStopCast) }
    private val tvIpAddress: TextView by lazy { findViewById(R.id.tvIpAddress) }
    private val tvStreamUrl: TextView by lazy { findViewById(R.id.tvStreamUrl) }
    private val tvStatus: TextView by lazy { findViewById(R.id.tvStatus) }

    private var isCasting = false
    private lateinit var mediaProjectionLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Register for MediaProjection result early
        mediaProjectionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // Permission granted! Start the service with the intent data.
                startScreenCastService(result.resultCode, result.data!!) // !! used after null check
            } else {
                // Permission denied
                Toast.makeText(this, "Screen Capture Permission Denied", Toast.LENGTH_SHORT).show()
                updateUI(false)
            }
        }

        // 2. Display Network Info (UI components are initialized lazily upon first access)
        val ipAddress = getLocalIpAddress()
        tvIpAddress.text = getString(R.string.ip_placeholder).replace("Finding...", ipAddress)
        tvStreamUrl.text = getString(R.string.url_placeholder).replace("[IP_ADDRESS]", ipAddress)

        // 3. Set up button listeners using concise lambda syntax
        btnStartCast.setOnClickListener { requestScreenCapturePermission() }
        btnStopCast.setOnClickListener { stopScreenCastService() }

        // 4. Initialize UI state
        updateUI(false)
    }


    /**
     * Finds the device's local Wi-Fi IP address.
     */
    @Suppress("Deprecation")
    private fun getLocalIpAddress(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            // Suppress deprecation warning for older API levels, though the modern alternative is complex.
            val ipAddress = wifiManager.connectionInfo.ipAddress
            // The Formatter function now accepts an Int directly
            Formatter.formatIpAddress(ipAddress)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting IP address: ${e.message}")
            "127.0.0.1"
        }
    }



    /**
     * Initiates the standard Android screen capture permission request.
     */
    private fun requestScreenCapturePermission() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // createScreenCaptureIntent() shows the system permission dialog
        mediaProjectionLauncher.launch(manager.createScreenCaptureIntent())
    }

    /**
     * Starts the Foreground Service responsible for casting, passing the permission result.
     */
    private fun startScreenCastService(resultCode: Int, resultData: Intent) {
        if (isCasting) return

        // Prepare Intent to pass MediaProjection data to the service
        val intent = Intent(this, ScreenCastService::class.java).apply {
            putExtra(ScreenCastService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenCastService.EXTRA_RESULT_INTENT, resultData)
            action = ScreenCastService.ACTION_START
        }

        // Start the service in foreground mode
        ContextCompat.startForegroundService(this, intent)
        updateUI(true)
        Toast.makeText(this, "Casting started! Use the URL shown.", Toast.LENGTH_LONG).show()
    }

    /**
     * Stops the Foreground Service.
     */
    private fun stopScreenCastService() {
        if (!isCasting) return

        val intent = Intent(this, ScreenCastService::class.java).apply {
            action = ScreenCastService.ACTION_STOP
        }
        startService(intent) // Send command to the service to stop
        updateUI(false)
        Toast.makeText(this, "Casting stopped.", Toast.LENGTH_SHORT).show()
    }




    /**
     * Updates the UI (buttons and status text) based on the casting state.
     */
    private fun updateUI(casting: Boolean) {
        isCasting = casting
        btnStartCast.isEnabled = !casting // Enable Start only if not casting
        btnStopCast.isEnabled = casting   // Enable Stop only if casting

        // Update status text and color
        tvStatus.setText(if (casting) R.string.status_casting else R.string.status_ready)

        // Use standard Kotlin syntax for color parsing
        val colorHex = if (casting) "#FF9800" else "#388E3C"
        // Integer.decode in Java is equivalent to Integer.parseUnsignedInt(hex.substring(1), 16) in modern Kotlin/Android,
        // but since we are using full hex (with alpha), Color.parseColor is often simpler and safer.
        // However, to strictly follow the original logic using Integer.decode:
        val colorInt = Integer.decode(colorHex)
        tvStatus.setTextColor(colorInt)
    }
}