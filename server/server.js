const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const multer = require('multer');
const sqlite3 = require('sqlite3').verbose();
const path = require('path');
const fs = require('fs');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(bodyParser.json({ limit: '50mb' }));
app.use(bodyParser.urlencoded({ extended: true, limit: '50mb' }));

// File upload configuration - use persistent storage
const uploadDir = process.env.UPLOAD_DIR || '/app/data/uploads';
const upload = multer({ 
    dest: uploadDir,
    limits: { fileSize: 100 * 1024 * 1024 } // 100MB limit
});

// Database setup - use persistent storage path
const dbPath = process.env.DB_PATH || '/app/data/sensor_data.db';
const db = new sqlite3.Database(dbPath);

// Create tables if they don't exist
db.serialize(() => {
    // Sensor data table
    db.run(`CREATE TABLE IF NOT EXISTS sensor_data (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp INTEGER,
        gyro_x REAL,
        gyro_y REAL,
        gyro_z REAL,
        accel_x REAL,
        accel_y REAL,
        accel_z REAL,
        device_id TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )`);

    // Location data table
    db.run(`CREATE TABLE IF NOT EXISTS location_data (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp INTEGER,
        latitude REAL,
        longitude REAL,
        accuracy REAL,
        device_id TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )`);

    // Barometer data table
    db.run(`CREATE TABLE IF NOT EXISTS barometer_data (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp INTEGER,
        pressure REAL,
        altitude REAL,
        device_id TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )`);

    // Audio files table
    db.run(`CREATE TABLE IF NOT EXISTS audio_files (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp INTEGER,
        file_path TEXT,
        duration INTEGER,
        device_id TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )`);

    // Photo files table
    db.run(`CREATE TABLE IF NOT EXISTS photo_files (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp INTEGER,
        file_path TEXT,
        device_id TEXT,
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )`);
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ 
        status: 'ok', 
        timestamp: new Date().toISOString(),
        uptime: process.uptime()
    });
});

// Main data upload endpoint
app.post('/upload', (req, res) => {
    try {
        const { sensors, locations, barometers, audios, photos, deviceId } = req.body;
        
        console.log(`Received data from device ${deviceId}:`, {
            sensors: sensors?.length || 0,
            locations: locations?.length || 0,
            barometers: barometers?.length || 0,
            audios: audios?.length || 0,
            photos: photos?.length || 0
        });

        // Insert sensor data
        if (sensors && sensors.length > 0) {
            const stmt = db.prepare(`INSERT INTO sensor_data 
                (timestamp, gyro_x, gyro_y, gyro_z, accel_x, accel_y, accel_z, device_id) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)`);
            
            sensors.forEach(sensor => {
                stmt.run([
                    sensor.timestamp,
                    sensor.gyroX,
                    sensor.gyroY,
                    sensor.gyroZ,
                    sensor.accelX,
                    sensor.accelY,
                    sensor.accelZ,
                    deviceId
                ]);
            });
            stmt.finalize();
        }

        // Insert location data
        if (locations && locations.length > 0) {
            const stmt = db.prepare(`INSERT INTO location_data 
                (timestamp, latitude, longitude, accuracy, device_id) 
                VALUES (?, ?, ?, ?, ?)`);
            
            locations.forEach(location => {
                stmt.run([
                    location.timestamp,
                    location.latitude,
                    location.longitude,
                    location.accuracy,
                    deviceId
                ]);
            });
            stmt.finalize();
        }

        // Insert barometer data
        if (barometers && barometers.length > 0) {
            const stmt = db.prepare(`INSERT INTO barometer_data 
                (timestamp, pressure, altitude, device_id) 
                VALUES (?, ?, ?, ?)`);
            
            barometers.forEach(barometer => {
                stmt.run([
                    barometer.timestamp,
                    barometer.pressure,
                    barometer.altitude,
                    deviceId
                ]);
            });
            stmt.finalize();
        }

        res.json({ 
            success: true, 
            message: 'Data uploaded successfully',
            received: {
                sensors: sensors?.length || 0,
                locations: locations?.length || 0,
                barometers: barometers?.length || 0,
                audios: audios?.length || 0,
                photos: photos?.length || 0
            }
        });

    } catch (error) {
        console.error('Upload error:', error);
        res.status(500).json({ 
            success: false, 
            error: error.message 
        });
    }
});

// File upload endpoint for audio/photos
app.post('/upload-files', upload.array('files', 10), (req, res) => {
    try {
        const files = req.files;
        const { deviceId, fileType } = req.body; // fileType: 'audio' or 'photo'
        
        console.log(`Received ${files.length} ${fileType} files from device ${deviceId}`);

        const tableName = fileType === 'audio' ? 'audio_files' : 'photo_files';
        const stmt = db.prepare(`INSERT INTO ${tableName} 
            (timestamp, file_path, duration, device_id) 
            VALUES (?, ?, ?, ?)`);

        files.forEach(file => {
            const timestamp = Date.now();
            const duration = fileType === 'audio' ? req.body.duration : null;
            
            stmt.run([
                timestamp,
                file.path,
                duration,
                deviceId
            ]);
        });
        
        stmt.finalize();

        res.json({ 
            success: true, 
            message: `${files.length} ${fileType} files uploaded successfully` 
        });

    } catch (error) {
        console.error('File upload error:', error);
        res.status(500).json({ 
            success: false, 
            error: error.message 
        });
    }
});

// Get data summary endpoint
app.get('/summary', (req, res) => {
    db.all(`SELECT 
        COUNT(*) as total_sensors,
        COUNT(DISTINCT device_id) as unique_devices,
        MIN(created_at) as first_record,
        MAX(created_at) as last_record
        FROM sensor_data`, (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            res.json(rows[0]);
        }
    });
});

// Get all sensor data
app.get('/data/sensors', (req, res) => {
    const limit = req.query.limit || 100;
    const deviceId = req.query.device;
    
    let query = `SELECT * FROM sensor_data ORDER BY timestamp DESC LIMIT ?`;
    let params = [limit];
    
    if (deviceId) {
        query = `SELECT * FROM sensor_data WHERE device_id = ? ORDER BY timestamp DESC LIMIT ?`;
        params = [deviceId, limit];
    }
    
    db.all(query, params, (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            res.json({
                count: rows.length,
                data: rows
            });
        }
    });
});

// Get all location data
app.get('/data/locations', (req, res) => {
    const limit = req.query.limit || 100;
    const deviceId = req.query.device;
    
    let query = `SELECT * FROM location_data ORDER BY timestamp DESC LIMIT ?`;
    let params = [limit];
    
    if (deviceId) {
        query = `SELECT * FROM location_data WHERE device_id = ? ORDER BY timestamp DESC LIMIT ?`;
        params = [deviceId, limit];
    }
    
    db.all(query, params, (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            res.json({
                count: rows.length,
                data: rows
            });
        }
    });
});

// Get all barometer data
app.get('/data/barometers', (req, res) => {
    const limit = req.query.limit || 100;
    const deviceId = req.query.device;
    
    let query = `SELECT * FROM barometer_data ORDER BY timestamp DESC LIMIT ?`;
    let params = [limit];
    
    if (deviceId) {
        query = `SELECT * FROM barometer_data WHERE device_id = ? ORDER BY timestamp DESC LIMIT ?`;
        params = [deviceId, limit];
    }
    
    db.all(query, params, (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            res.json({
                count: rows.length,
                data: rows
            });
        }
    });
});

// Get all data for a specific device
app.get('/data/device/:deviceId', (req, res) => {
    const deviceId = req.params.deviceId;
    const limit = req.query.limit || 50;
    
    db.all(`SELECT 
        'sensor' as type,
        timestamp,
        gyro_x, gyro_y, gyro_z,
        accel_x, accel_y, accel_z,
        NULL as latitude, NULL as longitude, NULL as accuracy,
        NULL as pressure, NULL as altitude
        FROM sensor_data WHERE device_id = ?
    UNION ALL
    SELECT 
        'location' as type,
        timestamp,
        NULL as gyro_x, NULL as gyro_y, NULL as gyro_z,
        NULL as accel_x, NULL as accel_y, NULL as accel_z,
        latitude, longitude, accuracy,
        NULL as pressure, NULL as altitude
        FROM location_data WHERE device_id = ?
    UNION ALL
    SELECT 
        'barometer' as type,
        timestamp,
        NULL as gyro_x, NULL as gyro_y, NULL as gyro_z,
        NULL as accel_x, NULL as accel_y, NULL as accel_z,
        NULL as latitude, NULL as longitude, NULL as accuracy,
        pressure, altitude
        FROM barometer_data WHERE device_id = ?
    ORDER BY timestamp DESC LIMIT ?`, [deviceId, deviceId, deviceId, limit], (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            res.json({
                deviceId: deviceId,
                count: rows.length,
                data: rows
            });
        }
    });
});

// Download sensor data as CSV
app.get('/download/sensors', (req, res) => {
    const deviceId = req.query.device;
    const limit = req.query.limit || 1000;
    
    let query = `SELECT * FROM sensor_data ORDER BY timestamp DESC LIMIT ?`;
    let params = [limit];
    
    if (deviceId) {
        query = `SELECT * FROM sensor_data WHERE device_id = ? ORDER BY timestamp DESC LIMIT ?`;
        params = [deviceId, limit];
    }
    
    db.all(query, params, (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            // Convert to CSV
            const csvHeader = 'timestamp,gyro_x,gyro_y,gyro_z,accel_x,accel_y,accel_z,device_id,created_at\n';
            const csvData = rows.map(row => 
                `${row.timestamp},${row.gyro_x},${row.gyro_y},${row.gyro_z},${row.accel_x},${row.accel_y},${row.accel_z},${row.device_id},${row.created_at}`
            ).join('\n');
            
            res.setHeader('Content-Type', 'text/csv');
            res.setHeader('Content-Disposition', `attachment; filename="sensor_data_${Date.now()}.csv"`);
            res.send(csvHeader + csvData);
        }
    });
});

// Download location data as CSV
app.get('/download/locations', (req, res) => {
    const deviceId = req.query.device;
    const limit = req.query.limit || 1000;
    
    let query = `SELECT * FROM location_data ORDER BY timestamp DESC LIMIT ?`;
    let params = [limit];
    
    if (deviceId) {
        query = `SELECT * FROM location_data WHERE device_id = ? ORDER BY timestamp DESC LIMIT ?`;
        params = [deviceId, limit];
    }
    
    db.all(query, params, (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            // Convert to CSV
            const csvHeader = 'timestamp,latitude,longitude,accuracy,device_id,created_at\n';
            const csvData = rows.map(row => 
                `${row.timestamp},${row.latitude},${row.longitude},${row.accuracy},${row.device_id},${row.created_at}`
            ).join('\n');
            
            res.setHeader('Content-Type', 'text/csv');
            res.setHeader('Content-Disposition', `attachment; filename="location_data_${Date.now()}.csv"`);
            res.send(csvHeader + csvData);
        }
    });
});

// Download barometer data as CSV
app.get('/download/barometers', (req, res) => {
    const deviceId = req.query.device;
    const limit = req.query.limit || 1000;
    
    let query = `SELECT * FROM barometer_data ORDER BY timestamp DESC LIMIT ?`;
    let params = [limit];
    
    if (deviceId) {
        query = `SELECT * FROM barometer_data WHERE device_id = ? ORDER BY timestamp DESC LIMIT ?`;
        params = [deviceId, limit];
    }
    
    db.all(query, params, (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            // Convert to CSV
            const csvHeader = 'timestamp,pressure,altitude,device_id,created_at\n';
            const csvData = rows.map(row => 
                `${row.timestamp},${row.pressure},${row.altitude},${row.device_id},${row.created_at}`
            ).join('\n');
            
            res.setHeader('Content-Type', 'text/csv');
            res.setHeader('Content-Disposition', `attachment; filename="barometer_data_${Date.now()}.csv"`);
            res.send(csvHeader + csvData);
        }
    });
});

// Download all data as JSON
app.get('/download/all', (req, res) => {
    const deviceId = req.query.device;
    const limit = req.query.limit || 1000;
    
    if (!deviceId) {
        return res.status(400).json({ error: 'device parameter is required for /download/all' });
    }
    
    db.all(`SELECT 
        'sensor' as type,
        timestamp,
        gyro_x, gyro_y, gyro_z,
        accel_x, accel_y, accel_z,
        NULL as latitude, NULL as longitude, NULL as accuracy,
        NULL as pressure, NULL as altitude,
        device_id, created_at
        FROM sensor_data WHERE device_id = ?
    UNION ALL
    SELECT 
        'location' as type,
        timestamp,
        NULL as gyro_x, NULL as gyro_y, NULL as gyro_z,
        NULL as accel_x, NULL as accel_y, NULL as accel_z,
        latitude, longitude, accuracy,
        NULL as pressure, NULL as altitude,
        device_id, created_at
        FROM location_data WHERE device_id = ?
    UNION ALL
    SELECT 
        'barometer' as type,
        timestamp,
        NULL as gyro_x, NULL as gyro_y, NULL as gyro_z,
        NULL as accel_x, NULL as accel_y, NULL as accel_z,
        NULL as latitude, NULL as longitude, NULL as accuracy,
        pressure, altitude,
        device_id, created_at
        FROM barometer_data WHERE device_id = ?
    ORDER BY timestamp DESC LIMIT ?`, [deviceId, deviceId, deviceId, limit], (err, rows) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            res.setHeader('Content-Type', 'application/json');
            res.setHeader('Content-Disposition', `attachment; filename="all_data_${deviceId}_${Date.now()}.json"`);
            res.json({
                deviceId: deviceId,
                count: rows.length,
                data: rows
            });
        }
    });
});

// Start server
app.listen(PORT, () => {
    console.log(`Sensor data server running on port ${PORT}`);
    console.log(`Health check: http://localhost:${PORT}/health`);
    console.log(`Upload endpoint: http://localhost:${PORT}/upload`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
    console.log('SIGTERM received, shutting down gracefully');
    db.close();
    process.exit(0);
}); 