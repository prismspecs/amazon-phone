package com.yourdomain.sensorlogger.data.models

// PhotoData: Holds timestamp and file path
// Extend as needed for additional photo fields
data class PhotoData(
    val timestamp: Long,
    val filePath: String
) 