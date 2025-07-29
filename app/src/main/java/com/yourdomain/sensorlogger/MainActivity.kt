package com.yourdomain.sensorlogger

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yourdomain.sensorlogger.service.SensorLoggingService
import com.yourdomain.sensorlogger.util.PermissionUtils

class MainActivity : AppCompatActivity() {

    private lateinit var startServiceButton: Button
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startServiceButton = findViewById(R.id.start_service_button)
        statusText = findViewById(R.id.status_text)

        startServiceButton.setOnClickListener {
            if (PermissionUtils.allPermissionsGranted(this)) {
                startLoggingService()
            } else {
                PermissionUtils.requestPermissions(this)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun startLoggingService() {
        val serviceIntent = Intent(this, SensorLoggingService::class.java)
        startService(serviceIntent)
        statusText.text = "Logging service started."
    }

    private fun updateUI() {
        if (PermissionUtils.allPermissionsGranted(this)) {
            statusText.text = "Permissions granted. Ready to start."
            startServiceButton.text = "Start Logging Service"
        } else {
            statusText.text = "Permissions not granted."
            startServiceButton.text = "Grant Permissions"
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101) {
            updateUI()
        }
    }
} 