package com.yourdomain.sensorlogger.data.models

/**
 * UnifiedSensorRecord: Combines all sensor data into a single record
 * This reduces null values by merging data from different sensors within time windows
 */
data class UnifiedSensorRecord(
    val timestamp: Long,
    val gyroX: Float?,
    val gyroY: Float?,
    val gyroZ: Float?,
    val accelX: Float?,
    val accelY: Float?,
    val accelZ: Float?,
    val latitude: Double?,
    val longitude: Double?,
    val accuracy: Float?,
    val pressure: Float?,
    val altitude: Float?,
    val deviceId: String,
    val type: String = "unified" // To identify this as a unified record
) 