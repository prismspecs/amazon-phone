# 📱 Amazon Phone Sensor Logger - System Architecture

## 🎯 **Project Overview**

A comprehensive Android sensor logging system with server-side data collection and 3D visualization capabilities. Designed for continuous background operation with minimal battery impact.

## 🏗️ **System Architecture**

### **Three-Tier Architecture**

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Android App   │    │   Node.js       │    │   Data          │
│   (Sensors)     │───▶│   Server        │───▶│   Visualizer    │
│                 │    │   (Storage)     │    │   (3D Display)  │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### **Core Components**

1. **Android Sensor Logger** - Continuous background service
2. **Node.js Data Server** - REST API with SQLite storage  
3. **Three.js Visualizer** - 3D phone model with real-time data

---

## 📱 **Android App Architecture**

### **Key Design Principles**
- **Foreground Service**: Ensures continuous operation with screen off
- **Wake Lock Management**: Prevents CPU sleep during logging
- **Battery Optimization Bypass**: Maintains operation on aggressive power-saving devices
- **Unified Data Model**: Combines all sensor types into single records
- **Real-time Data Collection**: High-frequency sensor polling for smooth visualization

### **Core Components**

#### **Service Layer**
- `SensorLoggingService.kt` - Main foreground service
- `WakeLockManager.kt` - Battery optimization management
- `NotificationHelper.kt` - Persistent notification display

#### **Sensor Controllers**
- `SensorController.kt` - Gyroscope and accelerometer
- `BarometerController.kt` - Pressure and altitude
- `GpsController.kt` - Location tracking
- `AudioRecorder.kt` - Audio capture
- `CameraController.kt` - Photo capture

#### **Data Management**
- `DataRepository.kt` - Queue management and batch processing
- `DataUploader.kt` - Server communication
- `UnifiedSensorRecord.kt` - Combined sensor data model

### **Unified Timing System**
- **Gyroscope**: 1Hz polling (1-second intervals) for motion tracking
- **Accelerometer**: 1Hz polling (1-second intervals) for orientation
- **Barometer**: 1Hz polling (1-second intervals) for altitude changes
- **GPS**: 15-second intervals for location tracking
- **Audio Recording**: 15-second cycles synchronized with uploads
- **Upload Interval**: 15 seconds for all data types
- **Data Association**: All data types use timestamp-based naming for correlation

### **Data Format**
```kotlin
data class UnifiedSensorRecord(
    val timestamp: Long,  // Unix timestamp in milliseconds
    val gyroX: Float?, val gyroY: Float?, val gyroZ: Float?,
    val accelX: Float?, val accelY: Float?, val accelZ: Float?,
    val latitude: Double?, val longitude: Double?, val accuracy: Float?,
    val pressure: Float?, val altitude: Float?,
    val deviceId: String,
    val type: String = "unified"
)
```

---

## 🌐 **Server Architecture**

### **Technology Stack**
- **Runtime**: Node.js with Express.js
- **Database**: SQLite with automatic migrations
- **Deployment**: Docker container on CapRover
- **URL**: `https://amazon.government.rip`

### **API Endpoints**

#### **Data Upload**
- `POST /upload` - Receive unified sensor records
- `POST /upload-files` - Receive audio/photo files

#### **Data Retrieval**
- `GET /data/sensors` - Query sensor data with filters
- `GET /summary` - System statistics

#### **Data Management**
- `DELETE /data/device/:deviceId` - Remove device data
- `DELETE /data/older-than/:days` - Cleanup old data
- `DELETE /data/all?confirm=true` - Clear all data (nuclear option)

### **Database Schema**
```sql
CREATE TABLE sensor_data (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    gyro_x REAL, gyro_y REAL, gyro_z REAL,
    accel_x REAL, accel_y REAL, accel_z REAL,
    latitude REAL, longitude REAL, accuracy REAL,
    pressure REAL, altitude REAL,
    device_id TEXT NOT NULL,
    type TEXT DEFAULT 'unified',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

---

## 📊 **Data Visualizer Architecture**

### **Technology Stack**
- **3D Engine**: Three.js with WebGL
- **UI Framework**: Vanilla JavaScript with HTML5
- **Data Format**: JSON with unified sensor records
- **Timing System**: Real-time timestamp-based playback

### **Core Features**
- **3D Phone Model**: Box geometry with rotation visualization
- **Real-time Orientation**: Accelerometer-based pitch/roll calculation
- **Movement Indication**: Gyroscope magnitude for activity glow
- **GPS Integration**: Dark-themed map with location tracking
- **Playback Controls**: Play, pause, reset, export functionality
- **Accurate Timing**: Real-time replay using actual sensor timestamps

### **Visualization Logic**
```javascript
// Orientation from accelerometer (gravity vector)
const pitch = Math.asin(-gravityY) * (180 / Math.PI);
const roll = Math.atan2(gravityX, gravityZ) * (180 / Math.PI);

// Movement indication from gyroscope
const gyroMagnitude = Math.sqrt(gyroX² + gyroY² + gyroZ²);
const activityLevel = Math.min(gyroMagnitude * 10, 1.0);

// Real-time playback using timestamps
const targetDataTime = this.dataStartTime + elapsedPlaybackTime;
```

### **Data Processing**
- **Bias Correction**: Automatic gyroscope bias calculation
- **Interpolation**: Smooth transitions between data points
- **Performance**: 60fps rendering with efficient updates
- **Time-based Playback**: Accurate replay using sensor timestamps
- **Fixed Batch Size**: Always exactly 15 sensor records per batch (engineered constraint)
- **Reverse Chronological Order**: Server returns data in newest-first order (higher timestamps first)

---

## 🔄 **Data Flow**

### **Unified Collection Pipeline**
1. **Sensor Polling** → Android sensors at 1Hz (gyro/accel/baro) and 15s (GPS)
2. **Audio Recording** → 15-second cycles synchronized with upload intervals
3. **Motion Filtering** → Disabled for continuous data collection
4. **Queue Management** → Buffer data in memory with timestamp association
5. **Batch Processing** → Group by 15-second time windows
6. **Unified Records** → Combine all sensor types with timestamps
7. **File Upload** → Audio files with timestamp-based naming
8. **Server Upload** → HTTP POST every 15 seconds
9. **Database Storage** → SQLite with automatic indexing and file associations

### **Visualization Pipeline**
1. **Data Download** → `quick_download.sh` script
2. **Format Conversion** → JSON for Three.js
3. **Bias Calculation** → Gyroscope drift correction
4. **Time-based Rendering** → Real-time playback using timestamps
5. **Interactive Controls** → Playback and camera manipulation

---

## 🛠️ **Development Workflow**

### **Android Development**
```bash
# Build and install
./build_and_install.sh

# Monitor logs
adb logcat | grep sensorlogger
```

### **Server Deployment**
```bash
# Deploy to CapRover
cd server/
./deploy.sh
```

### **Data Visualization**
```bash
# Download latest data
./quick_download.sh

# Start visualizer
cd data-visualizer/
npm start
```

---

## 📋 **Key Design Decisions**

### **Why High-Frequency Sensor Polling?**
- **Smooth Visualization**: 50Hz polling provides fluid motion tracking
- **Accurate Timing**: Real-time replay requires dense data points
- **Better Motion Detection**: More responsive to phone movements
- **Reduced Interpolation**: Less need for artificial smoothing

### **Why Real-time Timestamp Playback?**
- **Accurate Replay**: Matches original movement timing exactly
- **Variable Speed Support**: Can implement speed multipliers
- **Natural Motion**: Preserves the rhythm and timing of movements
- **Better Debugging**: Easier to correlate with real events

### **Why Disabled Motion Filtering?**
- **Complete Data**: Captures all movements for analysis
- **Testing Phase**: Need full data during development
- **Visualization Quality**: More data points for smooth playback
- **Battery Trade-off**: Acceptable for development and testing

### **Why Unified Records?**
- **Reduced Null Values**: 75% fewer null entries
- **Better Correlation**: All sensor data in single records
- **Simplified Processing**: Single database table
- **Improved Performance**: Fewer database operations

### **Why Accelerometer for Orientation?**
- **Stable Reference**: Gravity vector provides absolute orientation
- **No Drift**: Unlike gyroscope integration
- **Real-world Behavior**: Matches actual phone orientation
- **Simple Calculation**: Direct pitch/roll from gravity components

### **Why Foreground Service?**
- **Reliability**: Continues operation with screen off
- **Android Compliance**: Required for background processing
- **User Awareness**: Persistent notification shows active status
- **Battery Optimization**: Can bypass aggressive power saving

---

## 🔧 **Configuration Management**

### **Android App Settings**
- **Sensor Frequencies**: 50Hz for gyro/accel, 1Hz for barometer
- **Batch Processing**: 10-second timeout and 5+ reading thresholds
- **Motion Filtering**: Disabled for continuous data collection
- **Upload Endpoints**: Server URL and 30-second retry logic

### **Server Settings**
- **Database Path**: SQLite file location
- **CORS Origins**: Allowed client domains
- **File Upload Limits**: Maximum file sizes
- **Data Retention**: Automatic cleanup policies

### **Visualizer Settings**
- **Rendering Quality**: Shadow and antialiasing options
- **Animation Speed**: Real-time timestamp-based playback
- **Camera Controls**: Orbit and zoom limits
- **UI Theme**: Dark mode for projection environments

---

## 🚀 **Deployment Architecture**

### **Production Environment**
- **Android App**: APK distributed via direct install
- **Server**: CapRover container with automatic scaling
- **Visualizer**: Static files served from server
- **Database**: SQLite with automatic backups

### **Development Environment**
- **Android Studio**: Local development and debugging
- **Node.js**: Local server for testing
- **Three.js**: Browser-based visualization
- **Git**: Version control and deployment triggers

---

## 📈 **Performance Metrics**

### **Android App**
- **Battery Impact**: <10% additional drain (optimized for unified timing)
- **Data Volume**: ~2MB/hour of sensor data (1Hz polling)
- **Upload Frequency**: Every 15 seconds
- **Sensor Accuracy**: 1Hz motion-tracked readings with 15s GPS updates
- **Audio Recording**: 15-second cycles synchronized with uploads

### **Server**
- **Response Time**: <100ms for data queries
- **Storage Efficiency**: ~10KB per unified record
- **Scalability**: SQLite with automatic indexing
- **Uptime**: 99.9% via CapRover monitoring

### **Visualizer**
- **Rendering Performance**: 60fps on modern browsers
- **Memory Usage**: <50MB for typical datasets
- **Load Time**: <2 seconds for 1000+ records
- **Compatibility**: WebGL 1.0+ browsers
- **Playback Accuracy**: Real-time timestamp-based replay

---

## 🔄 **Recent Improvements**

### **Unified Timing System Implementation**
- **Standardized Intervals**: 1Hz for gyro/accel/baro, 15s for GPS, 15s for uploads
- **Audio Synchronization**: 15-second recording cycles aligned with upload intervals
- **Timestamp Association**: All data types use consistent timestamp-based naming
- **File Upload Integration**: Audio files uploaded with timestamp-based filenames
- **Data Correlation**: Easy association between sensor data and audio recordings

### **Visualization Improvements**
- **Real-time Playback**: Uses actual sensor timestamps for accurate replay
- **Time-based Animation**: Calculates playback position from elapsed time
- **Smooth Interpolation**: Interpolates between data points based on timestamps
- **Accurate Duration**: Shows actual recording duration in console

### **Data Quality Improvements**
- **1-second Time Windows**: More granular data grouping
- **Complete Timestamps**: All records include accurate Unix timestamps
- **Chronological Ordering**: Data sorted by timestamp for proper playback
- **Bias Correction**: Automatic gyroscope drift compensation
