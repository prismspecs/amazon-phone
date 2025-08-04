# Sensor Data Server

Minimal Node.js server to receive sensor data from Android app.

## Repository Structure

**Option 1: Separate Server Repository (Recommended)**
```
sensor-server-repo/
├── Dockerfile
├── package.json
├── server.js
├── captain-definition
└── README.md
```

**Option 2: Monorepo with Server Subdirectory**
```
sensor-project-repo/
├── server/          # Only this directory gets deployed
│   ├── Dockerfile
│   ├── package.json
│   ├── server.js
│   ├── captain-definition
│   └── README.md
├── app/             # Android app (not deployed)
└── plan.md
```

## Features

- **Data Upload**: Receives sensor, location, barometer data
- **File Upload**: Handles audio and photo uploads
- **SQLite Storage**: Lightweight database for data persistence
- **Health Check**: `/health` endpoint for monitoring
- **Data Summary**: `/summary` endpoint for statistics

## Endpoints

### POST `/upload`
Upload sensor data in JSON format:
```json
{
  "deviceId": "phone-123",
  "sensors": [
    {
      "timestamp": 1640995200000,
      "gyroX": 0.1,
      "gyroY": 0.2,
      "gyroZ": 0.3,
      "accelX": 9.8,
      "accelY": 0.1,
      "accelZ": 0.2
    }
  ],
  "locations": [
    {
      "timestamp": 1640995200000,
      "latitude": 40.7128,
      "longitude": -74.0060,
      "accuracy": 5.0
    }
  ],
  "barometers": [
    {
      "timestamp": 1640995200000,
      "pressure": 1013.25,
      "altitude": 0.0
    }
  ]
}
```

### POST `/upload-files`
Upload audio/photo files:
```
Content-Type: multipart/form-data
- files: [audio/photo files]
- deviceId: "phone-123"
- fileType: "audio" or "photo"
```

### GET `/health`
Health check endpoint.

### GET `/summary`
Get data statistics.

## Deployment with CapRover

### Option 1: Separate Server Repository

1. **Create new repository for server only:**
   ```bash
   mkdir sensor-server-repo
   cd sensor-server-repo
   # Copy only server files (Dockerfile, package.json, server.js, etc.)
   git init
   git add .
   git commit -m "Initial server setup"
   git remote add origin https://github.com/yourusername/sensor-server-repo.git
   git push -u origin main
   ```

2. **Deploy to CapRover:**
   - Go to CapRover dashboard
   - Click "One-Click Apps" → "Git"
   - Enter: `https://github.com/yourusername/sensor-server-repo.git`
   - Set app name: `sensor-server`
   - **Root Path**: `./` (root of repository)
   - Deploy

### Option 2: Monorepo with Subdirectory

1. **Keep current structure** with `server/` subdirectory
2. **Deploy to CapRover:**
   - Go to CapRover dashboard
   - Click "One-Click Apps" → "Git"
   - Enter your main repository URL
   - Set app name: `sensor-server`
   - **Root Path**: `./server` (important!)
   - Deploy

## Local Development

```bash
cd server
npm install
npm run dev
```

## Database

SQLite database (`sensor_data.db`) with tables:
- `sensor_data`: Gyroscope and accelerometer readings
- `location_data`: GPS coordinates
- `barometer_data`: Pressure and altitude
- `audio_files`: Audio file metadata
- `photo_files`: Photo file metadata

## Android App Integration

Update your Android app's `DataUploader.kt` to use your server URL:

```kotlin
private val endpoint = "https://your-app-name.your-domain.com/upload"
```

## Recommended Approach

**Use Option 1 (separate repository)** because:
- Cleaner separation of concerns
- Smaller deployment package
- Easier to manage server-specific dependencies
- No risk of accidentally deploying Android code
- Better for CI/CD pipelines 