To ensure **sensor logging continues with the screen off**, with full control over the phone’s behavior, you should use **Android** (not iOS), and develop a **foreground service** with wake locks and battery optimization bypass.

### ✅ **Key Android Features You Need**

1. **Foreground Service**

   - Keeps the app running in the background
   - Shows a persistent notification (required by Android)

2. **Partial Wake Lock**

   - Prevents the CPU from sleeping, so sensor data continues logging even with the screen off

3. **Ignore Battery Optimizations**

   - Allows your app to run uninterrupted on devices with aggressive power saving
   - Requires explicit user permission

4. **SensorManager + SensorEventListener**

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
- [ ] **Service Layer**: In progress.
- [ ] **Sensors Layer**: Pending.
- [ ] **Data Layer**: Pending.
- [ ] **Network Layer**: Pending.
- [ ] **Utils**: Pending.
- [ ] **UI/Permissions**: Pending.
