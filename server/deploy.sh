#!/bin/bash

# Sensor Data Server Deployment Script
# Creates tar.gz file for CapRover deployment with persistent storage

echo "🚀 Creating sensor server deployment package..."

# Create tar.gz with all necessary files
# Note: captain-definition must be in the root of the tar.gz
tar -czf sensor-server.tar.gz \
    captain-definition \
    Dockerfile \
    package.json \
    server.js

echo "✅ Created sensor-server.tar.gz"
echo ""
echo "📦 Deployment Instructions:"
echo "1. Go to your CapRover dashboard"
echo "2. Click 'One-Click Apps' → 'Upload'"
echo "3. Upload sensor-server.tar.gz"
echo "4. Set app name: amazon-server"
echo "5. Deploy"
echo ""
echo "🔗 After deployment, your server will be available at:"
echo "   https://amazon.government.rip"
echo ""
echo "📊 Test your deployment:"
echo "   curl https://amazon.government.rip/health"
echo ""
echo "💾 Persistent storage is configured:"
echo "   - Database: /app/data/sensor_data.db"
echo "   - Uploads: /app/data/uploads"
echo "   - Volume mapping: /captain/data/sensor-server → /app/data"
echo ""
echo "📈 Data endpoints:"
echo "   - Summary: https://amazon.government.rip/summary"
echo "   - Sensors: https://amazon.government.rip/data/sensors"
echo "   - Locations: https://amazon.government.rip/data/locations"
echo "   - Barometers: https://amazon.government.rip/data/barometers" 