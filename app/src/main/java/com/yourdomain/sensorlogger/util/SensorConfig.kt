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
    
    // Sensor polling rates (in microseconds) - INCREASED FREQUENCY
    // Available options:
    // SENSOR_DELAY_FASTEST = 0 (as fast as possible)
    // SENSOR_DELAY_GAME = 20000 (50Hz)
    // SENSOR_DELAY_UI = 66667 (15Hz)
    // SENSOR_DELAY_NORMAL = 200000 (5Hz)
    // Custom delay = 1000000 (1Hz - every second)
    // Custom delay = 2000000 (0.5Hz - every 2 seconds)
    
    // Accelerometer polling rate - CHANGED to 1Hz for consistency
    val ACCELEROMETER_DELAY = 1000000  // 1Hz (1 second) - every second
    
    // Gyroscope polling rate - CHANGED to 1Hz for consistency
    val GYROSCOPE_DELAY = 1000000      // 1Hz (1 second) - every second
    
    // Barometer polling rate - KEPT at 1Hz for consistency
    val BAROMETER_DELAY = 1000000    // 1Hz (1 second) - every second
    
    // GPS location update interval (in milliseconds) - KEPT reasonable
    val GPS_UPDATE_INTERVAL = 30000L      // 30 seconds
    val GPS_FASTEST_INTERVAL = 15000L     // 15 seconds
    
    // Audio recording settings
    val AUDIO_SAMPLE_RATE = 44100         // Hz
    val AUDIO_CHANNEL_CONFIG = 1          // Mono
    val AUDIO_ENCODING = 2                // PCM_16BIT
    
    // Photo capture interval (in milliseconds) - KEPT reasonable
    val PHOTO_INTERVAL = 60 * 60 * 1000L  // 60 minutes
    
    // Data upload interval (in milliseconds) - REDUCED for testing
    val UPLOAD_INTERVAL = 30 * 1000L // 30 seconds (TESTING - for immediate feedback)
    
    // Data batching settings - ADJUSTED for continuous data
    val BATCH_SIZE = 10                   // Reduced - process smaller batches
    val BATCH_TIMEOUT = 10000L            // 10 seconds - flush batch more frequently
    val ENABLE_DATA_BATCHING = true       // Enable batching to reduce frequency
    
    // Data filtering settings - RELAXED for continuous data
    val ENABLE_MOTION_FILTERING = false   // DISABLED - log all readings for testing
    val MOTION_THRESHOLD = 0.01f          // Much lower threshold (was 0.1f)
    val ENABLE_DUPLICATE_FILTERING = false // DISABLED - log all readings for testing
    
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