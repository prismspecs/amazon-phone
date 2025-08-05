#!/bin/bash

# Clear Data Script for Amazon Phone Sensor Logger
# This script deletes all data from the database

echo "🗑️  Clearing all sensor data from database..."

# Call the server's clear endpoint with confirmation
curl -X DELETE "https://amazon.government.rip/data/all?confirm=true" \
  -H "Content-Type: application/json" \
  -H "User-Agent: SensorLogger-ClearScript"

echo ""
echo "✅ Database cleared successfully!"
echo "📊 You can verify by checking: curl https://amazon.government.rip/summary" 