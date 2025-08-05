package com.yourdomain.sensorlogger.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.yourdomain.sensorlogger.data.DataRepository
import com.yourdomain.sensorlogger.data.models.BarometerData
import com.yourdomain.sensorlogger.util.SensorConfig
import com.yourdomain.sensorlogger.util.SensorDataManager

class BarometerController(context: Context, private val dataRepository: DataRepository) : SensorEventListener {
    private val TAG = "BarometerController"
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var pressureSensor: Sensor? = null
    
    // Store current barometer data for unified records
    private var currentPressure: Float? = null
    private var currentAltitude: Float? = null

    init {
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    }

    fun start() {
        if (SensorConfig.ENABLE_BAROMETER) {
            pressureSensor?.also { sensor ->
                sensorManager.registerListener(this, sensor, SensorConfig.BAROMETER_DELAY)
                Log.d(TAG, "Barometer listener registered with configurable rate")
                
                // Immediately request a reading to get initial data
                // This ensures we have barometer data in the first packet
                sensorManager.registerListener(object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
                            currentPressure = event.values[0]
                            currentAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure!!)
                            Log.d(TAG, "Initial barometer reading: pressure=${currentPressure}, altitude=${currentAltitude}")
                            sensorManager.unregisterListener(this)
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        Log.d(TAG, "Barometer listener unregistered")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
            currentPressure = event.values[0]
            currentAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure!!)
            
            // Send to UI
            SensorDataManager.updateBarometerData(currentPressure!!, currentAltitude!!)
            
            Log.d(TAG, "Barometer data updated: pressure=${currentPressure}, altitude=${currentAltitude}")
        }
    }
    
    // Get current barometer data for unified records
    fun getCurrentPressure(): Float? = currentPressure
    fun getCurrentAltitude(): Float? = currentAltitude

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
} 