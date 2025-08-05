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
import kotlin.math.abs
import kotlin.math.sqrt

class BarometerController(context: Context, private val dataRepository: DataRepository) : SensorEventListener {
    private val TAG = "BarometerController"
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var pressureSensor: Sensor? = null
    
    // Store current barometer data for unified records
    private var currentPressure: Float? = null
    private var currentAltitude: Float? = null
    
    // Sensor fusion variables for better altitude accuracy
    private var lastAltitude: Float? = null
    private var filteredAltitude: Float? = null
    private var lastAccelZ: Float? = null
    private var lastGyroMagnitude: Float? = null
    
    // Fusion thresholds and parameters
    private val ALTITUDE_JUMP_THRESHOLD = 2.0f  // meters - suspicious jump threshold
    private val ACCEL_MOVEMENT_THRESHOLD = 0.5f  // m/s² - significant vertical movement
    private val GYRO_MOVEMENT_THRESHOLD = 0.1f   // rad/s - significant rotation
    private val FILTER_ALPHA = 0.7f              // Smoothing factor (0.0 = no smoothing, 1.0 = no change)
    private val DRIFT_CORRECTION_RATE = 0.1f     // Slow drift correction when stationary

    init {
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
    }

    fun start() {
        if (SensorConfig.ENABLE_BAROMETER) {
            pressureSensor?.also { sensor ->
                sensorManager.registerListener(this, sensor, SensorConfig.BAROMETER_DELAY)
                Log.d(TAG, "Barometer listener registered with sensor fusion enabled")
                
                // Immediately request a reading to get initial data
                // This ensures we have barometer data in the first packet
                sensorManager.registerListener(object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event?.sensor?.type == Sensor.TYPE_PRESSURE) {
                            currentPressure = event.values[0]
                            currentAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, currentPressure!!)
                            filteredAltitude = currentAltitude
                            lastAltitude = currentAltitude
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
            val rawPressure = event.values[0]
            val rawAltitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, rawPressure)
            
            // Apply sensor fusion for better altitude accuracy
            val improvedAltitude = applySensorFusion(rawAltitude)
            
            currentPressure = rawPressure
            currentAltitude = improvedAltitude
            
            // Send to UI
            SensorDataManager.updateBarometerData(currentPressure!!, currentAltitude!!)
            
            Log.d(TAG, "Barometer data updated: raw_altitude=${rawAltitude}, improved_altitude=${currentAltitude}")
        }
    }
    
    /**
     * Apply sensor fusion to improve altitude accuracy using accel/gyro data
     * This function is called by SensorController to get current accel/gyro data
     */
    fun updateMotionData(accelX: Float?, accelY: Float?, accelZ: Float?, gyroX: Float?, gyroY: Float?, gyroZ: Float?) {
        // Store motion data for fusion (called from SensorController)
        lastAccelZ = accelZ
        lastGyroMagnitude = if (gyroX != null && gyroY != null && gyroZ != null) {
            sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ)
        } else null
    }
    
    /**
     * Apply sensor fusion logic to improve altitude accuracy
     */
    private fun applySensorFusion(rawAltitude: Float): Float {
        if (lastAltitude == null) {
            // First reading - initialize
            lastAltitude = rawAltitude
            filteredAltitude = rawAltitude
            return rawAltitude
        }
        
        val altitudeChange = rawAltitude - lastAltitude!!
        val isSignificantJump = abs(altitudeChange) > ALTITUDE_JUMP_THRESHOLD
        
        // Check if there's significant motion to validate the altitude change
        val hasVerticalMotion = lastAccelZ?.let { abs(it) > ACCEL_MOVEMENT_THRESHOLD } ?: false
        val hasRotation = lastGyroMagnitude?.let { it > GYRO_MOVEMENT_THRESHOLD } ?: false
        val hasMotion = hasVerticalMotion || hasRotation
        
        return when {
            // Case 1: Significant altitude jump with motion - likely real change
            isSignificantJump && hasMotion -> {
                Log.d(TAG, "Accepting altitude jump: ${altitudeChange}m with motion detected")
                lastAltitude = rawAltitude
                filteredAltitude = rawAltitude
                rawAltitude
            }
            
            // Case 2: Significant altitude jump without motion - likely noise/error
            isSignificantJump && !hasMotion -> {
                Log.d(TAG, "Filtering altitude jump: ${altitudeChange}m without motion - applying smoothing")
                val smoothedAltitude = filteredAltitude!! + (altitudeChange * (1.0f - FILTER_ALPHA))
                lastAltitude = rawAltitude
                filteredAltitude = smoothedAltitude
                smoothedAltitude
            }
            
            // Case 3: Small altitude change - apply light smoothing
            !isSignificantJump -> {
                val smoothedAltitude = filteredAltitude!! + (altitudeChange * (1.0f - FILTER_ALPHA))
                lastAltitude = rawAltitude
                filteredAltitude = smoothedAltitude
                smoothedAltitude
            }
            
            // Case 4: No motion detected - apply drift correction
            !hasMotion -> {
                Log.d(TAG, "Device stationary - applying drift correction")
                val driftCorrectedAltitude = filteredAltitude!! + (altitudeChange * DRIFT_CORRECTION_RATE)
                lastAltitude = rawAltitude
                filteredAltitude = driftCorrectedAltitude
                driftCorrectedAltitude
            }
            
            // Default case
            else -> {
                lastAltitude = rawAltitude
                filteredAltitude = rawAltitude
                rawAltitude
            }
        }
    }
    
    // Get current barometer data for unified records
    fun getCurrentPressure(): Float? = currentPressure
    fun getCurrentAltitude(): Float? = currentAltitude

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
} 