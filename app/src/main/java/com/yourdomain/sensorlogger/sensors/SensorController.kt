package com.yourdomain.sensorlogger.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.yourdomain.sensorlogger.data.DataRepository
import com.yourdomain.sensorlogger.data.models.SensorData
import com.yourdomain.sensorlogger.util.SensorConfig
import com.yourdomain.sensorlogger.util.SensorDataManager
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class SensorController(context: Context, private val dataRepository: DataRepository) : SensorEventListener {
    private val TAG = "SensorController"
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gyroSensor: Sensor? = null
    private var accelSensor: Sensor? = null
    
    // Throttling system
    private val throttleExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var lastProcessingTime = 0L
    private val minProcessingInterval = 1000L // Minimum 1 second between processing

    init {
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun start() {
        if (SensorConfig.ENABLE_GYROSCOPE) {
            gyroSensor?.also { sensor ->
                sensorManager.registerListener(this, sensor, SensorConfig.GYROSCOPE_DELAY)
                Log.d(TAG, "Gyroscope listener registered with ${SensorConfig.GYROSCOPE_DELAY}μs delay")
            }
        }
        if (SensorConfig.ENABLE_ACCELEROMETER) {
            accelSensor?.also { sensor ->
                sensorManager.registerListener(this, sensor, SensorConfig.ACCELEROMETER_DELAY)
                Log.d(TAG, "Accelerometer listener registered with ${SensorConfig.ACCELEROMETER_DELAY}μs delay")
            }
        }
        Log.d(TAG, "Sensor listeners registered with reduced frequency")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        throttleExecutor.shutdown()
        try {
            if (!throttleExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                throttleExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            throttleExecutor.shutdownNow()
        }
        Log.d(TAG, "Sensor listeners unregistered")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        val currentTime = System.currentTimeMillis()
        
        // Throttle processing to prevent excessive data delivery
        if (currentTime - lastProcessingTime < minProcessingInterval) {
            return // Skip processing if too soon
        }

        val timestamp = System.currentTimeMillis()
        var gyroX: Float? = null
        var gyroY: Float? = null
        var gyroZ: Float? = null
        var accelX: Float? = null
        var accelY: Float? = null
        var accelZ: Float? = null

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                gyroX = event.values[0]
                gyroY = event.values[1]
                gyroZ = event.values[2]
                // Send to UI only if enabled
                if (SensorConfig.SHOW_SENSOR_DATA_ON_SCREEN) {
                    SensorDataManager.updateGyroscopeData(gyroX, gyroY, gyroZ)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accelX = event.values[0]
                accelY = event.values[1]
                accelZ = event.values[2]
                // Send to UI only if enabled
                if (SensorConfig.SHOW_SENSOR_DATA_ON_SCREEN) {
                    SensorDataManager.updateAccelerometerData(accelX, accelY, accelZ)
                }
            }
        }
        
        val sensorData = SensorData(timestamp, gyroX, gyroY, gyroZ, accelX, accelY, accelZ)
        
        // Process data asynchronously to avoid blocking sensor thread
        throttleExecutor.execute {
            dataRepository.addSensorData(sensorData)
            lastProcessingTime = System.currentTimeMillis()
            
            if (SensorConfig.LOG_SENSOR_DATA) {
                Log.d(TAG, "Sensor data recorded: $sensorData")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
} 