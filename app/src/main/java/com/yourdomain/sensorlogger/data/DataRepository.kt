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
    private val unifiedRecordsQueue = ConcurrentLinkedQueue<UnifiedSensorRecord>()
    
    // Simple unified record addition
    fun addUnifiedRecord(record: UnifiedSensorRecord) {
        unifiedRecordsQueue.add(record)
        Log.d(TAG, "Added unified record: timestamp=${record.timestamp}, gyro=(${record.gyroX},${record.gyroY},${record.gyroZ}), accel=(${record.accelX},${record.accelY},${record.accelZ})")
    }
    
    // Get all unified records and clear queue
    fun getUnifiedRecords(): List<UnifiedSensorRecord> {
        return unifiedRecordsQueue.toList().also {
            unifiedRecordsQueue.clear()
        }
    }
    
    // Legacy methods for backward compatibility (simplified)
    private val sensorDataQueue = ConcurrentLinkedQueue<SensorData>()
    private val locationDataQueue = ConcurrentLinkedQueue<LocationData>()
    private val barometerDataQueue = ConcurrentLinkedQueue<BarometerData>()
    private val audioDataQueue = ConcurrentLinkedQueue<AudioData>()
    private val photoDataQueue = ConcurrentLinkedQueue<PhotoData>()
    
    fun addSensorData(data: SensorData) {
        // Legacy method - no longer used with unified approach
        // Keeping for backward compatibility but not processing
    }

    fun addLocationData(data: LocationData) {
        // Legacy method - no longer used with unified approach
        // Keeping for backward compatibility but not processing
    }

    fun addBarometerData(data: BarometerData) {
        // Legacy method - no longer used with unified approach
        // Keeping for backward compatibility but not processing
    }

    fun addAudioData(data: AudioData) {
        audioDataQueue.add(data)
    }

    fun addPhotoData(data: PhotoData) {
        photoDataQueue.add(data)
    }

    private fun shouldFilterData(data: SensorData): Boolean {
        // Legacy filtering - disabled for unified approach
        return false
    }
    
    // Legacy methods for backward compatibility (simplified)
    fun getUnsentSensorData(): List<SensorData> {
        return emptyList() // No longer used
    }

    fun getUnsentLocationData(): List<LocationData> {
        return emptyList() // No longer used
    }

    fun getUnsentBarometerData(): List<BarometerData> {
        return emptyList() // No longer used
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
        // No batchExecutor to shutdown, as unifiedRecordsQueue is now the primary storage
    }
} 