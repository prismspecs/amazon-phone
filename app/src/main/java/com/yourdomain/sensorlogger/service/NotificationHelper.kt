package com.yourdomain.sensorlogger.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.yourdomain.sensorlogger.R

object NotificationHelper {

    private const val CHANNEL_ID = "SensorLoggerChannel"
    private const val CHANNEL_NAME = "Sensor Logging Service"

    fun createNotification(context: Context): Notification {
        createNotificationChannel(context)
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Sensor Logger")
            .setContentText("Logging sensor data in the background.")
            .setSmallIcon(R.drawable.ic_notification) // Add a notification icon
            .build()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
} 