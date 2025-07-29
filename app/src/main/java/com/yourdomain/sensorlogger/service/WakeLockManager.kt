package com.yourdomain.sensorlogger.service

import android.content.Context
import android.os.PowerManager
import android.util.Log

object WakeLockManager {
    private const val TAG = "WakeLockManager"
    private var wakeLock: PowerManager.WakeLock? = null

    @Synchronized
    fun acquire(context: Context) {
        if (wakeLock == null) {
            val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SensorLogger::WakeLock")
            wakeLock?.setReferenceCounted(false)
            wakeLock?.acquire()
            Log.d(TAG, "WakeLock acquired")
        }
    }

    @Synchronized
    fun release() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            wakeLock = null
            Log.d(TAG, "WakeLock released")
        }
    }
} 