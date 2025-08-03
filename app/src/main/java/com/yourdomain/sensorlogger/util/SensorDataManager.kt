package com.yourdomain.sensorlogger.util

import android.os.Handler
import android.os.Looper
import com.yourdomain.sensorlogger.MainActivity

/**
 * Singleton to manage sensor data and communicate with MainActivity
 */
object SensorDataManager {
    
    private var mainActivity: MainActivity? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    fun setMainActivity(activity: MainActivity?) {
        mainActivity = activity
    }
    
    fun updateAccelerometerData(x: Float, y: Float, z: Float) {
        mainHandler.post {
            mainActivity?.updateAccelerometerData(x, y, z)
        }
    }
    
    fun updateGyroscopeData(x: Float, y: Float, z: Float) {
        mainHandler.post {
            mainActivity?.updateGyroscopeData(x, y, z)
        }
    }
    
    fun updateBarometerData(pressure: Float, altitude: Float) {
        mainHandler.post {
            mainActivity?.updateBarometerData(pressure, altitude)
        }
    }
    
    fun updateGpsData(lat: Double, lon: Double, accuracy: Float) {
        mainHandler.post {
            mainActivity?.updateGpsData(lat, lon, accuracy)
        }
    }
} 