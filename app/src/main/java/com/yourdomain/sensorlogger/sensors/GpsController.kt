package com.yourdomain.sensorlogger.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.yourdomain.sensorlogger.data.DataRepository
import com.yourdomain.sensorlogger.data.models.LocationData
import com.yourdomain.sensorlogger.util.SensorConfig
import com.yourdomain.sensorlogger.util.SensorDataManager

class GpsController(
    private val context: Context,
    private val dataRepository: DataRepository
) {
    private val TAG = "GpsController"
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private lateinit var locationCallback: LocationCallback
    
    // Store current GPS data for unified records
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null
    private var currentAccuracy: Float? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (!SensorConfig.ENABLE_GPS) {
            Log.d(TAG, "GPS disabled in config")
            return
        }
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    currentLatitude = location.latitude
                    currentLongitude = location.longitude
                    currentAccuracy = location.accuracy
                    
                    // Send to UI
                    SensorDataManager.updateGpsData(currentLatitude!!, currentLongitude!!, currentAccuracy!!)
                    
                    Log.d(TAG, "GPS data updated: lat=${currentLatitude}, lon=${currentLongitude}, accuracy=${currentAccuracy}")
                }
            }
        }

        val locationRequest = LocationRequest.create().apply {
            interval = SensorConfig.GPS_UPDATE_INTERVAL
            fastestInterval = SensorConfig.GPS_FASTEST_INTERVAL
            priority = SensorConfig.GPS_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        Log.d(TAG, "GPS location updates started")
    }
    
    // Get current GPS data for unified records
    fun getCurrentLatitude(): Double? = currentLatitude
    fun getCurrentLongitude(): Double? = currentLongitude
    fun getCurrentAccuracy(): Float? = currentAccuracy

    fun stop() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "GPS location updates stopped")
    }
} 