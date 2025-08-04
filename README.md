# Return-to-Sender: Android Sensor Logger

This Android application is designed for continuous, resilient background logging of sensor data, audio, GPS, and periodic photos. It runs as a foreground service to ensure data collection continues even when the screen is off, and it periodically uploads the collected data to a remote endpoint.

## Features

- **Continuous Background Logging**: Runs as a foreground service with a partial wake lock to prevent the system from sleeping.
- **Multi-Sensor Support**:
  - Gyroscope & Accelerometer (`SensorManager`)
  - GPS (`FusedLocationProviderClient`)
  - Barometric Pressure (`SensorManager`)
  - Audio (`MediaRecorder`)
- **Periodic Photos**: Captures a photo with the camera every 30 minutes.
- **Periodic Data Uploads**: Batches and sends all collected data and photos to a remote endpoint every 30 minutes.
- **Resilience**: Designed to handle device reboots and survive aggressive battery optimization.
- **Local Storage**: Persists data locally before successful upload (implementation via Room or file-based storage).

## Architecture

The application follows a modular, single-responsibility architecture:

- **`MainActivity.kt`**: Minimal UI for requesting permissions and starting/stopping the service.
- **`service/`**: Contains the core `SensorLoggingService`, `WakeLockManager`, and `NotificationHelper`.
- **`sensors/`**: Individual controllers for each hardware component (sensors, GPS, camera, audio).
- **`data/`**: `DataRepository` for managing local storage and data models for each data type.
- **`network/`**: `DataUploader` for handling communication with the remote server.
- **`util/`**: Utility objects for permissions, time, etc.

Refer to `plan.md` for a detailed breakdown of the architecture and data models.

## 📊 Data Visualization

### Quick Data Download & 3D Visualization

Download your phone's sensor data and visualize it in 3D:

```bash
# Download latest sensor data
./quick_download.sh

# Start the 3D visualizer
cd data-visualizer && npm start

# Open http://localhost:3000 and load visualizer_data.json
```

**Features:**
- **3D phone model** that responds to gyroscope/accelerometer data
- **GPS mini-map** with location tracking
- **Real-time sensor values** display
- **Dark theme** optimized for projection
- **Automatic device detection** (no configuration needed)

## 🚀 Quick Start

1. **Permissions**: The app will request the following permissions on first launch:

   - `FOREGROUND_SERVICE`
   - `WAKE_LOCK`
   - `RECORD_AUDIO`
   - `ACCESS_FINE_LOCATION`
   - `BODY_SENSORS`
   - `CAMERA`
   - `INTERNET`
   - `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`
   - `RECEIVE_BOOT_COMPLETED`

2. **Start Logging**: Use the button in the main activity to start the `SensorLoggingService`.
3. **Status**: A persistent notification will indicate that the logging service is active.

## Development

This project is built with Kotlin and Android Studio. Key dependencies will include:

- AndroidX Libraries (AppCompat, Core KTX)
- CameraX
- Room (for database)
- Retrofit (for networking)
- Coroutines (for async operations)
- FusedLocationProvider (for GPS)
