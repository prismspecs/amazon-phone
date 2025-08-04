package com.yourdomain.sensorlogger.network

import android.util.Log
import com.yourdomain.sensorlogger.data.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import org.json.JSONObject
import org.json.JSONArray
import java.io.OutputStreamWriter
import android.os.Build
import android.content.Context
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import java.util.UUID

class DataUploader(private val context: Context, private val dataRepository: DataRepository) {
    private val TAG = "DataUploader"
    
    // Updated to use your specific server URL
    private val endpoint = "https://amazon.government.rip/upload"
    private val fileUploadEndpoint = "https://amazon.government.rip/upload-files"
    
    // Generate unique device ID
    private val deviceId = generateDeviceId()
    private val uploadCounter = AtomicInteger(0)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun launchUpload() {
        GlobalScope.launch(Dispatchers.IO) {
            uploadAll()
        }
    }

    private suspend fun uploadAll() {
        try {
            // Gather all unsent data
            val sensors = dataRepository.getUnsentSensorData()
            val locations = dataRepository.getUnsentLocationData()
            val barometers = dataRepository.getUnsentBarometerData()
            val audios = dataRepository.getUnsentAudioData()
            val photos = dataRepository.getUnsentPhotoData()

            // Skip upload if no data
            if (sensors.isEmpty() && locations.isEmpty() && barometers.isEmpty() && 
                audios.isEmpty() && photos.isEmpty()) {
                Log.d(TAG, "No data to upload")
                return
            }

            // Create JSON payload
            val payload = JSONObject().apply {
                put("deviceId", deviceId)
                
                if (sensors.isNotEmpty()) {
                    val sensorsArray = JSONArray()
                    sensors.forEach { sensor ->
                        val sensorObj = JSONObject().apply {
                            put("timestamp", sensor.timestamp)
                            sensor.gyroX?.let { put("gyroX", it) }
                            sensor.gyroY?.let { put("gyroY", it) }
                            sensor.gyroZ?.let { put("gyroZ", it) }
                            sensor.accelX?.let { put("accelX", it) }
                            sensor.accelY?.let { put("accelY", it) }
                            sensor.accelZ?.let { put("accelZ", it) }
                        }
                        sensorsArray.put(sensorObj)
                    }
                    put("sensors", sensorsArray)
                }
                
                if (locations.isNotEmpty()) {
                    val locationsArray = JSONArray()
                    locations.forEach { location ->
                        val locationObj = JSONObject().apply {
                            put("timestamp", location.timestamp)
                            put("latitude", location.latitude)
                            put("longitude", location.longitude)
                            put("accuracy", location.accuracy)
                        }
                        locationsArray.put(locationObj)
                    }
                    put("locations", locationsArray)
                }
                
                if (barometers.isNotEmpty()) {
                    val barometersArray = JSONArray()
                    barometers.forEach { barometer ->
                        val barometerObj = JSONObject().apply {
                            put("timestamp", barometer.timestamp)
                            put("pressure", barometer.pressure)
                            put("altitude", barometer.altitude)
                        }
                        barometersArray.put(barometerObj)
                    }
                    put("barometers", barometersArray)
                }
            }

            // Log what would be sent
            val dataSummary = "${sensors.size} sensors, ${locations.size} locations, ${barometers.size} barometers"
            Log.d(TAG, "🔄 Starting upload to amazon.government.rip: $dataSummary")
            showToast("📤 Starting upload: $dataSummary")

            // Upload data
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "SensorLogger-Android")
            
            // Send payload
            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
            }
            
            val responseCode = conn.responseCode
            Log.d(TAG, "📡 Upload response: $responseCode")
            
            if (responseCode == 200) {
                val uploadCount = uploadCounter.incrementAndGet()
                val successMessage = "✅ Upload #$uploadCount successful! ($dataSummary)"
                Log.d(TAG, successMessage)
                showToast(successMessage)
            } else {
                val errorMessage = "❌ Upload failed: HTTP $responseCode"
                Log.e(TAG, errorMessage)
                showToast(errorMessage)
            }
            
            conn.disconnect()
            
        } catch (e: Exception) {
            val errorMessage = "❌ Upload failed: ${e.message}"
            Log.e(TAG, errorMessage, e)
            showToast(errorMessage)
        }
    }
    
    private fun showToast(message: String) {
        mainHandler.post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    private fun generateDeviceId(): String {
        return try {
            // Try to get a unique device identifier
            val androidId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: UUID.randomUUID().toString()
            
            "android-${androidId}"
        } catch (e: Exception) {
            // Fallback to random UUID
            "android-${UUID.randomUUID()}"
        }
    }
} 