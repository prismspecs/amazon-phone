package com.yourdomain.sensorlogger.network

import android.util.Log
import com.yourdomain.sensorlogger.data.DataRepository
import com.yourdomain.sensorlogger.data.models.AudioData
import com.yourdomain.sensorlogger.data.models.PhotoData
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
import java.io.File
import java.io.FileInputStream
import java.io.ByteArrayOutputStream
import android.os.Build
import android.content.Context
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.*

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
            // Get unified records directly
            val unifiedData = dataRepository.getUnifiedRecords()
            val audios = dataRepository.getUnsentAudioData()
            val photos = dataRepository.getUnsentPhotoData()

            // Skip upload if no data
            if (unifiedData.isEmpty() && audios.isEmpty() && photos.isEmpty()) {
                Log.d(TAG, "No data to upload")
                return
            }

            // Upload sensor data
            if (unifiedData.isNotEmpty()) {
                uploadSensorData(unifiedData)
            }

            // Upload audio files
            if (audios.isNotEmpty()) {
                uploadAudioFiles(audios)
            }

            // Upload photo files
            if (photos.isNotEmpty()) {
                uploadPhotoFiles(photos)
            }

        } catch (e: Exception) {
            val errorMessage = "❌ Upload failed: ${e.message}"
            Log.e(TAG, errorMessage, e)
            showToast(errorMessage)
        }
    }

    private suspend fun uploadSensorData(unifiedData: List<com.yourdomain.sensorlogger.data.models.UnifiedSensorRecord>) {
        // Create JSON payload with unified data format
        val payload = JSONObject().apply {
            put("deviceId", deviceId)
            put("count", unifiedData.size)
            
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
                    put("created_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(record.timestamp)))
                }
                dataArray.put(dataObj)
            }
            put("data", dataArray)
        }

        // Log what would be sent
        val dataSummary = "${unifiedData.size} unified records"
        Log.d(TAG, "🔄 Starting sensor data upload: $dataSummary")
        showToast("📤 Starting sensor upload: $dataSummary")

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
        Log.d(TAG, "📡 Sensor upload response: $responseCode")
        
        if (responseCode == 200) {
            val uploadCount = uploadCounter.incrementAndGet()
            val successMessage = "✅ Sensor upload #$uploadCount successful! ($dataSummary)"
            Log.d(TAG, successMessage)
            showToast(successMessage)
        } else {
            val errorMessage = "❌ Sensor upload failed: HTTP $responseCode"
            Log.e(TAG, errorMessage)
            showToast(errorMessage)
        }
        
        conn.disconnect()
    }

    private suspend fun uploadAudioFiles(audios: List<AudioData>) {
        Log.d(TAG, "🔄 Starting audio files upload: ${audios.size} files")
        showToast("📤 Starting audio upload: ${audios.size} files")

        audios.forEach { audioData ->
            try {
                val audioFile = File(audioData.filePath)
                if (!audioFile.exists()) {
                    Log.w(TAG, "Audio file not found: ${audioData.filePath}")
                    return@forEach
                }

                // Create timestamp-based filename for association
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(audioData.timestamp))
                val filename = "audio_${timestamp}.m4a"

                uploadFile(audioFile, filename, "audio", audioData.durationMs)
                Log.d(TAG, "✅ Audio file uploaded: $filename (duration: ${audioData.durationMs}ms)")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Audio file upload failed: ${audioData.filePath}", e)
            }
        }
    }

    private suspend fun uploadPhotoFiles(photos: List<PhotoData>) {
        Log.d(TAG, "🔄 Starting photo files upload: ${photos.size} files")
        showToast("📤 Starting photo upload: ${photos.size} files")

        photos.forEach { photoData ->
            try {
                val photoFile = File(photoData.filePath)
                if (!photoFile.exists()) {
                    Log.w(TAG, "Photo file not found: ${photoData.filePath}")
                    return@forEach
                }

                // Create timestamp-based filename for association
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(photoData.timestamp))
                val filename = "photo_${timestamp}.jpg"

                uploadFile(photoFile, filename, "photo", null)
                Log.d(TAG, "✅ Photo file uploaded: $filename")

            } catch (e: Exception) {
                Log.e(TAG, "❌ Photo file upload failed: ${photoData.filePath}", e)
            }
        }
    }

    private suspend fun uploadFile(file: File, filename: String, fileType: String, duration: Long? = null) {
        val url = URL(fileUploadEndpoint)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.doOutput = true
        
        val boundary = "*****"
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        conn.setRequestProperty("User-Agent", "SensorLogger-Android")

        conn.outputStream.use { outputStream ->
            val writer = outputStream.bufferedWriter()
            
            // Write multipart form data
            writer.write("--$boundary\r\n")
            writer.write("Content-Disposition: form-data; name=\"deviceId\"\r\n\r\n")
            writer.write("$deviceId\r\n")
            
            writer.write("--$boundary\r\n")
            writer.write("Content-Disposition: form-data; name=\"fileType\"\r\n\r\n")
            writer.write("$fileType\r\n")
            
            writer.write("--$boundary\r\n")
            writer.write("Content-Disposition: form-data; name=\"filename\"\r\n\r\n")
            writer.write("$filename\r\n")
            
            // Add duration for audio files
            duration?.let { dur ->
                writer.write("--$boundary\r\n")
                writer.write("Content-Disposition: form-data; name=\"duration\"\r\n\r\n")
                writer.write("$dur\r\n")
            }
            
            writer.write("--$boundary\r\n")
            writer.write("Content-Disposition: form-data; name=\"files\"; filename=\"$filename\"\r\n")
            writer.write("Content-Type: application/octet-stream\r\n\r\n")
            writer.flush()
            
            // Write file content
            FileInputStream(file).use { inputStream ->
                inputStream.copyTo(outputStream)
            }
            
            writer.write("\r\n--$boundary--\r\n")
            writer.flush()
        }

        val responseCode = conn.responseCode
        Log.d(TAG, "📡 File upload response: $responseCode")
        
        if (responseCode != 200) {
            Log.e(TAG, "❌ File upload failed: HTTP $responseCode")
        }
        
        conn.disconnect()
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