package com.yourdomain.sensorlogger.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.yourdomain.sensorlogger.data.DataRepository
import com.yourdomain.sensorlogger.data.models.BarometerData

class BarometerController(context: Context, private val dataRepository: DataRepository) : SensorEventListener {
    private val TAG = "BarometerController"
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var pressureSensor: Sensor? = null

    init {
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    }

    fun start() {
        pressureSensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            Log.d(TAG, "Barometer listener registered")
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        Log.d(TAG, "Barometer listener unregistered")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            val pressure = event.values[0]
            val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressure)
            val barometerData = BarometerData(System.currentTimeMillis(), pressure, altitude)
            dataRepository.addBarometerData(barometerData) // Assuming this method exists
            Log.d(TAG, "Barometer data recorded: $barometerData")
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
} 