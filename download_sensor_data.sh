#!/bin/bash

# Download Sensor Data for Data Visualizer
# This script downloads sensor data from your server and formats it for the data visualizer

set -e

# Configuration
SERVER_URL="https://amazon.government.rip"
OUTPUT_DIR="data-visualizer"
DEVICE_ID="android-unknown"  # Your device ID from the logs

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}📱 Downloading Sensor Data for Data Visualizer${NC}"
echo "=================================================="

# Check if server is reachable
echo -e "${YELLOW}🔍 Checking server connectivity...${NC}"
if ! curl -s "$SERVER_URL/health" > /dev/null; then
    echo -e "${RED}❌ Server is not reachable at $SERVER_URL${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Server is reachable${NC}"

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Download sensor data
echo -e "${YELLOW}📥 Downloading sensor data...${NC}"
SENSOR_DATA_FILE="$OUTPUT_DIR/sensor_data.json"

# Try to download data for specific device first
if curl -s "$SERVER_URL/data/sensors?device=$DEVICE_ID&limit=1000" > "$SENSOR_DATA_FILE"; then
    echo -e "${GREEN}✅ Downloaded sensor data to $SENSOR_DATA_FILE${NC}"
else
    echo -e "${YELLOW}⚠️  Could not download device-specific data, trying all data...${NC}"
    curl -s "$SERVER_URL/data/sensors?limit=1000" > "$SENSOR_DATA_FILE"
    echo -e "${GREEN}✅ Downloaded all sensor data to $SENSOR_DATA_FILE${NC}"
fi

# Check if we got data
if [ ! -s "$SENSOR_DATA_FILE" ]; then
    echo -e "${RED}❌ No sensor data found${NC}"
    exit 1
fi

# Get data summary
echo -e "${YELLOW}📊 Getting data summary...${NC}"
SUMMARY=$(curl -s "$SERVER_URL/summary")
echo -e "${GREEN}📈 Data Summary:${NC}"
echo "$SUMMARY" | jq '.'

# Convert to visualizer format
echo -e "${YELLOW}🔄 Converting to visualizer format...${NC}"
VISUALIZER_DATA_FILE="$OUTPUT_DIR/visualizer_data.json"

# Use Node.js to convert the data format
node -e "
const fs = require('fs');

try {
    const sensorData = JSON.parse(fs.readFileSync('$SENSOR_DATA_FILE', 'utf8'));
    
    if (!sensorData.data || !Array.isArray(sensorData.data)) {
        console.log('No data array found in sensor data');
        process.exit(1);
    }
    
    // Convert to visualizer format
    const visualizerData = sensorData.data.map(record => ({
        timestamp: record.timestamp,
        gyro_x: record.gyro_x || null,
        gyro_y: record.gyro_y || null,
        gyro_z: record.gyro_z || null,
        accel_x: record.accel_x || null,
        accel_y: record.accel_y || null,
        accel_z: record.accel_z || null,
        latitude: record.latitude || null,
        longitude: record.longitude || null,
        accuracy: record.accuracy || null,
        pressure: record.pressure || null,
        altitude: record.altitude || null,
        device_id: record.device_id || '$DEVICE_ID'
    }));
    
    // Write visualizer data
    fs.writeFileSync('$VISUALIZER_DATA_FILE', JSON.stringify(visualizerData, null, 2));
    
    console.log('✅ Converted', visualizerData.length, 'records to visualizer format');
    console.log('📁 Saved to:', '$VISUALIZER_DATA_FILE');
    
    // Show sample data
    if (visualizerData.length > 0) {
        console.log('📋 Sample record:');
        console.log(JSON.stringify(visualizerData[0], null, 2));
    }
    
} catch (error) {
    console.error('❌ Error converting data:', error.message);
    process.exit(1);
}
"

# Create a simple HTML file to open the visualizer with the data
HTML_FILE="$OUTPUT_DIR/open_visualizer.html"
cat > "$HTML_FILE" << EOF
<!DOCTYPE html>
<html>
<head>
    <title>Open Data Visualizer</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .button { 
            background: #007bff; color: white; padding: 15px 30px; 
            text-decoration: none; border-radius: 5px; display: inline-block;
            margin: 10px 5px;
        }
        .info { background: #f8f9fa; padding: 20px; border-radius: 5px; margin: 20px 0; }
    </style>
</head>
<body>
    <h1>📱 Phone Sensor Data Visualizer</h1>
    
    <div class="info">
        <h3>📊 Data Summary</h3>
        <pre id="summary">Loading...</pre>
    </div>
    
    <h3>🚀 Quick Actions</h3>
    <a href="http://localhost:3000" class="button" target="_blank">
        🌐 Open Data Visualizer
    </a>
    <a href="visualizer_data.json" class="button" target="_blank">
        📄 View Raw Data
    </a>
    
    <h3>📋 Instructions</h3>
    <ol>
        <li>Start the data visualizer: <code>cd data-visualizer && npm start</code></li>
        <li>Open <a href="http://localhost:3000" target="_blank">http://localhost:3000</a></li>
        <li>Load the data file: <code>visualizer_data.json</code></li>
        <li>Explore your phone's sensor data in 3D!</li>
    </ol>
    
    <script>
        // Load and display summary
        fetch('$SERVER_URL/summary')
            .then(response => response.json())
            .then(data => {
                document.getElementById('summary').textContent = JSON.stringify(data, null, 2);
            })
            .catch(error => {
                document.getElementById('summary').textContent = 'Error loading summary: ' + error.message;
            });
    </script>
</body>
</html>
EOF

echo -e "${GREEN}✅ Created HTML helper file: $HTML_FILE${NC}"

# Show final summary
echo -e "${BLUE}🎉 Download Complete!${NC}"
echo "=================================================="
echo -e "${GREEN}📁 Files created:${NC}"
echo "  • $SENSOR_DATA_FILE (raw server data)"
echo "  • $VISUALIZER_DATA_FILE (visualizer format)"
echo "  • $HTML_FILE (helper page)"
echo ""
echo -e "${YELLOW}🚀 Next steps:${NC}"
echo "  1. cd data-visualizer"
echo "  2. npm start"
echo "  3. Open http://localhost:3000"
echo "  4. Load visualizer_data.json"
echo ""
echo -e "${BLUE}📊 Data Summary:${NC}"
echo "$SUMMARY" | jq -r '.total_sensors, .unique_devices, .first_record, .last_record' | while read line; do
    echo "  $line"
done 