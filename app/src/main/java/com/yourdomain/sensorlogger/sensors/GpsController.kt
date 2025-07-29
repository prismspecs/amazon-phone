package com.yourdomain.sensorlogger.sensors

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.yourdomain.sensorlogger.data.DataRepository
import com.yourdomain.sensorlogger.data.models.LocationData

class GpsController(
    private val context: Context,
    private val dataRepository: DataRepository
) {
    private val TAG = "GpsController"
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private lateinit var locationCallback: LocationCallback

    @SuppressLint("MissingPermission")
    fun start() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    val locationData = LocationData(
                        System.currentTimeMillis(),
                        location.latitude,
                        location.longitude,
                        location.accuracy
                    )
                    dataRepository.addLocationData(locationData) // Assuming this method exists
                    Log.d(TAG, "Location recorded: $locationData")
                }
            }
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        Log.d(TAG, "GPS location updates started")
    }

    fun stop() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "GPS location updates stopped")
    }
} 