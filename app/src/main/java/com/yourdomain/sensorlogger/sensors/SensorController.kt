package com.yourdomain.sensorlogger.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.yourdomain.sensorlogger.data.DataRepository
import com.yourdomain.sensorlogger.data.models.SensorData

class SensorController(context: Context, private val dataRepository: DataRepository) : SensorEventListener {
    private val TAG = "SensorController"
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gyroSensor: Sensor? = null
    private var accelSensor: Sensor? = null

    init {
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    fun start() {
        gyroSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        accelSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        Log.d(TAG, "Sensor listeners registered")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        Log.d(TAG, "Sensor listeners unregistered")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

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
            }
            Sensor.TYPE_ACCELEROMETER -> {
                accelX = event.values[0]
                accelY = event.values[1]
                accelZ = event.values[2]
            }
        }
        
        val sensorData = SensorData(timestamp, gyroX, gyroY, gyroZ, accelX, accelY, accelZ)
        dataRepository.addSensorData(sensorData) // Assuming this method exists
        Log.d(TAG, "Sensor data recorded: $sensorData")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
} 