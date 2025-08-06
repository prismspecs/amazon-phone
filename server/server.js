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

// Add error handling for database path
console.log('Database path:', dbPath);
console.log('Upload directory:', uploadDir);

// Check if data directory exists
const dataDir = path.dirname(dbPath);
if (!fs.existsSync(dataDir)) {
    console.log('Creating data directory:', dataDir);
    fs.mkdirSync(dataDir, { recursive: true });
}

// Check if upload directory exists
if (!fs.existsSync(uploadDir)) {
    console.log('Creating upload directory:', uploadDir);
    fs.mkdirSync(uploadDir, { recursive: true });
}

const db = new sqlite3.Database(dbPath, (err) => {
    if (err) {
        console.error('Database connection error:', err);
        process.exit(1);
    } else {
        console.log('Database connected successfully');
    }
});

// Create tables if they don't exist
db.serialize(() => {
    // Unified sensor data table (includes all sensor types)
    db.run(`CREATE TABLE IF NOT EXISTS sensor_data (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        timestamp INTEGER,
        gyro_x REAL,
        gyro_y REAL,
        gyro_z REAL,
        accel_x REAL,
        accel_y REAL,
        accel_z REAL,
        latitude REAL,
        longitude REAL,
        accuracy REAL,
        pressure REAL,
        altitude REAL,
        device_id TEXT,
        type TEXT DEFAULT 'unified',
        created_at DATETIME DEFAULT CURRENT_TIMESTAMP
    )`);
});

// Migration: Add new columns to existing sensor_data table if they don't exist
// This runs after table creation to ensure the table exists
setTimeout(() => {
    console.log('Running database migration...');
    db.all(`PRAGMA table_info(sensor_data)`, (err, rows) => {
        if (err) {
            console.error('Migration error checking table info:', err);
            return;
        }
        
        if (rows) {
            const existingColumns = rows.map(row => row.name);
            console.log('Existing columns:', existingColumns);
            
            // Add missing columns
            if (!existingColumns.includes('latitude')) {
                db.run(`ALTER TABLE sensor_data ADD COLUMN latitude REAL`, (err) => {
                    if (err) console.error('Error adding latitude column:', err);
                    else console.log('Added latitude column to sensor_data table');
                });
            }
            if (!existingColumns.includes('longitude')) {
                db.run(`ALTER TABLE sensor_data ADD COLUMN longitude REAL`, (err) => {
                    if (err) console.error('Error adding longitude column:', err);
                    else console.log('Added longitude column to sensor_data table');
                });
            }
            if (!existingColumns.includes('accuracy')) {
                db.run(`ALTER TABLE sensor_data ADD COLUMN accuracy REAL`, (err) => {
                    if (err) console.error('Error adding accuracy column:', err);
                    else console.log('Added accuracy column to sensor_data table');
                });
            }
            if (!existingColumns.includes('pressure')) {
                db.run(`ALTER TABLE sensor_data ADD COLUMN pressure REAL`, (err) => {
                    if (err) console.error('Error adding pressure column:', err);
                    else console.log('Added pressure column to sensor_data table');
                });
            }
            if (!existingColumns.includes('altitude')) {
                db.run(`ALTER TABLE sensor_data ADD COLUMN altitude REAL`, (err) => {
                    if (err) console.error('Error adding altitude column:', err);
                    else console.log('Added altitude column to sensor_data table');
                });
            }
            if (!existingColumns.includes('type')) {
                db.run(`ALTER TABLE sensor_data ADD COLUMN type TEXT DEFAULT 'unified'`, (err) => {
                    if (err) console.error('Error adding type column:', err);
                    else console.log('Added type column to sensor_data table');
                });
            }
        }
    });
}, 1000); // Wait 1 second for table creation to complete

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
        const { data, count, deviceId, audios, photos } = req.body;
        
        console.log(`Received unified data from device ${deviceId}:`, {
            count: count || 0,
            data: data?.length || 0,
            audios: audios?.length || 0,
            photos: photos?.length || 0
        });

        // Insert unified sensor data
        if (data && data.length > 0) {
            const stmt = db.prepare(`INSERT INTO sensor_data 
                (timestamp, gyro_x, gyro_y, gyro_z, accel_x, accel_y, accel_z, 
                 latitude, longitude, accuracy, pressure, altitude, device_id, type) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`);
            
            data.forEach(record => {
                stmt.run([
                    record.timestamp,
                    record.gyro_x,
                    record.gyro_y,
                    record.gyro_z,
                    record.accel_x,
                    record.accel_y,
                    record.accel_z,
                    record.latitude,
                    record.longitude,
                    record.accuracy,
                    record.pressure,
                    record.altitude,
                    record.device_id,
                    record.type
                ]);
            });
            stmt.finalize();
        }

        res.json({ 
            success: true, 
            message: 'Unified data uploaded successfully',
            received: {
                count: count || 0,
                data: data?.length || 0,
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
    const startTime = req.query.start_time;
    const endTime = req.query.end_time;
    
    let query = `SELECT * FROM sensor_data`;
    let params = [];
    let conditions = [];
    
    if (deviceId) {
        conditions.push('device_id = ?');
        params.push(deviceId);
    }
    
    if (startTime && endTime) {
        // For reverse chronological order, we want records between endTime and startTime
        // since newer records have higher timestamps
        conditions.push('timestamp >= ? AND timestamp <= ?');
        params.push(endTime, startTime);
    } else if (startTime) {
        conditions.push('timestamp <= ?');
        params.push(startTime);
    } else if (endTime) {
        conditions.push('timestamp >= ?');
        params.push(endTime);
    }
    
    if (conditions.length > 0) {
        query += ' WHERE ' + conditions.join(' AND ');
    }
    
    query += ' ORDER BY timestamp DESC LIMIT ?';
    params.push(limit);
    
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

// ===== DELETE ENDPOINTS =====

// Delete all sensor data
app.delete('/data/sensors', (req, res) => {
    const deviceId = req.query.device;
    const beforeDate = req.query.before; // ISO date string
    const afterDate = req.query.after;   // ISO date string
    
    let query = 'DELETE FROM sensor_data';
    let params = [];
    let conditions = [];
    
    if (deviceId) {
        conditions.push('device_id = ?');
        params.push(deviceId);
    }
    
    if (beforeDate) {
        conditions.push('created_at < ?');
        params.push(beforeDate);
    }
    
    if (afterDate) {
        conditions.push('created_at > ?');
        params.push(afterDate);
    }
    
    if (conditions.length > 0) {
        query += ' WHERE ' + conditions.join(' AND ');
    }
    
    db.run(query, params, function(err) {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            res.json({ 
                success: true, 
                message: `Deleted ${this.changes} sensor records`,
                deletedCount: this.changes
            });
        }
    });
});

// Delete all location data
app.delete('/data/locations', (req, res) => {
    const deviceId = req.query.device;
    const beforeDate = req.query.before;
    const afterDate = req.query.after;
    
    let query = 'DELETE FROM location_data';
    let params = [];
    let conditions = [];
    
    if (deviceId) {
        conditions.push('device_id = ?');
        params.push(deviceId);
    }
    
    if (beforeDate) {
        conditions.push('created_at < ?');
        params.push(beforeDate);
    }
    
    if (afterDate) {
        conditions.push('created_at > ?');
        params.push(afterDate);
    }
    
    if (conditions.length > 0) {
        query += ' WHERE ' + conditions.join(' AND ');
    }
    
    db.run(query, params, function(err) {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            res.json({ 
                success: true, 
                message: `Deleted ${this.changes} location records`,
                deletedCount: this.changes
            });
        }
    });
});

// Delete all barometer data
app.delete('/data/barometers', (req, res) => {
    const deviceId = req.query.device;
    const beforeDate = req.query.before;
    const afterDate = req.query.after;
    
    let query = 'DELETE FROM barometer_data';
    let params = [];
    let conditions = [];
    
    if (deviceId) {
        conditions.push('device_id = ?');
        params.push(deviceId);
    }
    
    if (beforeDate) {
        conditions.push('created_at < ?');
        params.push(beforeDate);
    }
    
    if (afterDate) {
        conditions.push('created_at > ?');
        params.push(afterDate);
    }
    
    if (conditions.length > 0) {
        query += ' WHERE ' + conditions.join(' AND ');
    }
    
    db.run(query, params, function(err) {
        if (err) {
            res.status(500).json({ error: err.message });
        } else {
            res.json({ 
                success: true, 
                message: `Deleted ${this.changes} barometer records`,
                deletedCount: this.changes
            });
        }
    });
});

// Delete all data for a specific device
app.delete('/data/device/:deviceId', (req, res) => {
    const deviceId = req.params.deviceId;
    const beforeDate = req.query.before;
    const afterDate = req.query.after;
    
    let conditions = [];
    let params = [deviceId];
    
    if (beforeDate) {
        conditions.push('created_at < ?');
        params.push(beforeDate);
    }
    
    if (afterDate) {
        conditions.push('created_at > ?');
        params.push(afterDate);
    }
    
    const whereClause = conditions.length > 0 ? ' AND ' + conditions.join(' AND ') : '';
    
    // Delete from all tables
    const queries = [
        `DELETE FROM sensor_data WHERE device_id = ?${whereClause}`,
        `DELETE FROM location_data WHERE device_id = ?${whereClause}`,
        `DELETE FROM barometer_data WHERE device_id = ?${whereClause}`,
        `DELETE FROM audio_files WHERE device_id = ?${whereClause}`,
        `DELETE FROM photo_files WHERE device_id = ?${whereClause}`
    ];
    
    let totalDeleted = 0;
    let completedQueries = 0;
    const totalQueries = queries.length;
    
    queries.forEach((query, index) => {
        db.run(query, params, function(err) {
            if (err) {
                return res.status(500).json({ error: err.message });
            }
            
            totalDeleted += this.changes;
            completedQueries++;
            
            if (completedQueries === totalQueries) {
                res.json({ 
                    success: true, 
                    message: `Deleted ${totalDeleted} total records for device ${deviceId}`,
                    deletedCount: totalDeleted
                });
            }
        });
    });
});

// Delete all data (NUCLEAR OPTION - use with caution!)
app.delete('/data/all', (req, res) => {
    const confirm = req.query.confirm;
    
    if (confirm !== 'true') {
        return res.status(400).json({ 
            error: 'This will delete ALL data. Add ?confirm=true to proceed.',
            warning: 'This is irreversible!'
        });
    }
    
    const queries = [
        'DELETE FROM sensor_data',
        'DELETE FROM location_data', 
        'DELETE FROM barometer_data',
        'DELETE FROM audio_files',
        'DELETE FROM photo_files'
    ];
    
    let totalDeleted = 0;
    let completedQueries = 0;
    const totalQueries = queries.length;
    
    queries.forEach((query, index) => {
        db.run(query, [], function(err) {
            if (err) {
                return res.status(500).json({ error: err.message });
            }
            
            totalDeleted += this.changes;
            completedQueries++;
            
            if (completedQueries === totalQueries) {
                res.json({ 
                    success: true, 
                    message: `Deleted ${totalDeleted} total records from all tables`,
                    deletedCount: totalDeleted,
                    warning: 'All data has been permanently deleted!'
                });
            }
        });
    });
});

// Get all audio files
app.get('/data/audio', (req, res) => {
    const limit = req.query.limit || 100;
    const deviceId = req.query.device;
    
    let query = `SELECT * FROM audio_files ORDER BY timestamp DESC LIMIT ?`;
    let params = [limit];
    
    if (deviceId) {
        query = `SELECT * FROM audio_files WHERE device_id = ? ORDER BY timestamp DESC LIMIT ?`;
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

// Get audio file by ID
app.get('/data/audio/:id', (req, res) => {
    const audioId = req.params.id;
    
    db.get(`SELECT * FROM audio_files WHERE id = ?`, [audioId], (err, row) => {
        if (err) {
            res.status(500).json({ error: err.message });
        } else if (!row) {
            res.status(404).json({ error: 'Audio file not found' });
        } else {
            res.json(row);
        }
    });
});

// Download audio file
app.get('/download/audio/:id', (req, res) => {
    const audioId = req.params.id;
    
    db.get(`SELECT * FROM audio_files WHERE id = ?`, [audioId], (err, row) => {
        if (err) {
            return res.status(500).json({ error: err.message });
        } else if (!row) {
            return res.status(404).json({ error: 'Audio file not found' });
        }
        
        const filePath = row.file_path;
        if (!fs.existsSync(filePath)) {
            return res.status(404).json({ error: 'Audio file not found on disk' });
        }
        
        // Extract filename from path
        const filename = path.basename(filePath);
        
        res.setHeader('Content-Type', 'audio/mp4');
        res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
        
        const fileStream = fs.createReadStream(filePath);
        fileStream.pipe(res);
    });
});

// Get all photo files
app.get('/data/photos', (req, res) => {
    const limit = req.query.limit || 100;
    const deviceId = req.query.device;
    
    let query = `SELECT * FROM photo_files ORDER BY timestamp DESC LIMIT ?`;
    let params = [limit];
    
    if (deviceId) {
        query = `SELECT * FROM photo_files WHERE device_id = ? ORDER BY timestamp DESC LIMIT ?`;
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

// Download photo file
app.get('/download/photo/:id', (req, res) => {
    const photoId = req.params.id;
    
    db.get(`SELECT * FROM photo_files WHERE id = ?`, [photoId], (err, row) => {
        if (err) {
            return res.status(500).json({ error: err.message });
        } else if (!row) {
            return res.status(404).json({ error: 'Photo file not found' });
        }
        
        const filePath = row.file_path;
        if (!fs.existsSync(filePath)) {
            return res.status(404).json({ error: 'Photo file not found on disk' });
        }
        
        // Extract filename from path
        const filename = path.basename(filePath);
        
        res.setHeader('Content-Type', 'image/jpeg');
        res.setHeader('Content-Disposition', `attachment; filename="${filename}"`);
        
        const fileStream = fs.createReadStream(filePath);
        fileStream.pipe(res);
    });
});

// Delete data older than X days
app.delete('/data/older-than/:days', (req, res) => {
    const days = parseInt(req.params.days);
    const deviceId = req.query.device;
    
    if (isNaN(days) || days < 1) {
        return res.status(400).json({ error: 'Days must be a positive number' });
    }
    
    const cutoffDate = new Date();
    cutoffDate.setDate(cutoffDate.getDate() - days);
    const cutoffISO = cutoffDate.toISOString();
    
    let conditions = ['created_at < ?'];
    let params = [cutoffISO];
    
    if (deviceId) {
        conditions.push('device_id = ?');
        params.push(deviceId);
    }
    
    const whereClause = conditions.join(' AND ');
    
    const queries = [
        `DELETE FROM sensor_data WHERE ${whereClause}`,
        `DELETE FROM location_data WHERE ${whereClause}`,
        `DELETE FROM barometer_data WHERE ${whereClause}`,
        `DELETE FROM audio_files WHERE ${whereClause}`,
        `DELETE FROM photo_files WHERE ${whereClause}`
    ];
    
    let totalDeleted = 0;
    let completedQueries = 0;
    const totalQueries = queries.length;
    
    queries.forEach((query, index) => {
        db.run(query, params, function(err) {
            if (err) {
                return res.status(500).json({ error: err.message });
            }
            
            totalDeleted += this.changes;
            completedQueries++;
            
            if (completedQueries === totalQueries) {
                res.json({ 
                    success: true, 
                    message: `Deleted ${totalDeleted} records older than ${days} days`,
                    deletedCount: totalDeleted,
                    cutoffDate: cutoffISO
                });
            }
        });
    });
});

// Start server
app.listen(PORT, () => {
    console.log(`Sensor data server running on port ${PORT}`);
    console.log(`Health check: http://localhost:${PORT}/health`);
    console.log(`Upload endpoint: http://localhost:${PORT}/upload`);
    console.log(`Delete endpoints available at /data/sensors, /data/locations, /data/barometers, /data/device/:id, /data/all, /data/older-than/:days`);
});

// Graceful shutdown
process.on('SIGTERM', () => {
    console.log('SIGTERM received, shutting down gracefully');
    db.close();
    process.exit(0);
}); 