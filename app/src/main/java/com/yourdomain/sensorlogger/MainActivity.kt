package com.yourdomain.sensorlogger

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.yourdomain.sensorlogger.service.SensorLoggingService
import com.yourdomain.sensorlogger.util.PermissionUtils
import com.yourdomain.sensorlogger.util.SensorConfig
import com.yourdomain.sensorlogger.util.SensorDataManager

class MainActivity : AppCompatActivity() {

    private lateinit var startServiceButton: Button
    private lateinit var stopServiceButton: Button
    private lateinit var statusText: TextView
    private lateinit var accelerometerText: TextView
    private lateinit var gyroscopeText: TextView
    private lateinit var barometerText: TextView
    private lateinit var gpsText: TextView
    private lateinit var lastUpdateText: TextView
    
    private val handler = Handler(Looper.getMainLooper())
    private var lastAccelerometerData = "X=0.0, Y=0.0, Z=0.0"
    private var lastGyroscopeData = "X=0.0, Y=0.0, Z=0.0"
    private var lastBarometerData = "Pressure=0.0 hPa, Altitude=0.0m"
    private var lastGpsData = "Lat=0.0, Lon=0.0, Accuracy=0.0m"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Register with SensorDataManager
        SensorDataManager.setMainActivity(this)

        startServiceButton = findViewById(R.id.start_service_button)
        stopServiceButton = findViewById(R.id.stop_service_button)
        statusText = findViewById(R.id.status_text)
        accelerometerText = findViewById(R.id.accelerometer_text)
        gyroscopeText = findViewById(R.id.gyroscope_text)
        barometerText = findViewById(R.id.barometer_text)
        gpsText = findViewById(R.id.gps_text)
        lastUpdateText = findViewById(R.id.last_update_text)

        startServiceButton.setOnClickListener {
            if (PermissionUtils.allPermissionsGranted(this)) {
                startLoggingService()
            } else {
                PermissionUtils.requestPermissions(this)
            }
        }
        
        stopServiceButton.setOnClickListener {
            stopLoggingService()
        }
        
        // Start real-time sensor data display if enabled
        if (SensorConfig.SHOW_SENSOR_DATA_ON_SCREEN) {
            startSensorDataDisplay()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
        SensorDataManager.setMainActivity(this)
    }
    
    override fun onPause() {
        super.onPause()
        SensorDataManager.setMainActivity(null)
    }

    private fun startLoggingService() {
        val serviceIntent = Intent(this, SensorLoggingService::class.java)
        startService(serviceIntent)
        statusText.text = "Logging service started."
    }

    private fun stopLoggingService() {
        val serviceIntent = Intent(this, SensorLoggingService::class.java)
        stopService(serviceIntent)
        statusText.text = "Logging service stopped."
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
    
    private fun startSensorDataDisplay() {
        // Update sensor data display every 500ms
        handler.postDelayed(object : Runnable {
            override fun run() {
                updateSensorDataDisplay()
                handler.postDelayed(this, 500)
            }
        }, 500)
    }
    
    private fun updateSensorDataDisplay() {
        // Update accelerometer display
        accelerometerText.text = "Accelerometer: $lastAccelerometerData"
        
        // Update gyroscope display
        gyroscopeText.text = "Gyroscope: $lastGyroscopeData"
        
        // Update barometer display
        barometerText.text = "Barometer: $lastBarometerData"
        
        // Update GPS display
        gpsText.text = "GPS: $lastGpsData"
        
        // Update last update time
        val currentTime = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        lastUpdateText.text = "Last Update: $currentTime"
    }
    
    // Public methods to update sensor data from service
    fun updateAccelerometerData(x: Float, y: Float, z: Float) {
        lastAccelerometerData = "X=${String.format("%.3f", x)}, Y=${String.format("%.3f", y)}, Z=${String.format("%.3f", z)}"
    }
    
    fun updateGyroscopeData(x: Float, y: Float, z: Float) {
        lastGyroscopeData = "X=${String.format("%.6f", x)}, Y=${String.format("%.6f", y)}, Z=${String.format("%.6f", z)}"
    }
    
    fun updateBarometerData(pressure: Float, altitude: Float) {
        lastBarometerData = "Pressure=${String.format("%.1f", pressure)} hPa, Altitude=${String.format("%.1f", altitude)}m"
    }
    
    fun updateGpsData(lat: Double, lon: Double, accuracy: Float) {
        lastGpsData = "Lat=${String.format("%.6f", lat)}, Lon=${String.format("%.6f", lon)}, Accuracy=${String.format("%.1f", accuracy)}m"
    }
} 