package com.yourdomain.sensorlogger.data

import android.util.Log
import com.yourdomain.sensorlogger.data.models.*
import com.yourdomain.sensorlogger.util.SensorConfig
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

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
        
        if (SensorConfig.ENABLE_DATA_BATCHING) {
            // Add to queue for batching
            sensorDataQueue.add(data)
            
            // Check if batch is full
            if (sensorDataQueue.size >= SensorConfig.BATCH_SIZE) {
                processBatches()
            }
        } else {
            // Direct processing without batching
            sensorDataQueue.add(data)
        }
    }

    fun addLocationData(data: LocationData) {
        locationDataQueue.add(data)
    }

    fun addBarometerData(data: BarometerData) {
        barometerDataQueue.add(data)
    }

    fun addAudioData(data: AudioData) {
        audioDataQueue.add(data)
    }

    fun addPhotoData(data: PhotoData) {
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
        if (sensorDataQueue.isEmpty()) {
            return
        }
        
        val batchSize = sensorDataQueue.size
        Log.d(TAG, "Processing batch of $batchSize sensor readings")
        
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