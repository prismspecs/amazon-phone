# Phone Sensor Data Visualizer

A 3D visualization tool for phone sensor data using Three.js. Visualize phone rotation, movement, GPS coordinates, and sensor readings in real-time playback.

## Features

- **3D Phone Model**: Flattened cube representation of the phone
- **Real-time Rotation**: Visualize gyroscope data as phone rotation
- **Movement Tracking**: Accelerometer data affects phone position
- **GPS Visualization**: Mini-map with location tracking and path
- **Sensor Data Display**: Real-time values for all sensors
- **Playback Controls**: Play, pause, reset, and export data
- **Dark Theme**: Optimized for low-light environments

## Quick Start

### Option 1: Simple HTTP Server (No Dependencies)

```bash
cd data-visualizer
python3 -m http.server 3000
# or
python -m http.server 3000
```

Then open: http://localhost:3000

### Option 2: Node.js Server (Recommended)

```bash
cd data-visualizer
npm install
npm start
```

Then open: http://localhost:3000

## Usage

1. **Load Data**: Click "📁 Load Data File" and select your sensor data JSON file
2. **Play**: Click "▶️ Play" to start visualization playback
3. **Pause**: Click "⏸️ Pause" to stop playback
4. **Reset**: Click "🔄 Reset" to return to the beginning
5. **Export**: Click "💾 Export" to download the current dataset

## Data Format

The visualizer expects JSON data in one of these formats:

### Format 1: Unified Sensor Records (Recommended)
```json
{
  "deviceId": "android-123456",
  "data": [
    {
      "timestamp": 1640995200000,
      "gyro_x": 0.1,
      "gyro_y": 0.2,
      "gyro_z": 0.3,
      "accel_x": 9.8,
      "accel_y": 0.1,
      "accel_z": 0.2,
      "latitude": 40.7128,
      "longitude": -74.0060,
      "accuracy": 5.0,
      "pressure": 1006.65967,
      "altitude": 55.012142
    }
  ]
}
```

### Format 2: Simple Array
```json
[
  {
    "timestamp": 1640995200000,
    "gyro_x": 0.1,
    "gyro_y": 0.2,
    "gyro_z": 0.3,
    "accel_x": 9.8,
    "accel_y": 0.1,
    "accel_z": 0.2,
    "latitude": 40.7128,
    "longitude": -74.0060,
    "accuracy": 5.0,
    "pressure": 1006.65967,
    "altitude": 55.012142
  }
]
```

## Visualization Features

### 3D Phone Model
- **Body**: Dark gray flattened cube (2x0.1x4 units)
- **Screen**: Black plane with emissive glow
- **Rotation Indicator**: Red sphere showing rotation direction
- **Coordinate Axes**: Reference axes (X=red, Y=green, Z=blue)

### Sensor Visualization
- **Gyroscope**: Controls phone rotation (X, Y, Z axes)
- **Accelerometer**: Affects phone position (gravity-removed)
- **Screen Glow**: Intensity based on sensor activity
- **Real-time Values**: Displayed in UI panels

### GPS Features
- **Mini-map**: Dark-themed Google Maps integration
- **Location Marker**: Green circle with black center
- **Path Tracking**: Green line showing GPS route
- **Auto-centering**: Map centers on first GPS coordinate

### Controls
- **Mouse**: Rotate camera view
- **Scroll**: Zoom in/out
- **Right-click**: Pan camera
- **Touch**: Mobile-friendly touch controls

## Technical Details

### Three.js Setup
- **Renderer**: WebGL with antialiasing
- **Shadows**: PCF soft shadow mapping
- **Lighting**: Ambient + directional + point lights
- **Controls**: OrbitControls for camera manipulation

### Performance Optimizations
- **RequestAnimationFrame**: Smooth 60fps rendering
- **Efficient Updates**: Only update when data changes
- **Memory Management**: Proper cleanup of Three.js objects
- **Responsive Design**: Adapts to window resizing

### Browser Compatibility
- **Modern Browsers**: Chrome, Firefox, Safari, Edge
- **WebGL Support**: Required for 3D rendering
- **ES6+ Features**: Uses modern JavaScript
- **Mobile Support**: Touch-friendly interface

## Customization

### Google Maps API
Replace `YOUR_GOOGLE_MAPS_API_KEY` in `index.html` with your API key:
```html
<script src="https://maps.googleapis.com/maps/api/js?key=YOUR_API_KEY&libraries=geometry"></script>
```

### Visual Customization
- **Phone Model**: Modify `createPhoneModel()` in `visualizer.js`
- **Colors**: Update CSS variables in `index.html`
- **Lighting**: Adjust `setupLighting()` function
- **Playback Speed**: Change `playbackSpeed` variable

### Data Processing
- **Rotation Sensitivity**: Modify rotation multipliers in `updatePhoneModel()`
- **Position Sensitivity**: Adjust acceleration multipliers
- **Activity Threshold**: Change screen glow calculation

## Troubleshooting

### Common Issues

1. **"Invalid file format" error**
   - Ensure your JSON file matches the expected format
   - Check for syntax errors in JSON

2. **Map not loading**
   - Verify Google Maps API key is set
   - Check browser console for API errors

3. **3D rendering issues**
   - Ensure WebGL is supported in your browser
   - Try updating graphics drivers

4. **Performance problems**
   - Reduce dataset size for large files
   - Close other browser tabs
   - Check system resources

### Debug Mode
Open browser console (F12) to see:
- Data loading progress
- Sensor value updates
- Error messages
- Performance metrics

## Future Enhancements

- [ ] Audio playback integration
- [ ] Photo display overlay
- [ ] Multiple device support
- [ ] Advanced filtering options
- [ ] Export to video format
- [ ] Real-time data streaming
- [ ] Custom phone models
- [ ] Sensor calibration tools

## License

MIT License - See LICENSE file for details. 