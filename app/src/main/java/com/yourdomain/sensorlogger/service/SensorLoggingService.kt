package com.yourdomain.sensorlogger.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.yourdomain.sensorlogger.data.DataRepository
import com.yourdomain.sensorlogger.sensors.BarometerController
import com.yourdomain.sensorlogger.sensors.GpsController
import com.yourdomain.sensorlogger.sensors.SensorController
import com.yourdomain.sensorlogger.sensors.CameraController
import com.yourdomain.sensorlogger.sensors.AudioRecorder
import com.yourdomain.sensorlogger.network.DataUploader
import com.yourdomain.sensorlogger.util.SensorConfig

// SensorLoggingService: Foreground service for continuous sensor/audio/location logging
// - Acquires WakeLock via WakeLockManager
// - Shows persistent notification via NotificationHelper
// - Starts/stops sensor/audio/location/camera controllers
// - Triggers periodic uploads and photo snaps
class SensorLoggingService : Service() {

    private val TAG = "SensorLoggingService"
    private lateinit var dataRepository: DataRepository
    private lateinit var sensorController: SensorController
    private lateinit var gpsController: GpsController
    private lateinit var barometerController: BarometerController
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var cameraController: CameraController
    private lateinit var dataUploader: DataUploader

    private val handler = Handler(Looper.getMainLooper())
    private val scheduledTask = object : Runnable {
        override fun run() {
            Log.d(TAG, "Scheduled task running - reduced frequency")
            cameraController.takePhoto()
            dataUploader.launchUpload()
            handler.postDelayed(this, SensorConfig.UPLOAD_INTERVAL) // Use UPLOAD_INTERVAL instead of PHOTO_INTERVAL
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        WakeLockManager.acquire(this)

        dataRepository = DataRepository()
        sensorController = SensorController(this, dataRepository)
        gpsController = GpsController(this, dataRepository)
        barometerController = BarometerController(this, dataRepository)
        audioRecorder = AudioRecorder(this, dataRepository)
        cameraController = CameraController(this, dataRepository)
        dataUploader = DataUploader(this, dataRepository)
        
        // Connect barometer controller to sensor controller for unified data
        sensorController.setBarometerController(barometerController)
        sensorController.setGpsController(gpsController)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with reduced data frequency")
        val notification = NotificationHelper.createNotification(this)
        startForeground(1, notification)

        sensorController.start()
        gpsController.start()
        barometerController.start()
        
        if (SensorConfig.ENABLE_AUDIO) {
            audioRecorder.start()
        }
        if (SensorConfig.ENABLE_CAMERA) {
            cameraController.start()
        }
        
        // Schedule tasks with reduced frequency
        handler.postDelayed(scheduledTask, SensorConfig.UPLOAD_INTERVAL)

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        WakeLockManager.release()

        sensorController.stop()
        gpsController.stop()
        barometerController.stop()
        audioRecorder.stop()
        cameraController.stop()
        handler.removeCallbacks(scheduledTask)
        
        // Properly shutdown DataRepository
        dataRepository.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null
} 