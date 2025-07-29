package com.yourdomain.sensorlogger.data.models

// LocationData: Holds timestamp, latitude, longitude, and accuracy
// Extend as needed for additional location fields
data class LocationData(
    val timestamp: Long,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?
) 