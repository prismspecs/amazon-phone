package com.yourdomain.sensorlogger.data.models

// SensorData: Holds timestamp, gyro, and accelerometer data
// Extend as needed for additional sensor fields
data class SensorData(
    val timestamp: Long,
    val gyroX: Float?,
    val gyroY: Float?,
    val gyroZ: Float?,
    val accelX: Float?,
    val accelY: Float?,
    val accelZ: Float?
) 