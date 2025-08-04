#!/usr/bin/env node

// Test script for simple orientation approach
const fs = require('fs');

try {
    const data = JSON.parse(fs.readFileSync('data-visualizer/visualizer_data.json', 'utf8'));
    
    console.log('📱 Simple Orientation Test');
    console.log('=========================');
    console.log(`Total records: ${data.length}`);
    console.log('');
    
    // Find records with accelerometer data
    const accelRecords = data.filter(r => r.accel_x !== null && r.accel_y !== null && r.accel_z !== null);
    console.log(`Records with accelerometer data: ${accelRecords.length}`);
    console.log('');
    
    if (accelRecords.length > 0) {
        console.log('📊 Accelerometer Orientation Examples:');
        console.log('====================================');
        
        accelRecords.slice(0, 5).forEach((record, i) => {
            const accelX = record.accel_x;
            const accelY = record.accel_y;
            const accelZ = record.accel_z;
            
            // Calculate magnitude
            const magnitude = Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
            
            // Calculate orientation angles
            const gravityX = accelX / magnitude;
            const gravityY = accelY / magnitude;
            const gravityZ = accelZ / magnitude;
            
            const pitch = Math.asin(-gravityY) * (180 / Math.PI); // Forward/backward tilt
            const roll = Math.atan2(gravityX, gravityZ) * (180 / Math.PI); // Left/right tilt
            
            console.log(`Record ${i + 1}:`);
            console.log(`  Raw accelerometer: X=${accelX.toFixed(2)}, Y=${accelY.toFixed(2)}, Z=${accelZ.toFixed(2)}`);
            console.log(`  Magnitude: ${magnitude.toFixed(2)} m/s²`);
            console.log(`  Normalized gravity: X=${gravityX.toFixed(3)}, Y=${gravityY.toFixed(3)}, Z=${gravityZ.toFixed(3)}`);
            console.log(`  Pitch (forward/back): ${pitch.toFixed(1)}°`);
            console.log(`  Roll (left/right): ${roll.toFixed(1)}°`);
            console.log('');
        });
    }
    
    // Test gyroscope data for movement indication
    const gyroRecords = data.filter(r => r.gyro_x !== null || r.gyro_y !== null || r.gyro_z !== null);
    console.log('🔄 Gyroscope Movement Examples:');
    console.log('===============================');
    console.log(`Records with gyroscope data: ${gyroRecords.length}`);
    console.log('');
    
    if (gyroRecords.length > 0) {
        gyroRecords.slice(0, 5).forEach((record, i) => {
            const gyroX = record.gyro_x || 0;
            const gyroY = record.gyro_y || 0;
            const gyroZ = record.gyro_z || 0;
            
            const magnitude = Math.sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ);
            
            console.log(`Record ${i + 1}:`);
            console.log(`  Gyroscope: X=${gyroX.toFixed(6)}, Y=${gyroY.toFixed(6)}, Z=${gyroZ.toFixed(6)}`);
            console.log(`  Movement magnitude: ${magnitude.toFixed(6)} rad/s`);
            console.log(`  Activity level: ${(magnitude * 10).toFixed(3)}`);
            console.log('');
        });
    }
    
    console.log('✅ Simple orientation test complete!');
    console.log('');
    console.log('📋 Expected behavior:');
    console.log('- Phone orientation based on gravity (accelerometer)');
    console.log('- Screen glow based on movement (gyroscope)');
    console.log('- No continuous rotation or flipping');
    console.log('- Stable orientation when phone is stationary');
    
} catch (error) {
    console.error('❌ Error:', error.message);
} 