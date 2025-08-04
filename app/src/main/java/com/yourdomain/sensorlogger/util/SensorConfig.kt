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
    
    // Sensor polling rates (in microseconds) - REDUCED FREQUENCY
    // Available options:
    // SENSOR_DELAY_FASTEST = 0 (as fast as possible)
    // SENSOR_DELAY_GAME = 20000 (50Hz)
    // SENSOR_DELAY_UI = 66667 (15Hz)
    // SENSOR_DELAY_NORMAL = 200000 (5Hz)
    // Custom delay = 1000000 (1Hz - every second)
    // Custom delay = 2000000 (0.5Hz - every 2 seconds)
    
    // Accelerometer polling rate - REDUCED from 2Hz to 0.5Hz
    val ACCELEROMETER_DELAY = 2000000  // 0.5Hz (2 seconds) - every 2 seconds
    
    // Gyroscope polling rate - REDUCED from 2Hz to 0.5Hz
    val GYROSCOPE_DELAY = 2000000      // 0.5Hz (2 seconds) - every 2 seconds
    
    // Barometer polling rate - REDUCED from 2Hz to 0.5Hz
    val BAROMETER_DELAY = 2000000      // 0.5Hz (2 seconds) - every 2 seconds
    
    // GPS location update interval (in milliseconds) - INCREASED
    val GPS_UPDATE_INTERVAL = 30000L      // 30 seconds (was 10 seconds)
    val GPS_FASTEST_INTERVAL = 15000L     // 15 seconds (was 5 seconds)
    
    // Audio recording settings
    val AUDIO_SAMPLE_RATE = 44100         // Hz
    val AUDIO_CHANNEL_CONFIG = 1          // Mono
    val AUDIO_ENCODING = 2                // PCM_16BIT
    
    // Photo capture interval (in milliseconds) - INCREASED
    val PHOTO_INTERVAL = 60 * 60 * 1000L  // 60 minutes (was 30 minutes)
    
    // Data upload interval (in milliseconds) - TESTING: 2 minutes
    val UPLOAD_INTERVAL = 2 * 60 * 1000L // 2 minutes (TESTING ONLY - reset to 60 minutes later)
    
    // Data batching settings - NEW
    val BATCH_SIZE = 50                   // Number of sensor readings to batch before processing
    val BATCH_TIMEOUT = 30000L            // 30 seconds - flush batch even if not full
    val ENABLE_DATA_BATCHING = true       // Enable batching to reduce frequency
    
    // Data filtering settings - NEW
    val ENABLE_MOTION_FILTERING = true    // Only log when significant motion detected
    val MOTION_THRESHOLD = 0.1f           // Minimum acceleration change to trigger logging
    val ENABLE_DUPLICATE_FILTERING = true // Filter out duplicate readings
    
    // Wake lock timeout (in milliseconds) - 0 = no timeout
    val WAKE_LOCK_TIMEOUT = 0L           // No timeout (keep alive indefinitely)
    
    // Logging settings
    val ENABLE_VERBOSE_LOGGING = false    // Set to false to reduce log output
    val LOG_SENSOR_DATA = false           // Set to false to disable sensor data logging
    
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