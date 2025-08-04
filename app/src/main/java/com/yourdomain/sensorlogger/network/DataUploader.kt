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
            // Gather unified sensor data (reduces null values)
            val unifiedData = dataRepository.getUnifiedSensorData()
            val audios = dataRepository.getUnsentAudioData()
            val photos = dataRepository.getUnsentPhotoData()

            // Skip upload if no data
            if (unifiedData.isEmpty() && audios.isEmpty() && photos.isEmpty()) {
                Log.d(TAG, "No data to upload")
                return
            }

            // Create JSON payload with unified data format
            val payload = JSONObject().apply {
                put("deviceId", deviceId)
                put("count", unifiedData.size)
                
                if (unifiedData.isNotEmpty()) {
                    val dataArray = JSONArray()
                    unifiedData.forEach { record ->
                        val dataObj = JSONObject().apply {
                            put("type", record.type)
                            put("timestamp", record.timestamp)
                            put("gyro_x", record.gyroX)
                            put("gyro_y", record.gyroY)
                            put("gyro_z", record.gyroZ)
                            put("accel_x", record.accelX)
                            put("accel_y", record.accelY)
                            put("accel_z", record.accelZ)
                            put("latitude", record.latitude)
                            put("longitude", record.longitude)
                            put("accuracy", record.accuracy)
                            put("pressure", record.pressure)
                            put("altitude", record.altitude)
                            put("device_id", record.deviceId)
                            put("created_at", java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                .format(java.util.Date(record.timestamp)))
                        }
                        dataArray.put(dataObj)
                    }
                    put("data", dataArray)
                }
            }

            // Log what would be sent
            val dataSummary = "${unifiedData.size} unified records"
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