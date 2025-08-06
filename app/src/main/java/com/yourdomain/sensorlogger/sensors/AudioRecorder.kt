package com.yourdomain.sensorlogger.sensors

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import com.yourdomain.sensorlogger.data.DataRepository
import com.yourdomain.sensorlogger.data.models.AudioData
import com.yourdomain.sensorlogger.util.SensorConfig
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.os.Handler
import android.os.Looper

class AudioRecorder(
    private val context: Context,
    private val dataRepository: DataRepository
) {
    private val TAG = "AudioRecorder"
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var recordingStartTime: Long = 0
    private var isRecording = false
    
    // Handler for 15-second recording cycles
    private val handler = Handler(Looper.getMainLooper())
    private val recordingRunnable = object : Runnable {
        override fun run() {
            stopCurrentRecording()
            startNewRecording()
            // Schedule next recording cycle
            handler.postDelayed(this, SensorConfig.UPLOAD_INTERVAL)
        }
    }

    fun start() {
        Log.d(TAG, "Starting audio recorder with 15-second cycles")
        startNewRecording()
        
        // Schedule 15-second recording cycles
        handler.postDelayed(recordingRunnable, SensorConfig.UPLOAD_INTERVAL)
    }

    fun stop() {
        Log.d(TAG, "Stopping audio recorder")
        handler.removeCallbacks(recordingRunnable)
        stopCurrentRecording()
    }

    private fun startNewRecording() {
        if (isRecording) {
            Log.w(TAG, "Already recording, skipping start")
            return
        }

        recordingStartTime = System.currentTimeMillis()
        
        // Create filename with timestamp for easy association with sensor data
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(recordingStartTime))
        val filename = "audio_${timestamp}.m4a"
        
        audioFile = File(context.getExternalFilesDir(null), filename)
        
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(64000) // 64 kbps for efficiency
            setAudioSamplingRate(22050) // 22.05 kHz for efficiency
            setOutputFile(audioFile?.absolutePath)
            try {
                prepare()
                start()
                isRecording = true
                Log.d(TAG, "Audio recording started: $filename (start time: $recordingStartTime)")
            } catch (e: IOException) {
                Log.e(TAG, "Audio recording prepare() failed", e)
                isRecording = false
            }
        }
    }

    private fun stopCurrentRecording() {
        if (!isRecording) {
            return
        }

        mediaRecorder?.apply {
            try {
                stop()
                release()
                Log.d(TAG, "Audio recording stopped")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping audio recording", e)
            }
        }
        mediaRecorder = null
        isRecording = false
        
        // Create AudioData with timestamp for association
        audioFile?.let { file ->
            val recordingEndTime = System.currentTimeMillis()
            val duration = recordingEndTime - recordingStartTime
            
            val audioData = AudioData(
                timestamp = recordingStartTime,  // Use start time for association
                filePath = file.absolutePath,
                durationMs = duration
            )
            
            dataRepository.addAudioData(audioData)
            Log.d(TAG, "Added audio data: ${file.name} (duration: ${duration}ms, start: $recordingStartTime)")
        }
    }
}