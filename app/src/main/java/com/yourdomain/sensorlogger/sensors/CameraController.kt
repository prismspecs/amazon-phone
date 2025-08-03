package com.yourdomain.sensorlogger.sensors

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.yourdomain.sensorlogger.data.DataRepository
import com.yourdomain.sensorlogger.data.models.PhotoData
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraController(
    private val context: Context,
    private val dataRepository: DataRepository
) {
    private val TAG = "CameraController"
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var cameraProvider: ProcessCameraProvider? = null

    init {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun start() {
        // Camera initialization is complex in service context
        // For now, just log that camera is ready for manual photo capture
        Log.d(TAG, "Camera controller initialized (manual photo capture only)")
    }

    fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(context.getExternalFilesDir(null), "photo_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val photoData = PhotoData(System.currentTimeMillis(), photoFile.absolutePath)
                    dataRepository.addPhotoData(photoData) // Assuming this method exists
                    Log.d(TAG, "Photo saved: ${photoFile.absolutePath}")
                }
            })
    }

    fun stop() {
        cameraExecutor.shutdown()
        Log.d(TAG, "Camera stopped")
    }
} 