#!/bin/bash

echo "🔧 Building Android Sensor Logger App..."
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ ADB not found. Installing Android tools..."
    sudo apt update && sudo apt install -y android-tools-adb
fi

# Build the app
echo "📦 Building APK..."
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    
    # Check for connected devices
    echo "📱 Checking for connected devices..."
    adb devices
    
    # Install on device
    echo "📲 Installing on device..."
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    
    if [ $? -eq 0 ]; then
        echo "✅ App installed successfully!"
        echo "🚀 Launching app..."
        adb shell am start -n com.yourdomain.sensorlogger/.MainActivity
    else
        echo "❌ Installation failed. Make sure your phone is connected and USB debugging is enabled."
    fi
else
    echo "❌ Build failed!"
fi 