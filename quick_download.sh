#!/bin/bash

# Quick download script for sensor data
# Downloads data from the most recent device automatically

SERVER_URL="https://amazon.government.rip"

echo "📱 Quick Download: Latest Device Data"

# Download and convert in one go
curl -s "$SERVER_URL/data/sensors?limit=1000" | \
node -e "
const fs = require('fs');
const data = JSON.parse(require('fs').readFileSync(0, 'utf8'));
if (data.data && data.data.length > 0) {
    const visualizerData = data.data.map(r => ({
        timestamp: r.timestamp,
        gyro_x: r.gyro_x, gyro_y: r.gyro_y, gyro_z: r.gyro_z,
        accel_x: r.accel_x, accel_y: r.accel_y, accel_z: r.accel_z,
        latitude: r.latitude, longitude: r.longitude, accuracy: r.accuracy,
        pressure: r.pressure, altitude: r.altitude, device_id: r.device_id
    }));
    fs.writeFileSync('data-visualizer/visualizer_data.json', JSON.stringify(visualizerData, null, 2));
    console.log('✅ Downloaded', visualizerData.length, 'records to data-visualizer/visualizer_data.json');
    console.log('📱 Device ID:', data.data[0].device_id);
} else {
    console.log('❌ No data found');
}
" 