package com.yourdomain.sensorlogger.data

import android.util.Log
import com.yourdomain.sensorlogger.data.models.*
import com.yourdomain.sensorlogger.util.SensorConfig
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// DataRepository: Handles storing, retrieving, and marking sensor/audio/location/photo data as sent
class DataRepository {
    private val TAG = "DataRepository"
    private val sensorDataQueue = ConcurrentLinkedQueue<SensorData>()
    private val locationDataQueue = ConcurrentLinkedQueue<LocationData>()
    private val barometerDataQueue = ConcurrentLinkedQueue<BarometerData>()
    private val audioDataQueue = ConcurrentLinkedQueue<AudioData>()
    private val photoDataQueue = ConcurrentLinkedQueue<PhotoData>()
    
    // Batching system
    private val batchExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private var lastAccelerometerData: SensorData? = null
    private var lastGyroscopeData: SensorData? = null
    
    init {
        if (SensorConfig.ENABLE_DATA_BATCHING) {
            // Schedule batch processing
            batchExecutor.scheduleAtFixedRate({
                processBatches()
            }, SensorConfig.BATCH_TIMEOUT, SensorConfig.BATCH_TIMEOUT, TimeUnit.MILLISECONDS)
        }
    }

    fun addSensorData(data: SensorData) {
        if (shouldFilterData(data)) {
            return
        }
        
        // Simply add to queue - no processing triggers
        sensorDataQueue.add(data)
    }

    fun addLocationData(data: LocationData) {
        // Simply add to queue - no processing triggers
        locationDataQueue.add(data)
    }

    fun addBarometerData(data: BarometerData) {
        // Simply add to queue - no processing triggers
        barometerDataQueue.add(data)
    }

    fun addAudioData(data: AudioData) {
        // Simply add to queue - no processing triggers
        audioDataQueue.add(data)
    }

    fun addPhotoData(data: PhotoData) {
        // Simply add to queue - no processing triggers
        photoDataQueue.add(data)
    }

    private fun shouldFilterData(data: SensorData): Boolean {
        if (!SensorConfig.ENABLE_MOTION_FILTERING && !SensorConfig.ENABLE_DUPLICATE_FILTERING) {
            return false
        }
        
        // Motion filtering - only log when significant motion detected
        if (SensorConfig.ENABLE_MOTION_FILTERING && data.accelX != null && data.accelY != null && data.accelZ != null) {
            val acceleration = Math.sqrt((data.accelX * data.accelX + data.accelY * data.accelY + data.accelZ * data.accelZ).toDouble())
            if (acceleration < SensorConfig.MOTION_THRESHOLD) {
                return true // Filter out low motion
            }
        }
        
        // Duplicate filtering
        if (SensorConfig.ENABLE_DUPLICATE_FILTERING) {
            if (data.accelX != null && data.accelY != null && data.accelZ != null) {
                val lastData = lastAccelerometerData
                if (lastData != null && 
                    lastData.accelX != null && lastData.accelY != null && lastData.accelZ != null &&
                    Math.abs(data.accelX - lastData.accelX) < 0.01f &&
                    Math.abs(data.accelY - lastData.accelY) < 0.01f &&
                    Math.abs(data.accelZ - lastData.accelZ) < 0.01f) {
                    return true // Filter out duplicate readings
                }
                lastAccelerometerData = data
            }
        }
        
        return false
    }
    
    private fun processBatches() {
        // Only process if there's significant data to process
        val totalData = sensorDataQueue.size + locationDataQueue.size + barometerDataQueue.size
        
        if (totalData < 20) { // Only process if we have at least 20 total readings
            return
        }
        
        Log.d(TAG, "Processing batch of $totalData total readings (sensors: ${sensorDataQueue.size}, locations: ${locationDataQueue.size}, barometers: ${barometerDataQueue.size})")
        
        // Process the batch (data remains in queue until explicitly retrieved)
        // This reduces the frequency of data processing
    }

    fun getUnsentSensorData(): List<SensorData> {
        return sensorDataQueue.toList().also {
            sensorDataQueue.clear()
        }
    }

    fun getUnsentLocationData(): List<LocationData> {
        return locationDataQueue.toList().also {
            locationDataQueue.clear()
        }
    }

    fun getUnsentBarometerData(): List<BarometerData> {
        return barometerDataQueue.toList().also {
            barometerDataQueue.clear()
        }
    }

    fun getUnsentAudioData(): List<AudioData> {
        return audioDataQueue.toList().also {
            audioDataQueue.clear()
        }
    }

    fun getUnsentPhotoData(): List<PhotoData> {
        return photoDataQueue.toList().also {
            photoDataQueue.clear()
        }
    }
    
    /**
     * Get unified sensor data that merges readings from different sensors
     * This reduces null values by combining gyro, accel, location, and barometer data
     */
    suspend fun getUnifiedSensorData(): List<UnifiedSensorRecord> {
        return withContext(Dispatchers.IO) {
            val unifiedRecords = mutableListOf<UnifiedSensorRecord>()
            
            // Get all current data from queues
            val allSensorData = sensorDataQueue.toList()
            val allLocationData = locationDataQueue.toList()
            val allBarometerData = barometerDataQueue.toList()
            
            if (allSensorData.isEmpty() && allLocationData.isEmpty() && allBarometerData.isEmpty()) {
                return@withContext emptyList()
            }
            
            // Group data by time windows (5 seconds)
            val timeWindow = 5000L
            val sensorGroups = allSensorData.groupBy { it.timestamp / timeWindow }
            val locationGroups = allLocationData.groupBy { it.timestamp / timeWindow }
            val barometerGroups = allBarometerData.groupBy { it.timestamp / timeWindow }
            
            // Create unified records for each time window
            val allTimeWindows = (sensorGroups.keys + locationGroups.keys + barometerGroups.keys).sorted()
            
            allTimeWindows.forEach { timeWindowKey ->
                val windowStart = timeWindowKey * timeWindow
                
                // Get the most recent data from each sensor type in this window
                val sensorsInWindow = sensorGroups[timeWindowKey] ?: emptyList()
                val locationsInWindow = locationGroups[timeWindowKey] ?: emptyList()
                val barometersInWindow = barometerGroups[timeWindowKey] ?: emptyList()
                
                // Find the most recent reading from each sensor type
                val latestSensor = sensorsInWindow.maxByOrNull { it.timestamp }
                val latestLocation = locationsInWindow.maxByOrNull { it.timestamp }
                val latestBarometer = barometersInWindow.maxByOrNull { it.timestamp }
                
                // Use the timestamp from the most recent reading
                val recordTimestamp = listOfNotNull(
                    latestSensor?.timestamp,
                    latestLocation?.timestamp,
                    latestBarometer?.timestamp
                ).maxOrNull() ?: windowStart
                
                val unifiedRecord = UnifiedSensorRecord(
                    timestamp = recordTimestamp,
                    gyroX = latestSensor?.gyroX,
                    gyroY = latestSensor?.gyroY,
                    gyroZ = latestSensor?.gyroZ,
                    accelX = latestSensor?.accelX,
                    accelY = latestSensor?.accelY,
                    accelZ = latestSensor?.accelZ,
                    latitude = latestLocation?.latitude,
                    longitude = latestLocation?.longitude,
                    accuracy = latestLocation?.accuracy,
                    pressure = latestBarometer?.pressure,
                    altitude = latestBarometer?.altitude,
                    deviceId = "android-${android.os.Build.SERIAL}" // Generate device ID
                )
                
                unifiedRecords.add(unifiedRecord)
            }
            
            // Clear the queues after processing
            sensorDataQueue.clear()
            locationDataQueue.clear()
            barometerDataQueue.clear()
            
            unifiedRecords.sortedByDescending { it.timestamp }
        }
    }

    fun shutdown() {
        batchExecutor.shutdown()
        try {
            if (!batchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            batchExecutor.shutdownNow()
        }
    }
} 