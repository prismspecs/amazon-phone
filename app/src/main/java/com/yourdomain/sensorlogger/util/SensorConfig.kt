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
    
    // UNIFIED TIMING SYSTEM
    // All data should adhere to standard timing intervals:
    // - Gyro/Accel/Baro: Every second (1Hz)
    // - GPS: Every 30 seconds
// - Upload: Every 30 seconds
// - Audio: Covers same period as upload interval (30-second cycles)
    
    // Main timing configuration - CHANGE THIS TO MODIFY ALL TIMING
    val MAIN_INTERVAL_SECONDS = 30L       // Main interval for all operations (30 seconds)
    val MAIN_INTERVAL_MS = MAIN_INTERVAL_SECONDS * 1000L

    // High-frequency sensors (every second)
    val ACCELEROMETER_DELAY = 1000000  // 1Hz (1 second)
    val GYROSCOPE_DELAY = 1000000      // 1Hz (1 second)
    val BAROMETER_DELAY = 1000000      // 1Hz (1 second)
    
    // GPS location update interval (every 30 seconds)
    val GPS_UPDATE_INTERVAL = MAIN_INTERVAL_MS      // 30 seconds
    val GPS_FASTEST_INTERVAL = MAIN_INTERVAL_MS    // 30 seconds

    // Upload interval (every 30 seconds)
    val UPLOAD_INTERVAL = MAIN_INTERVAL_MS         // 30 seconds
    
    // Audio recording settings
    val AUDIO_SAMPLE_RATE = 44100         // Hz
    val AUDIO_CHANNEL_CONFIG = 1          // Mono
    val AUDIO_ENCODING = 2                // PCM_16BIT
    
    // Photo capture interval (in milliseconds) - KEPT reasonable
    val PHOTO_INTERVAL = 60 * 60 * 1000L  // 60 minutes

    // Data batching settings - DERIVED from main interval
    val BATCH_SIZE = MAIN_INTERVAL_SECONDS.toInt()  // 30 seconds worth of data (30 records at 1Hz)
    val BATCH_TIMEOUT = MAIN_INTERVAL_MS            // 30 seconds - flush batch every 30 seconds
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