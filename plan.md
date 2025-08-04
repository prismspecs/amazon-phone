To ensure **sensor logging continues with the screen off**, with full control over the phone's behavior, you should use **Android** (not iOS), and develop a **foreground service** with wake locks and battery optimization bypass.

### ✅ **Key Android Features You Need**

1. **Foreground Service**

   - Keeps the app running in the background
   - Shows a persistent notification (required by Android)

2. **Partial Wake Lock**

   - Prevents the CPU from sleeping, so sensor data continues logging even with the screen off

3. **Ignore Battery Optimizations**

   - Allows your app to run uninterrupted on devices with aggressive power saving
   - Requires explicit user permission

4. **SensorManager + SensorEventListener**w

   - For gyroscope, accelerometer, barometer (if available)

5. **Data Storage**

   - Use `Room` (SQLite) or simple file/CSV logging
   - Optional: periodically write to file to avoid RAM buildup

---

### 🔧 Development Stack

- **Platform**: Android 8+
- **Language**: Kotlin (preferred) or Java
- **IDE**: Android Studio
- **Permissions**:

  - `FOREGROUND_SERVICE`
  - `WAKE_LOCK`
  - `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
  - `BODY_SENSORS` (if required by OEM)
  - `RECEIVE_BOOT_COMPLETED` (for autostart)

---

### 📱 Sensor Availability by API

- **Gyroscope**: `Sensor.TYPE_GYROSCOPE`
- **Accelerometer**: `Sensor.TYPE_ACCELEROMETER`
- **Barometric Pressure**: `Sensor.TYPE_PRESSURE`
- **Orientation (rotation vector/quaternion)**: `Sensor.TYPE_ROTATION_VECTOR`

---

### 🔋 Battery Strategy

- **Use airplane mode**
- **Disable Wi-Fi, Bluetooth, mobile data**
- **Dim screen or turn it off**
- **Ensure wake lock + foreground service active**

---

### 📦 Optional Enhancements

- **Autostart on boot** (in case the phone reboots mid-transit)
- **Buttonless interface** – e.g., recording starts automatically when charging stops or motion begins
- **Time-limited logging** – to avoid massive files
- **Encrypted file output** – in case the phone is inspected

---

Let me know if you want a sample Kotlin project with wake lock + background sensor logging.

---

## 📱 Android Sensor Logger App Architecture (Initial Scaffold)

### Directory Structure

```
app/
  src/
    main/
      java/
        com/
          yourdomain/
            sensorlogger/
              MainActivity.kt
              service/
                SensorLoggingService.kt
                WakeLockManager.kt
                NotificationHelper.kt
              sensors/
                SensorController.kt
                AudioRecorder.kt
                GpsController.kt
                BarometerController.kt
                CameraController.kt
              data/
                DataRepository.kt
                models/
                  SensorData.kt
                  AudioData.kt
                  LocationData.kt
                  BarometerData.kt
                  PhotoData.kt
              network/
                DataUploader.kt
              util/
                TimeUtils.kt
                PermissionUtils.kt
      res/
        layout/
          activity_main.xml
        values/
          strings.xml
          colors.xml
          styles.xml
      AndroidManifest.xml
```

### Component Overview

- **MainActivity.kt**: Minimal UI for permissions/status, starts/stops service.
- **service/**: Foreground service, wake lock, notification.
- **sensors/**: Sensor, audio, GPS, barometer, camera controllers.
- **data/**: Repository and models for storing sensor/audio/location/photo data.
- **network/**: Handles uploading data/photos to endpoint.
- **util/**: Time and permission utilities.

### Data Model

- **SensorData**: timestamp, gyro, accelerometer, etc.
- **AudioData**: timestamp, file path, duration.
- **LocationData**: timestamp, lat, lon, accuracy.
- **BarometerData**: timestamp, pressure, altitude.
- **PhotoData**: timestamp, file path.

---

This structure supports modular, resilient, and efficient sensor/media logging as described above. Each component is designed for single responsibility and easy extension.

### Implementation Status

- [x] **Project Scaffolding**: Complete. All modules and classes created.
- [x] **Service Layer**: Complete with wake lock and notification.
- [x] **Sensors Layer**: Complete with throttling and batching.
- [x] **Data Layer**: Complete with filtering and batching.
- [x] **Network Layer**: Complete with upload functionality.
- [x] **Utils**: Complete with configuration system.
- [ ] **UI/Permissions**: Pending.

---

## 🚀 Frequency Optimization (COMPLETED)

### Problem Solved
The app was delivering sensor data extremely frequently, causing excessive battery drain and performance issues.

### Key Optimizations Implemented

#### 1. **Reduced Sensor Polling Rates** (75% reduction)
- **Accelerometer/Gyroscope/Barometer**: 2Hz → 0.5Hz (every 2 seconds)
- **GPS updates**: 10 seconds → 30 seconds  
- **Photo captures**: 30 minutes → 60 minutes
- **Data uploads**: 30 minutes → 60 minutes

#### 2. **Motion Filtering System** (Configurable)
- **Motion Threshold**: `MOTION_THRESHOLD = 0.1f` in `SensorConfig.kt`
- **How it works**: Calculates total acceleration magnitude from X,Y,Z values
- **Formula**: `√(accelX² + accelY² + accelZ²)`
- **Current setting**: Only logs when acceleration > 0.1 m/s²
- **Fine-grained control**: You can adjust this value in `SensorConfig.kt`:
  - `0.1f` = Very sensitive (logs most movements)
  - `0.5f` = Medium sensitivity (current setting)
  - `1.0f` = Low sensitivity (only significant movements)
  - `2.0f` = Very low sensitivity (only major movements)

#### 3. **Data Batching & Throttling**
- **Batch size**: 50 sensor readings before processing
- **Batch timeout**: 30 seconds (flush incomplete batches)
- **Processing throttle**: Minimum 1-second interval between processing
- **Duplicate filtering**: Eliminates readings with <0.01f difference

#### 4. **Configurable Settings in SensorConfig.kt**
```kotlin
// Motion filtering
val ENABLE_MOTION_FILTERING = true
val MOTION_THRESHOLD = 0.1f           // Adjust this value for sensitivity

// Data batching  
val BATCH_SIZE = 50                   // Readings per batch
val BATCH_TIMEOUT = 30000L            // 30 seconds timeout
val ENABLE_DATA_BATCHING = true

// Polling rates
val ACCELEROMETER_DELAY = 2000000     // 0.5Hz (2 seconds)
val GPS_UPDATE_INTERVAL = 30000L      // 30 seconds
val UPLOAD_INTERVAL = 60 * 60 * 1000L // 60 minutes
```

### Result
- **75% reduction** in sensor polling frequency
- **66% reduction** in GPS updates  
- **50% reduction** in data uploads
- **Motion-based filtering** eliminates unnecessary readings
- **Significantly reduced battery consumption**

---

## 🌐 Server Setup (COMPLETED)

### Minimal Node.js Server with CapRover Deployment

A complete server setup is included in the `server/` directory for receiving sensor data.

#### **Server Features:**
- **Express.js** with CORS support
- **SQLite database** for data persistence
- **File upload** support for audio/photos
- **Health check** and **data summary** endpoints
- **Docker** containerization
- **CapRover** deployment ready

#### **Server Endpoints:**
- `POST /upload` - Receive sensor data (JSON)
- `POST /upload-files` - Receive audio/photo files
- `GET /health` - Health check
- `GET /summary` - Data statistics

#### **Deployment Steps:**

1. **Push to Git:**
   ```bash
   git add server/
   git commit -m "Add sensor data server"
   git push origin main
   ```

2. **Deploy to CapRover:**
   - Go to CapRover dashboard
   - Click "One-Click Apps" → "Git"
   - Enter your Git repository URL
   - Set app name (e.g., `sensor-server`)
   - Deploy

3. **Update Android App:**
   - Edit `DataUploader.kt` line 15:
   ```kotlin
   private val endpoint = "https://your-app-name.your-domain.com/upload"
   ```

#### **Server Database Tables:**
- `sensor_data` - Gyroscope and accelerometer readings
- `location_data` - GPS coordinates  
- `barometer_data` - Pressure and altitude
- `audio_files` - Audio file metadata
- `photo_files` - Photo file metadata

#### **Data Format:**
```json
{
  "deviceId": "android-123456",
  "sensors": [
    {
      "timestamp": 1640995200000,
      "gyroX": 0.1, "gyroY": 0.2, "gyroZ": 0.3,
      "accelX": 9.8, "accelY": 0.1, "accelZ": 0.2
    }
  ],
  "locations": [
    {
      "timestamp": 1640995200000,
      "latitude": 40.7128, "longitude": -74.0060, "accuracy": 5.0
    }
  ]
}
```

This server setup provides a complete, scalable solution for receiving and storing sensor data from your Android app.

---

## 📋 Development Notes & Warnings

### ⚠️ **Important Configuration Notes**

#### **CapRover captain-definition**
- **ALWAYS use `"schemaVersion": 2`** (NOT `"schema": 2`)
- The captain-definition file must use `schemaVersion` property
- Do not automatically rename or change this property
- This is critical for proper CapRover deployment

#### **Persistent Storage Setup**
- Add persistent directory in CapRover dashboard: `/app/data` → `sensor-data`
- Database path: `/app/data/sensor_data.db`
- Upload directory: `/app/data/uploads`

#### **Deployment Files**
- `sensor-server.tar.gz` is in `.gitignore` (should not be committed)
- Use `./deploy.sh` in `server/` directory to create deployment package
- Always verify captain-definition uses `schemaVersion` before deploying
