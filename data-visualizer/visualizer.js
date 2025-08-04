class PhoneDataVisualizer {
    constructor() {
        this.scene = null;
        this.camera = null;
        this.renderer = null;
        this.controls = null;
        this.phoneModel = null;
        this.map = null;
        this.mapMarker = null;
        
        // Animation state
        this.isPlaying = false;
        this.currentIndex = 0;
        this.sensorData = [];
        this.currentData = null;
        this.targetData = null;
        this.interpolationProgress = 0;
        this.interpolationDuration = 100; // ms between data points
        this.lastTimestamp = 0;
        
        // Gyroscope bias correction
        this.gyroBias = { x: 0, y: 0, z: 0 };
        this.biasCalculated = false;
        
        // Rotation tracking
        this.currentRotation = { x: 0, y: 0, z: 0 };
        this.lastGyroData = { x: 0, y: 0, z: 0 };
        this.lastUpdateTime = 0;
        
        // UI elements
        this.playBtn = document.getElementById('play-btn');
        this.pauseBtn = document.getElementById('pause-btn');
        this.resetBtn = document.getElementById('reset-btn');
        this.exportBtn = document.getElementById('export-btn');
        this.fileInput = document.getElementById('file-input');
        this.loading = document.getElementById('loading');
        
        this.init();
    }

    init() {
        this.setupThreeJS();
        this.setupMap();
        this.setupEventListeners();
        this.animate();
        this.hideLoading();
    }

    setupThreeJS() {
        // Scene
        this.scene = new THREE.Scene();
        this.scene.background = new THREE.Color(0x0a0a0a);

        // Camera
        this.camera = new THREE.PerspectiveCamera(
            75, 
            window.innerWidth / window.innerHeight, 
            0.1, 
            1000
        );
        this.camera.position.set(0, 0, 5);

        // Renderer
        this.renderer = new THREE.WebGLRenderer({ antialias: true });
        this.renderer.setSize(window.innerWidth, window.innerHeight);
        this.renderer.shadowMap.enabled = true;
        this.renderer.shadowMap.type = THREE.PCFSoftShadowMap;
        document.getElementById('threejs-container').appendChild(this.renderer.domElement);

        // Controls
        this.controls = new THREE.OrbitControls(this.camera, this.renderer.domElement);
        this.controls.enableDamping = true;
        this.controls.dampingFactor = 0.05;

        // Lighting
        this.setupLighting();

        // Create phone model
        this.createPhoneModel();

        // Add coordinate axes for reference
        this.addCoordinateAxes();
    }

    setupLighting() {
        // Ambient light
        const ambientLight = new THREE.AmbientLight(0x404040, 0.6);
        this.scene.add(ambientLight);

        // Directional light
        const directionalLight = new THREE.DirectionalLight(0xffffff, 0.8);
        directionalLight.position.set(5, 5, 5);
        directionalLight.castShadow = true;
        directionalLight.shadow.mapSize.width = 2048;
        directionalLight.shadow.mapSize.height = 2048;
        this.scene.add(directionalLight);

        // Point light for phone highlighting
        const pointLight = new THREE.PointLight(0x00ff88, 0.5, 10);
        pointLight.position.set(0, 2, 2);
        this.scene.add(pointLight);
    }

    createPhoneModel() {
        // Cardboard box (thicker cube)
        const boxGeometry = new THREE.BoxGeometry(1, 0.6, 2); 
        const boxMaterial = new THREE.MeshPhongMaterial({ 
            color: 0x8B4513, // Brown color for cardboard
            transparent: true,
            opacity: 0.9
        });
        this.phoneModel = new THREE.Mesh(boxGeometry, boxMaterial);
        this.phoneModel.castShadow = true;
        this.phoneModel.receiveShadow = true;

        // Add subtle emissive glow to simulate cardboard texture
        boxMaterial.emissive = new THREE.Color(0x222222);
        this.phoneModel.material = boxMaterial;

        // Add box to scene
        this.scene.add(this.phoneModel);

        // Create rotation indicator
        this.createRotationIndicator();
    }

    createRotationIndicator() {
        // Create a visual indicator for rotation
        const indicatorGeometry = new THREE.SphereGeometry(0.05, 8, 8);
        const indicatorMaterial = new THREE.MeshPhongMaterial({ color: 0xff0000 });
        this.rotationIndicator = new THREE.Mesh(indicatorGeometry, indicatorMaterial);
        this.rotationIndicator.position.set(0, 0.2, 0);
        this.phoneModel.add(this.rotationIndicator);
    }

    addCoordinateAxes() {
        const axesHelper = new THREE.AxesHelper(2);
        this.scene.add(axesHelper);
    }

    setupMap() {
        // Initialize map with a default location (can be updated with real data)
        const mapOptions = {
            center: { lat: 40.7128, lng: -74.0060 },
            zoom: 12,
            mapTypeId: google.maps.MapTypeId.ROADMAP,
            styles: [
                { featureType: "all", elementType: "labels.text.fill", stylers: [{ color: "#ffffff" }] },
                { featureType: "all", elementType: "labels.text.stroke", stylers: [{ color: "#000000" }] },
                { featureType: "all", elementType: "geometry.fill", stylers: [{ color: "#2c2c2c" }] },
                { featureType: "road", elementType: "geometry", stylers: [{ color: "#38414e" }] },
                { featureType: "road", elementType: "geometry.stroke", stylers: [{ color: "#212a37" }] },
                { featureType: "road", elementType: "labels.text.fill", stylers: [{ color: "#9ca5b3" }] },
                { featureType: "water", elementType: "geometry", stylers: [{ color: "#17263c" }] },
                { featureType: "water", elementType: "labels.text.fill", stylers: [{ color: "#515c6d" }] },
                { featureType: "water", elementType: "labels.text.stroke", stylers: [{ color: "#17263c" }] }
            ]
        };

        this.map = new google.maps.Map(document.getElementById('map'), mapOptions);

        // Create marker for phone location
        this.mapMarker = new google.maps.Marker({
            position: mapOptions.center,
            map: this.map,
            title: 'Phone Location',
            icon: {
                url: 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(`
                    <svg width="20" height="20" xmlns="http://www.w3.org/2000/svg">
                        <circle cx="10" cy="10" r="8" fill="#00ff88" stroke="#000" stroke-width="2"/>
                        <circle cx="10" cy="10" r="3" fill="#000"/>
                    </svg>
                `),
                scaledSize: new google.maps.Size(20, 20)
            }
        });
    }

    setupEventListeners() {
        // File input
        this.fileInput.addEventListener('change', (e) => this.handleFileLoad(e));

        // Playback controls
        this.playBtn.addEventListener('click', () => this.play());
        this.pauseBtn.addEventListener('click', () => this.pause());
        this.resetBtn.addEventListener('click', () => this.reset());
        this.exportBtn.addEventListener('click', () => this.exportData());

        // Window resize
        window.addEventListener('resize', () => this.onWindowResize());
    }

    handleFileLoad(event) {
        const file = event.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = (e) => {
            try {
                const data = JSON.parse(e.target.result);
                this.loadSensorData(data);
            } catch (error) {
                console.error('Error parsing file:', error);
                alert('Invalid file format. Please load a valid JSON file.');
            }
        };
        reader.readAsText(file);
    }

    loadSensorData(data) {
        // Handle different data formats
        if (data.data && Array.isArray(data.data)) {
            this.sensorData = data.data;
        } else if (Array.isArray(data)) {
            this.sensorData = data;
        } else {
            console.error('Invalid data format');
            return;
        }

        console.log(`Loaded ${this.sensorData.length} sensor records`);
        
        // Calculate gyroscope bias
        this.calculateGyroBias();
        
        // Enable controls
        this.playBtn.disabled = false;
        this.resetBtn.disabled = false;
        this.exportBtn.disabled = false;

        // Update map with GPS data
        this.updateMapWithGPSData();

        // Start at beginning
        this.reset();
    }

    calculateGyroBias() {
        if (this.biasCalculated || this.sensorData.length === 0) return;
        
        const gyroX = this.sensorData.map(r => r.gyro_x).filter(x => x !== null);
        const gyroY = this.sensorData.map(r => r.gyro_y).filter(x => x !== null);
        const gyroZ = this.sensorData.map(r => r.gyro_z).filter(x => x !== null);
        
        if (gyroX.length > 0) {
            this.gyroBias.x = gyroX.reduce((a, b) => a + b, 0) / gyroX.length;
            this.gyroBias.y = gyroY.reduce((a, b) => a + b, 0) / gyroY.length;
            this.gyroBias.z = gyroZ.reduce((a, b) => a + b, 0) / gyroZ.length;
            
            console.log('Gyroscope bias calculated:', this.gyroBias);
        }
        
        this.biasCalculated = true;
    }

    updateMapWithGPSData() {
        // Find valid GPS coordinates
        const gpsData = this.sensorData.filter(record => 
            record.latitude && record.longitude && 
            record.latitude !== 0 && record.longitude !== 0
        );

        if (gpsData.length > 0) {
            const center = {
                lat: gpsData[0].latitude,
                lng: gpsData[0].longitude
            };
            
            this.map.setCenter(center);
            this.map.setZoom(15);

            // Create path for GPS track
            const path = new google.maps.Polyline({
                path: gpsData.map(point => ({
                    lat: point.latitude,
                    lng: point.longitude
                })),
                geodesic: true,
                strokeColor: '#00ff88',
                strokeOpacity: 1.0,
                strokeWeight: 3
            });

            path.setMap(this.map);
        }
    }

    play() {
        if (this.sensorData.length === 0) return;
        
        this.isPlaying = true;
        this.playBtn.disabled = true;
        this.pauseBtn.disabled = false;
        this.lastTimestamp = performance.now();
    }

    pause() {
        this.isPlaying = false;
        this.playBtn.disabled = false;
        this.pauseBtn.disabled = true;
    }

    reset() {
        this.currentIndex = 0;
        this.pause();
        
        // Reset phone position and rotation
        if (this.phoneModel) {
            this.phoneModel.position.set(0, 0, 0);
            this.phoneModel.rotation.set(0, 0, 0);
        }
        
        // Reset interpolation state
        this.currentData = null;
        this.targetData = null;
        this.interpolationProgress = 0;
        this.currentRotation = { x: 0, y: 0, z: 0 };
        this.lastGyroData = { x: 0, y: 0, z: 0 };
        this.lastUpdateTime = 0;
        
        // Recalculate bias if data is loaded
        if (this.sensorData.length > 0) {
            this.calculateGyroBias();
        }
        
        this.updatePhoneModel(this.sensorData[0] || {});
        this.updateUI(this.sensorData[0] || {});
    }

    exportData() {
        if (this.sensorData.length === 0) return;

        const dataStr = JSON.stringify(this.sensorData, null, 2);
        const dataBlob = new Blob([dataStr], { type: 'application/json' });
        
        const link = document.createElement('a');
        link.href = URL.createObjectURL(dataBlob);
        link.download = 'sensor_data_export.json';
        link.click();
    }

    updatePhoneModel(data) {
        if (!this.phoneModel || !data) return;

        // Simple approach: Use accelerometer for orientation, gyroscope for movement indication
        
        // Use accelerometer to determine phone orientation (gravity direction)
        if (data.accel_x !== undefined || data.accel_y !== undefined || data.accel_z !== undefined) {
            const accelX = data.accel_x || 0;
            const accelY = data.accel_y || 0;
            const accelZ = data.accel_z || 0;
            
            // Calculate total acceleration magnitude
            const magnitude = Math.sqrt(accelX * accelX + accelY * accelY + accelZ * accelZ);
            
            if (magnitude > 5) { // Only use when magnitude is reasonable
                // Normalize to get gravity direction
                const gravityX = accelX / magnitude;
                const gravityY = accelY / magnitude;
                const gravityZ = accelZ / magnitude;
                
                // Convert gravity direction to Euler angles
                // This gives us the phone's orientation relative to gravity
                const pitch = Math.asin(-gravityY); // Forward/backward tilt
                const roll = Math.atan2(gravityX, gravityZ); // Left/right tilt
                
                // Apply orientation directly (no integration)
                this.phoneModel.rotation.x = pitch;
                this.phoneModel.rotation.z = roll;
            }
        }
        
        // Use gyroscope to show movement activity (not for rotation)
        if (data.gyro_x !== undefined || data.gyro_y !== undefined || data.gyro_z !== undefined) {
            const gyroX = (data.gyro_x || 0) - this.gyroBias.x;
            const gyroY = (data.gyro_y || 0) - this.gyroBias.y;
            const gyroZ = (data.gyro_z || 0) - this.gyroBias.z;
            
            // Calculate gyroscope magnitude for activity indication
            const gyroMagnitude = Math.sqrt(gyroX * gyroX + gyroY * gyroY + gyroZ * gyroZ);
            
            // Use gyroscope data to affect screen glow (movement indicator)
            const activity = gyroMagnitude * 10; // Scale for visibility
            const intensity = Math.min(activity, 1);
            this.phoneModel.material.emissive.setHex(0x111111 + Math.floor(intensity * 0x444444));
        }
    }

    updateUI(data) {
        if (!data) return;

        // Update sensor values (with bias correction for gyro)
        const gyroX = (data.gyro_x || 0) - this.gyroBias.x;
        const gyroY = (data.gyro_y || 0) - this.gyroBias.y;
        const gyroZ = (data.gyro_z || 0) - this.gyroBias.z;
        
        document.getElementById('gyro-x').textContent = gyroX.toFixed(6);
        document.getElementById('gyro-y').textContent = gyroY.toFixed(6);
        document.getElementById('gyro-z').textContent = gyroZ.toFixed(6);
        document.getElementById('accel-x').textContent = (data.accel_x || 0).toFixed(3);
        document.getElementById('accel-y').textContent = (data.accel_y || 0).toFixed(3);
        document.getElementById('accel-z').textContent = (data.accel_z || 0).toFixed(3);
        document.getElementById('pressure').textContent = (data.pressure || 0).toFixed(1);
        document.getElementById('altitude').textContent = (data.altitude || 0).toFixed(1);

        // Update GPS values
        document.getElementById('latitude').textContent = (data.latitude || 0).toFixed(6);
        document.getElementById('longitude').textContent = (data.longitude || 0).toFixed(6);
        document.getElementById('accuracy').textContent = (data.accuracy || 0).toFixed(1);

        // Update map marker if GPS data is available
        if (data.latitude && data.longitude && data.latitude !== 0 && data.longitude !== 0) {
            const position = { lat: data.latitude, lng: data.longitude };
            this.mapMarker.setPosition(position);
        }
    }

    animate() {
        requestAnimationFrame(() => this.animate());

        if (this.isPlaying && this.sensorData.length > 0) {
            const currentTime = performance.now();
            const deltaTime = currentTime - this.lastTimestamp;
            
            // Advance through data based on time
            if (deltaTime > this.interpolationDuration) {
                this.currentIndex = (this.currentIndex + 1) % this.sensorData.length;
                this.lastTimestamp = currentTime;
                
                // Set up interpolation
                this.currentData = this.targetData || this.sensorData[this.currentIndex];
                this.targetData = this.sensorData[this.currentIndex];
                this.interpolationProgress = 0;
            }
            
            // Interpolate between data points
            if (this.currentData && this.targetData) {
                this.interpolationProgress += deltaTime / this.interpolationDuration;
                this.interpolationProgress = Math.min(this.interpolationProgress, 1);
                
                const interpolatedData = this.interpolateData(this.currentData, this.targetData, this.interpolationProgress);
                this.updatePhoneModel(interpolatedData);
                this.updateUI(interpolatedData);
            }
        }

        this.controls.update();
        this.renderer.render(this.scene, this.camera);
    }

    interpolateData(startData, endData, progress) {
        const interpolated = {};
        
        // Interpolate all numeric values
        Object.keys(startData).forEach(key => {
            if (typeof startData[key] === 'number' && typeof endData[key] === 'number') {
                interpolated[key] = startData[key] + (endData[key] - startData[key]) * progress;
            } else {
                interpolated[key] = endData[key]; // Use target value for non-numeric
            }
        });
        
        return interpolated;
    }

    onWindowResize() {
        this.camera.aspect = window.innerWidth / window.innerHeight;
        this.camera.updateProjectionMatrix();
        this.renderer.setSize(window.innerWidth, window.innerHeight);
    }

    hideLoading() {
        this.loading.style.display = 'none';
    }
}

// Initialize the visualizer when the page loads
document.addEventListener('DOMContentLoaded', () => {
    new PhoneDataVisualizer();
}); 