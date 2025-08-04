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
        
        if (totalData < 5) { // Reduced from 20 to 5 - process smaller batches
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
     * This creates continuous data by interpolating between sensor readings
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
            
            // Sort all data by timestamp
            val sortedSensorData = allSensorData.sortedBy { it.timestamp }
            val sortedLocationData = allLocationData.sortedBy { it.timestamp }
            val sortedBarometerData = allBarometerData.sortedBy { it.timestamp }
            
            // Get all unique timestamps from all sensor types
            val allTimestamps = mutableSetOf<Long>()
            allTimestamps.addAll(sortedSensorData.map { it.timestamp })
            allTimestamps.addAll(sortedLocationData.map { it.timestamp })
            allTimestamps.addAll(sortedBarometerData.map { it.timestamp })
            
            val sortedTimestamps = allTimestamps.sorted()
            
            // Create unified records for each timestamp
            sortedTimestamps.forEach { timestamp ->
                // Find the closest sensor data (within 100ms window)
                val sensorData = sortedSensorData.findLast { 
                    it.timestamp <= timestamp && timestamp - it.timestamp <= 100 
                }
                
                // Find the closest location data (within 5 second window)
                val locationData = sortedLocationData.findLast { 
                    it.timestamp <= timestamp && timestamp - it.timestamp <= 5000 
                }
                
                // Find the closest barometer data (within 2 second window)
                val barometerData = sortedBarometerData.findLast { 
                    it.timestamp <= timestamp && timestamp - it.timestamp <= 2000 
                }
                
                val unifiedRecord = UnifiedSensorRecord(
                    timestamp = timestamp,
                    gyroX = sensorData?.gyroX,
                    gyroY = sensorData?.gyroY,
                    gyroZ = sensorData?.gyroZ,
                    accelX = sensorData?.accelX,
                    accelY = sensorData?.accelY,
                    accelZ = sensorData?.accelZ,
                    latitude = locationData?.latitude,
                    longitude = locationData?.longitude,
                    accuracy = locationData?.accuracy,
                    pressure = barometerData?.pressure,
                    altitude = barometerData?.altitude,
                    deviceId = "android-${android.os.Build.SERIAL}"
                )
                
                unifiedRecords.add(unifiedRecord)
            }
            
            // Clear the queues after processing
            sensorDataQueue.clear()
            locationDataQueue.clear()
            barometerDataQueue.clear()
            
            unifiedRecords.sortedBy { it.timestamp }
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