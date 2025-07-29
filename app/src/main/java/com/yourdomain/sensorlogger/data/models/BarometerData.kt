package com.yourdomain.sensorlogger.data.models

// BarometerData: Holds timestamp, pressure, and altitude
// Extend as needed for additional barometer fields
data class BarometerData(
    val timestamp: Long,
    val pressure: Float?,
    val altitude: Float?
) 