#!/usr/bin/env node

// Test script to verify gyroscope bias correction
const fs = require('fs');

try {
    const data = JSON.parse(fs.readFileSync('data-visualizer/visualizer_data.json', 'utf8'));
    
    // Calculate bias
    const gyroX = data.map(r => r.gyro_x).filter(x => x !== null);
    const gyroY = data.map(r => r.gyro_y).filter(x => x !== null);
    const gyroZ = data.map(r => r.gyro_z).filter(x => x !== null);
    
    const biasX = gyroX.reduce((a, b) => a + b, 0) / gyroX.length;
    const biasY = gyroY.reduce((a, b) => a + b, 0) / gyroY.length;
    const biasZ = gyroZ.reduce((a, b) => a + b, 0) / gyroZ.length;
    
    console.log('📊 Gyroscope Bias Analysis');
    console.log('==========================');
    console.log(`Original bias X: ${biasX.toFixed(6)}`);
    console.log(`Original bias Y: ${biasY.toFixed(6)}`);
    console.log(`Original bias Z: ${biasZ.toFixed(6)}`);
    console.log('');
    
    // Show corrected values
    console.log('🔧 Bias-Corrected Sample Values:');
    console.log('================================');
    data.slice(0, 5).forEach((record, i) => {
        const correctedX = (record.gyro_x || 0) - biasX;
        const correctedY = (record.gyro_y || 0) - biasY;
        const correctedZ = (record.gyro_z || 0) - biasZ;
        
        console.log(`Record ${i + 1}:`);
        console.log(`  Original:  X=${(record.gyro_x || 0).toFixed(6)}, Y=${(record.gyro_y || 0).toFixed(6)}, Z=${(record.gyro_z || 0).toFixed(6)}`);
        console.log(`  Corrected: X=${correctedX.toFixed(6)}, Y=${correctedY.toFixed(6)}, Z=${correctedZ.toFixed(6)}`);
        console.log('');
    });
    
    // Calculate corrected statistics
    const correctedX = gyroX.map(x => x - biasX);
    const correctedY = gyroY.map(y => y - biasY);
    const correctedZ = gyroZ.map(z => z - biasZ);
    
    const correctedBiasX = correctedX.reduce((a, b) => a + b, 0) / correctedX.length;
    const correctedBiasY = correctedY.reduce((a, b) => a + b, 0) / correctedY.length;
    const correctedBiasZ = correctedZ.reduce((a, b) => a + b, 0) / correctedZ.length;
    
    console.log('✅ Correction Results:');
    console.log('======================');
    console.log(`Corrected bias X: ${correctedBiasX.toFixed(6)} (should be ~0)`);
    console.log(`Corrected bias Y: ${correctedBiasY.toFixed(6)} (should be ~0)`);
    console.log(`Corrected bias Z: ${correctedBiasZ.toFixed(6)} (should be ~0)`);
    
} catch (error) {
    console.error('❌ Error:', error.message);
} 