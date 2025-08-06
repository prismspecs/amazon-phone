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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

// DataRepository: Handles storing, retrieving, and marking sensor/audio/location/photo data as sent
class DataRepository {
    private val TAG = "DataRepository"
    
    // Batching mechanism for 15-second chunks
    private val currentBatch = ConcurrentLinkedQueue<UnifiedSensorRecord>()
    private val completedBatches = ConcurrentLinkedQueue<List<UnifiedSensorRecord>>()
    private val batchExecutor = Executors.newSingleThreadScheduledExecutor()
    private val isBatchingEnabled = AtomicBoolean(true)
    private val hasStartedBatching = AtomicBoolean(false)
    private val batchCounter = AtomicInteger(0)
    
    init {
        // Start batch timer immediately when DataRepository is created
        Log.d(TAG, "🚀 Starting batch timer immediately at ${System.currentTimeMillis()}")
        batchExecutor.scheduleAtFixedRate({
            Log.d(TAG, "🕐 Batch executor triggered at ${System.currentTimeMillis()}")
            // Only flush if we have exactly 15 records, otherwise wait
            if (currentBatch.size == 15) {
                flushCurrentBatch()
            } else {
                Log.d(TAG, "⏳ Timer triggered but only ${currentBatch.size}/15 records - waiting")
            }
        }, SensorConfig.BATCH_TIMEOUT, SensorConfig.BATCH_TIMEOUT, TimeUnit.MILLISECONDS)
    }
    
    // Simple unified record addition with batching
    fun addUnifiedRecord(record: UnifiedSensorRecord) {
        if (isBatchingEnabled.get()) {
            currentBatch.add(record)
            val currentSize = currentBatch.size
            Log.d(TAG, "➕ Added to batch: timestamp=${record.timestamp}, batch size=${currentSize}")
            
            // Log when we reach exactly 15 records
            if (currentSize == 15) {
                Log.d(TAG, "🎯 Batch reached exactly 15 records!")
                // Force flush immediately when we reach 15 records
                flushCurrentBatch()
            }
        }
    }
    
    private fun flushCurrentBatch() {
        val currentSize = currentBatch.size
        Log.d(TAG, "🔄 Flush called with ${currentSize} records in current batch")
        
        // Only flush if we have exactly 15 records
        if (currentSize == 15) {
            val batchData = currentBatch.toList()
            currentBatch.clear()
            completedBatches.add(batchData)
            Log.d(TAG, "📦 Flushed batch: ${batchData.size} records at ${System.currentTimeMillis()}")
            
            // Log the time span of this batch
            if (batchData.isNotEmpty()) {
                val firstTime = batchData.first().timestamp
                val lastTime = batchData.last().timestamp
                val duration = (lastTime - firstTime) / 1000
                Log.d(TAG, "⏱️ Batch time span: ${duration} seconds (${firstTime} to ${lastTime})")
            }
        } else {
            Log.d(TAG, "⏳ Waiting for exactly 15 records: ${currentSize}/15")
        }
    }
    
    // Get all completed batches and clear them
    fun getUnifiedRecords(): List<UnifiedSensorRecord> {
        val allRecords = mutableListOf<UnifiedSensorRecord>()
        
        // Get all completed batches ONLY
        while (completedBatches.isNotEmpty()) {
            val batch = completedBatches.poll()
            if (batch != null) {
                allRecords.addAll(batch)
            }
        }
        
        // DO NOT include current batch - wait for it to complete
        // This ensures we only get full 15-second chunks
        
        return allRecords
    }
    
    // Legacy methods for backward compatibility (simplified)
    private val audioDataQueue = ConcurrentLinkedQueue<AudioData>()
    private val photoDataQueue = ConcurrentLinkedQueue<PhotoData>()
    
    fun addAudioData(data: AudioData) {
        audioDataQueue.add(data)
    }

    fun addPhotoData(data: PhotoData) {
        photoDataQueue.add(data)
    }
    
    // Legacy methods for backward compatibility (simplified)
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
        isBatchingEnabled.set(false)
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