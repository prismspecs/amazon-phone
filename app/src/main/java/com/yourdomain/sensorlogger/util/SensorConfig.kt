package com.yourdomain.sensorlogger.util

import android.hardware.SensorManager

/**
 * Configuration file for sensor logging settings
 * Modify these values to adjust polling rates and behavior
 */
object SensorConfig {
    
    // Sensor polling rates (in microseconds)
    // Available options:
    // SENSOR_DELAY_FASTEST = 0 (as fast as possible)
    // SENSOR_DELAY_GAME = 20000 (50Hz)
    // SENSOR_DELAY_UI = 66667 (15Hz)
    // SENSOR_DELAY_NORMAL = 200000 (5Hz)
    
    // Sensor enable/disable flags
    val ENABLE_ACCELEROMETER = true
    val ENABLE_GYROSCOPE = true
    val ENABLE_BAROMETER = true
    val ENABLE_GPS = true
    val ENABLE_AUDIO = true
    val ENABLE_CAMERA = false  // Disabled due to service complexity
    
    // Sensor polling rates (in microseconds)
    // Available options:
    // SENSOR_DELAY_FASTEST = 0 (as fast as possible)
    // SENSOR_DELAY_GAME = 20000 (50Hz)
    // SENSOR_DELAY_UI = 66667 (15Hz)
    // SENSOR_DELAY_NORMAL = 200000 (5Hz)
    // Custom delay = 500000 (2Hz - every half second)
    
    // Accelerometer polling rate
    val ACCELEROMETER_DELAY = 500000  // 2Hz (500ms) - every half second
    
    // Gyroscope polling rate
    val GYROSCOPE_DELAY = 500000      // 2Hz (500ms) - every half second
    
    // Barometer polling rate
    val BAROMETER_DELAY = 500000      // 2Hz (500ms) - every half second
    
    // GPS location update interval (in milliseconds)
    val GPS_UPDATE_INTERVAL = 10000L      // 10 seconds
    val GPS_FASTEST_INTERVAL = 5000L      // 5 seconds
    
    // Audio recording settings
    val AUDIO_SAMPLE_RATE = 44100         // Hz
    val AUDIO_CHANNEL_CONFIG = 1          // Mono
    val AUDIO_ENCODING = 2                // PCM_16BIT
    
    // Photo capture interval (in milliseconds)
    val PHOTO_INTERVAL = 30 * 60 * 1000L  // 30 minutes
    
    // Data upload interval (in milliseconds)
    val UPLOAD_INTERVAL = 30 * 60 * 1000L // 30 minutes
    
    // Wake lock timeout (in milliseconds) - 0 = no timeout
    val WAKE_LOCK_TIMEOUT = 0L           // No timeout (keep alive indefinitely)
    
    // Logging settings
    val ENABLE_VERBOSE_LOGGING = true     // Set to false to reduce log output
    val LOG_SENSOR_DATA = true            // Set to false to disable sensor data logging
    
    // Display settings
    val SHOW_SENSOR_DATA_ON_SCREEN = true // Show real-time sensor data on phone screen
    
    // File storage settings
    val MAX_LOG_FILE_SIZE = 100 * 1024 * 1024L  // 100MB max file size
    val LOG_FILE_PREFIX = "sensor_log_"
    
    // Battery optimization settings
    val IGNORE_BATTERY_OPTIMIZATION = true  // Request to ignore battery optimization
    
    // Sensor accuracy requirements
    val GPS_ACCURACY = com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
} 