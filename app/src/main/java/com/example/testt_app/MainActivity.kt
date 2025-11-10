package com.example.testt_app

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

    // Initialize UI components
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

        mediaProjectionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // Permission granted!
                startScreenCastService(result.resultCode, result.data!!) // !! used after null check
            } else {
                // Permission denied
                Toast.makeText(this, "Screen Capture Permission Denied", Toast.LENGTH_SHORT).show()
                updateUI(false)
            }
        }
        val ipAddress = getLocalIpAddress()
        tvIpAddress.text = getString(R.string.ip_placeholder).replace("Finding...", ipAddress)
        tvStreamUrl.text = getString(R.string.url_placeholder).replace("[IP_ADDRESS]", ipAddress)

        btnStartCast.setOnClickListener { requestScreenCapturePermission() }
        btnStopCast.setOnClickListener { stopScreenCastService() }


        updateUI(false)
    }



    @Suppress("Deprecation")
    private fun getLocalIpAddress(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            val ipAddress = wifiManager.connectionInfo.ipAddress

            Formatter.formatIpAddress(ipAddress)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Error getting IP address: ${e.message}")
            "127.0.0.1"
        }
    }

    private fun requestScreenCapturePermission() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        mediaProjectionLauncher.launch(manager.createScreenCaptureIntent())
    }


    private fun startScreenCastService(resultCode: Int, resultData: Intent) {
        if (isCasting) return

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


    private fun stopScreenCastService() {
        if (!isCasting) return

        val intent = Intent(this, ScreenCastService::class.java).apply {
            action = ScreenCastService.ACTION_STOP
        }
        startService(intent)
        updateUI(false)
        Toast.makeText(this, "Casting stopped.", Toast.LENGTH_SHORT).show()
    }

    private fun updateUI(casting: Boolean) {
        isCasting = casting
        btnStartCast.isEnabled = !casting // Enable Start only if not casting
        btnStopCast.isEnabled = casting   // Enable Stop only if casting

        tvStatus.setText(if (casting) R.string.status_casting else R.string.status_ready)

        val colorHex = if (casting) "#FF9800" else "#388E3C"

        val colorInt = Integer.decode(colorHex)
        tvStatus.setTextColor(colorInt)
    }
}