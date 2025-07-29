package com.yourdomain.sensorlogger.sensors

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import com.yourdomain.sensorlogger.data.DataRepository
import com.yourdomain.sensorlogger.data.models.AudioData
import java.io.File
import java.io.IOException

class AudioRecorder(
    private val context: Context,
    private val dataRepository: DataRepository
) {
    private val TAG = "AudioRecorder"
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    fun start() {
        audioFile = File(context.getExternalFilesDir(null), "audio_record_${System.currentTimeMillis()}.3gp")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFile?.absolutePath)
            try {
                prepare()
                start()
                Log.d(TAG, "Audio recording started")
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed", e)
            }
        }
    }

    fun stop() {
        mediaRecorder?.apply {
            stop()
            release()
            Log.d(TAG, "Audio recording stopped")
        }
        mediaRecorder = null
        audioFile?.let {
            val audioData = AudioData(System.currentTimeMillis(), it.absolutePath, 0) // Duration can be calculated if needed
            dataRepository.addAudioData(audioData) // Assuming this method exists
        }
    }
} 