package com.yourdomain.sensorlogger.data.models

// AudioData: Holds timestamp, file path, and duration
// Extend as needed for additional audio fields
data class AudioData(
    val timestamp: Long,
    val filePath: String,
    val durationMs: Long
) 