#!/usr/bin/env node

// Test script to verify Android sensor rotation mapping
const fs = require('fs');

try {
    const data = JSON.parse(fs.readFileSync('data-visualizer/visualizer_data.json', 'utf8'));
    
    console.log('📱 Android Sensor Rotation Mapping Test');
    console.log('=====================================');
    console.log(`Total records: ${data.length}`);
    console.log('');
    
    // Calculate bias
    const gyroX = data.map(r => r.gyro_x).filter(x => x !== null);
    const gyroY = data.map(r => r.gyro_y).filter(x => x !== null);
    const gyroZ = data.map(r => r.gyro_z).filter(x => x !== null);
    
    const biasX = gyroX.reduce((a, b) => a + b, 0) / gyroX.length;
    const biasY = gyroY.reduce((a, b) => a + b, 0) / gyroY.length;
    const biasZ = gyroZ.reduce((a, b) => a + b, 0) / gyroZ.length;
    
    console.log('🔧 Bias Correction:');
    console.log(`  X-axis bias: ${biasX.toFixed(6)}`);
    console.log(`  Y-axis bias: ${biasY.toFixed(6)}`);
    console.log(`  Z-axis bias: ${biasZ.toFixed(6)}`);
    console.log('');
    
    // Show sample rotation calculations
    console.log('🔄 Rotation Mapping Examples:');
    console.log('============================');
    
    data.slice(0, 5).forEach((record, i) => {
        const gyroX = (record.gyro_x || 0) - biasX;
        const gyroY = (record.gyro_y || 0) - biasY;
        const gyroZ = (record.gyro_z || 0) - biasZ;
        
        // Convert to degrees/second
        const gyroXDeg = gyroX * (180 / Math.PI) * 50;
        const gyroYDeg = gyroY * (180 / Math.PI) * 50;
        const gyroZDeg = gyroZ * (180 / Math.PI) * 50;
        
        console.log(`Record ${i + 1}:`);
        console.log(`  Raw gyro (rad/s): X=${(record.gyro_x || 0).toFixed(6)}, Y=${(record.gyro_y || 0).toFixed(6)}, Z=${(record.gyro_z || 0).toFixed(6)}`);
        console.log(`  Corrected (rad/s): X=${gyroX.toFixed(6)}, Y=${gyroY.toFixed(6)}, Z=${gyroZ.toFixed(6)}`);
        console.log(`  Scaled (deg/s): X=${gyroXDeg.toFixed(2)}, Y=${gyroYDeg.toFixed(2)}, Z=${gyroZDeg.toFixed(2)}`);
        console.log(`  Rotation effect:`);
        console.log(`    X-axis: ${gyroXDeg > 0 ? 'Forward tilt' : 'Backward tilt'} (${Math.abs(gyroXDeg).toFixed(2)}°/s)`);
        console.log(`    Y-axis: ${gyroYDeg > 0 ? 'Right turn' : 'Left turn'} (${Math.abs(gyroYDeg).toFixed(2)}°/s)`);
        console.log(`    Z-axis: ${gyroZDeg > 0 ? 'Right tilt' : 'Left tilt'} (${Math.abs(gyroZDeg).toFixed(2)}°/s)`);
        console.log('');
    });
    
    // Show accelerometer data if available
    const accelData = data.filter(r => r.accel_x !== null || r.accel_y !== null || r.accel_z !== null);
    if (accelData.length > 0) {
        console.log('📊 Accelerometer Data:');
        console.log('======================');
        console.log(`Records with accel data: ${accelData.length}`);
        
        const sample = accelData[0];
        const magnitude = Math.sqrt(sample.accel_x * sample.accel_x + sample.accel_y * sample.accel_y + sample.accel_z * sample.accel_z);
        console.log(`Sample magnitude: ${magnitude.toFixed(2)} m/s² (should be ~9.8 when stationary)`);
        console.log(`Sample values: X=${sample.accel_x.toFixed(2)}, Y=${sample.accel_y.toFixed(2)}, Z=${sample.accel_z.toFixed(2)}`);
    }
    
    console.log('✅ Rotation mapping test complete!');
    console.log('');
    console.log('📋 Expected behavior:');
    console.log('- Phone should rotate smoothly based on gyroscope data');
    console.log('- No more leftward drift (bias corrected)');
    console.log('- Visible rotation for even small movements');
    console.log('- Screen glow intensity based on sensor activity');
    
} catch (error) {
    console.error('❌ Error:', error.message);
} 