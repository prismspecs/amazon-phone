package com.yourdomain.sensorlogger.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.yourdomain.sensorlogger.data.DataRepository
import com.yourdomain.sensorlogger.data.models.UnifiedSensorRecord
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
    
    // Reference to barometer controller for unified data
    private var barometerController: BarometerController? = null
    private var gpsController: GpsController? = null
    
    // Unified data collection
    private var currentGyroX: Float? = null
    private var currentGyroY: Float? = null
    private var currentGyroZ: Float? = null
    private var currentAccelX: Float? = null
    private var currentAccelY: Float? = null
    private var currentAccelZ: Float? = null
    
    // Timer for sending unified packets
    private val packetExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var lastPacketTime = 0L

    init {
        gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }
    
    // Set barometer controller reference
    fun setBarometerController(controller: BarometerController) {
        barometerController = controller
    }
    
    // Set GPS controller reference
    fun setGpsController(controller: GpsController) {
        gpsController = controller
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
        
        // Immediately send first packet with whatever data we have
        // This ensures we don't wait for the slowest sensor
        packetExecutor.execute {
            sendUnifiedPacket()
        }
        
        // Start sending unified packets every second
        packetExecutor.scheduleAtFixedRate({
            sendUnifiedPacket()
        }, 1000, 1000, TimeUnit.MILLISECONDS)
        
        Log.d(TAG, "Sensor controller started with immediate first packet")
    }

    fun stop() {
        sensorManager.unregisterListener(this)
        packetExecutor.shutdown()
        try {
            if (!packetExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                packetExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            packetExecutor.shutdownNow()
        }
        Log.d(TAG, "Sensor controller stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                currentGyroX = event.values[0]
                currentGyroY = event.values[1]
                currentGyroZ = event.values[2]
                
                // Update barometer controller with motion data for fusion
                barometerController?.updateMotionData(currentAccelX, currentAccelY, currentAccelZ, currentGyroX, currentGyroY, currentGyroZ)
                
                // Update UI
                if (SensorConfig.SHOW_SENSOR_DATA_ON_SCREEN) {
                    SensorDataManager.updateGyroscopeData(currentGyroX!!, currentGyroY!!, currentGyroZ!!)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                currentAccelX = event.values[0]
                currentAccelY = event.values[1]
                currentAccelZ = event.values[2]
                
                // Update barometer controller with motion data for fusion
                barometerController?.updateMotionData(currentAccelX, currentAccelY, currentAccelZ, currentGyroX, currentGyroY, currentGyroZ)
                
                // Update UI
                if (SensorConfig.SHOW_SENSOR_DATA_ON_SCREEN) {
                    SensorDataManager.updateAccelerometerData(currentAccelX!!, currentAccelY!!, currentAccelZ!!)
                }
            }
        }
    }

    private fun sendUnifiedPacket() {
        val currentTime = System.currentTimeMillis()
        
        // Get barometer data if available
        val currentPressure = barometerController?.getCurrentPressure()
        val currentAltitude = barometerController?.getCurrentAltitude()
        
        // Get GPS data if available
        val currentLatitude = gpsController?.getCurrentLatitude()
        val currentLongitude = gpsController?.getCurrentLongitude()
        val currentAccuracy = gpsController?.getCurrentAccuracy()
        
        // Only send if we have at least some sensor data
        if (currentGyroX != null || currentAccelX != null || currentPressure != null || currentLatitude != null) {
            val unifiedRecord = UnifiedSensorRecord(
                timestamp = currentTime,
                gyroX = currentGyroX,
                gyroY = currentGyroY,
                gyroZ = currentGyroZ,
                accelX = currentAccelX,
                accelY = currentAccelY,
                accelZ = currentAccelZ,
                latitude = currentLatitude,
                longitude = currentLongitude,
                accuracy = currentAccuracy,
                pressure = currentPressure,
                altitude = currentAltitude,
                deviceId = "android-${android.os.Build.SERIAL}"
            )
            
            // Add to repository
            dataRepository.addUnifiedRecord(unifiedRecord)
            
            if (SensorConfig.LOG_SENSOR_DATA) {
                Log.d(TAG, "Unified packet sent: gyro=(${currentGyroX},${currentGyroY},${currentGyroZ}), accel=(${currentAccelX},${currentAccelY},${currentAccelZ}), pressure=${currentPressure}, altitude=${currentAltitude}, lat=${currentLatitude}, lon=${currentLongitude}, acc=${currentAccuracy}")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not used
    }
} 