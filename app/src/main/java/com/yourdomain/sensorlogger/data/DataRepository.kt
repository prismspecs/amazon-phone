package com.yourdomain.sensorlogger.data

import com.yourdomain.sensorlogger.data.models.*
import java.util.concurrent.ConcurrentLinkedQueue

// DataRepository: Handles storing, retrieving, and marking sensor/audio/location/photo data as sent
class DataRepository {
    private val sensorDataQueue = ConcurrentLinkedQueue<SensorData>()
    private val locationDataQueue = ConcurrentLinkedQueue<LocationData>()
    private val barometerDataQueue = ConcurrentLinkedQueue<BarometerData>()
    private val audioDataQueue = ConcurrentLinkedQueue<AudioData>()
    private val photoDataQueue = ConcurrentLinkedQueue<PhotoData>()

    fun addSensorData(data: SensorData) {
        sensorDataQueue.add(data)
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
    // TODO: Implement methods for other data types
} 