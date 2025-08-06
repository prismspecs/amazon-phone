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
        
        // Real-time playback using timestamps
        this.playbackStartTime = 0;  // When playback started (performance.now())
        this.dataStartTime = 0;      // Timestamp of first data point
        this.dataEndTime = 0;        // Timestamp of last data point
        this.playbackSpeed = 1.0;    // Speed multiplier (1.0 = real-time)
        
        // Smooth animation settings
        this.targetFrameRate = 60;   // Target 60fps for smooth animation
        this.frameInterval = 1000 / this.targetFrameRate; // ~16.67ms per frame
        this.lastFrameTime = 0;
        
        // Height tracking
        this.baseAltitude = 0;       // Starting altitude (will be set from first reading)
        this.currentHeight = 0;      // Current height relative to starting position
        this.heightScale = 10.0;     // Scale factor for height visualization (increased from 0.1 to 10.0)
        
        // Gyroscope bias correction
        this.gyroBias = { x: 0, y: 0, z: 0 };
        this.biasCalculated = false;
        
        // UI elements
        this.playBtn = document.getElementById('play-btn');
        this.pauseBtn = document.getElementById('pause-btn');
        this.resetBtn = document.getElementById('reset-btn');
        this.exportBtn = document.getElementById('export-btn');
        this.fileInput = document.getElementById('file-input');
        this.loading = document.getElementById('loading');
        
        // Audio elements
        this.audioInput = document.getElementById('audio-input');
        this.audioPlayBtn = document.getElementById('audio-play-btn');
        this.audioStopBtn = document.getElementById('audio-stop-btn');
        this.audioStatus = document.getElementById('audio-status');
        this.audioElement = null;
        this.audioStartTime = 0;  // When audio should start relative to data
        
        // Timeline elements
        this.timelineContainer = document.getElementById('timeline-container');
        this.timelineTrack = document.getElementById('timeline-track');
        this.timelineProgress = document.getElementById('timeline-progress');
        this.timelineHandle = document.getElementById('timeline-handle');
        this.currentTimeDisplay = document.getElementById('current-time');
        this.totalTimeDisplay = document.getElementById('total-time');
        this.timestampDisplay = document.getElementById('timestamp-display');
        
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
        
        // Add ground plane and height indicator
        this.addGroundPlane();
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
        // Phone body (brown box)
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

    addGroundPlane() {
        // Create a ground plane to show the starting height
        const groundGeometry = new THREE.PlaneGeometry(10, 10);
        const groundMaterial = new THREE.MeshBasicMaterial({ 
            color: 0x333333, 
            transparent: true, 
            opacity: 0.3,
            side: THREE.DoubleSide
        });
        this.groundPlane = new THREE.Mesh(groundGeometry, groundMaterial);
        this.groundPlane.rotation.x = -Math.PI / 2; // Rotate to be horizontal
        this.groundPlane.position.y = 0; // At starting height
        this.scene.add(this.groundPlane);
        
        // Add height indicator line
        const lineGeometry = new THREE.BufferGeometry().setFromPoints([
            new THREE.Vector3(0, 0, 0),
            new THREE.Vector3(0, 0, 0)
        ]);
        const lineMaterial = new THREE.LineBasicMaterial({ color: 0x00ff88 });
        this.heightLine = new THREE.Line(lineGeometry, lineMaterial);
        this.scene.add(this.heightLine);
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
        this.fileInput.addEventListener('change', (e) => this.handleFileLoad(e));
        this.playBtn.addEventListener('click', () => this.play());
        this.pauseBtn.addEventListener('click', () => this.pause());
        this.resetBtn.addEventListener('click', () => this.reset());
        this.exportBtn.addEventListener('click', () => this.exportData());
        
        // Audio controls
        this.audioInput.addEventListener('change', (e) => this.handleAudioLoad(e));
        this.audioPlayBtn.addEventListener('click', () => this.playAudio());
        this.audioStopBtn.addEventListener('click', () => this.stopAudio());
        
        // Timeline event listeners
        this.timelineTrack.addEventListener('click', (e) => this.seekToPosition(e));
        this.timelineHandle.addEventListener('mousedown', (e) => this.startDragging(e));
        
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

    handleAudioLoad(event) {
        const file = event.target.files[0];
        if (!file) return;

        // Check file format
        const fileExtension = file.name.split('.').pop().toLowerCase();
        const supportedFormats = ['mp3', 'wav', 'm4a', 'aac', 'ogg', 'webm'];
        
        if (!supportedFormats.includes(fileExtension)) {
            this.audioStatus.textContent = `Unsupported format: ${fileExtension}. Supported: ${supportedFormats.join(', ')}`;
            console.warn(`Unsupported audio format: ${fileExtension}`);
            return;
        }

        // Create audio element
        this.audioElement = new Audio();
        this.audioElement.preload = 'auto';
        
        // Create object URL for the audio file
        const audioUrl = URL.createObjectURL(file);
        this.audioElement.src = audioUrl;
        
        // Set up audio event listeners
        this.audioElement.addEventListener('loadedmetadata', () => {
            this.audioStatus.textContent = `Audio loaded: ${file.name} (${this.formatTime(this.audioElement.duration)})`;
            this.audioPlayBtn.disabled = false;
            this.audioStopBtn.disabled = false;
            console.log(`Audio loaded: ${file.name}, duration: ${this.audioElement.duration}s`);
        });
        
        this.audioElement.addEventListener('error', (e) => {
            console.error('Audio loading error:', e);
            this.audioStatus.textContent = `Error loading audio file: ${fileExtension} format may not be supported`;
        });
        
        this.audioElement.addEventListener('ended', () => {
            this.audioStatus.textContent = 'Audio playback ended';
        });
    }

    playAudio() {
        if (!this.audioElement) return;
        
        if (this.isPlaying && this.sensorData.length > 0) {
            // Synchronize audio with sensor data playback
            const currentDataTime = this.dataStartTime + (performance.now() - this.playbackStartTime) * this.playbackSpeed;
            const audioOffset = (currentDataTime - this.dataStartTime) / 1000; // Convert to seconds
            
            this.audioElement.currentTime = Math.max(0, audioOffset);
            this.audioElement.play();
            this.audioStatus.textContent = 'Audio playing (synchronized)';
        } else {
            // Play audio from beginning
            this.audioElement.currentTime = 0;
            this.audioElement.play();
            this.audioStatus.textContent = 'Audio playing';
        }
    }

    stopAudio() {
        if (!this.audioElement) return;
        
        this.audioElement.pause();
        this.audioElement.currentTime = 0;
        this.audioStatus.textContent = 'Audio stopped';
    }

    loadSensorData(data) {
        this.sensorData = data;
        this.currentIndex = 0;
        
        if (this.sensorData.length > 0) {
            // Sort data by timestamp to ensure chronological order (ascending)
            this.sensorData.sort((a, b) => a.timestamp - b.timestamp);
            
            // Filter out any invalid timestamps
            this.sensorData = this.sensorData.filter(record => 
                record.timestamp && typeof record.timestamp === 'number' && record.timestamp > 0
            );
            
            if (this.sensorData.length === 0) {
                console.error('No valid timestamp data found');
                return;
            }
            
            // Calculate time range for playback
            this.dataStartTime = this.sensorData[0].timestamp;
            this.dataEndTime = this.sensorData[this.sensorData.length - 1].timestamp;
            
            // Set base altitude from first valid altitude reading
            const firstAltitudeReading = this.sensorData.find(record => 
                record.altitude !== null && record.altitude !== undefined
            );
            if (firstAltitudeReading) {
                this.baseAltitude = firstAltitudeReading.altitude;
                console.log(`Base altitude set to: ${this.baseAltitude} meters`);
            }
            
            console.log(`Loaded ${this.sensorData.length} data points`);
            console.log(`Time range: ${new Date(this.dataStartTime).toISOString()} to ${new Date(this.dataEndTime).toISOString()}`);
            console.log(`Duration: ${(this.dataEndTime - this.dataStartTime) / 1000} seconds`);
            
            // Log some sample timestamps to debug
            console.log('Sample timestamps:', this.sensorData.slice(0, 5).map(d => d.timestamp));
            
            this.calculateGyroBias();
            this.updatePhoneModel(this.sensorData[0]);
            this.updateUI(this.sensorData[0]);
            
            // Enable controls
            this.playBtn.disabled = false;
            this.resetBtn.disabled = false;
            this.exportBtn.disabled = false;
            
            // Show and update timeline
            this.timelineContainer.style.display = 'block';
            this.updateTimeline();
            
            // Update map with GPS data
            this.updateMapWithGPSData();
            
            // Start at beginning
            this.reset();
        }
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
        this.playbackStartTime = performance.now();
        
        // Start audio if available
        if (this.audioElement && this.audioElement.readyState >= 2) {
            const currentDataTime = this.dataStartTime + (performance.now() - this.playbackStartTime) * this.playbackSpeed;
            const audioOffset = (currentDataTime - this.dataStartTime) / 1000;
            this.audioElement.currentTime = Math.max(0, audioOffset);
            this.audioElement.play();
            this.audioStatus.textContent = 'Audio playing (synchronized)';
        }
    }

    pause() {
        this.isPlaying = false;
        this.playBtn.disabled = false;
        this.pauseBtn.disabled = true;
        
        // Pause audio if playing
        if (this.audioElement && !this.audioElement.paused) {
            this.audioElement.pause();
            this.audioStatus.textContent = 'Audio paused';
        }
    }

    reset() {
        this.currentIndex = 0;
        this.pause();
        
        // Reset audio if available
        if (this.audioElement) {
            this.audioElement.currentTime = 0;
            this.audioStatus.textContent = 'Audio reset';
        }
        
        // Reset phone position and rotation
        if (this.phoneModel) {
            this.phoneModel.position.set(0, 0, 0); // Reset height to 0
            this.phoneModel.rotation.set(0, 0, 0);
        }
        
        // Reset interpolation state
        this.currentData = null;
        this.targetData = null;
        this.interpolationProgress = 0;
        
        // Recalculate bias if data is loaded
        if (this.sensorData.length > 0) {
            this.calculateGyroBias();
        }
        
        this.updatePhoneModel(this.sensorData[0] || {});
        this.updateUI(this.sensorData[0] || {});
        this.updateTimeline();
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

        // Calculate height change from altitude
        let heightChange = 0;
        if (data.altitude !== null && data.altitude !== undefined) {
            heightChange = (data.altitude - this.baseAltitude) * this.heightScale;
            this.currentHeight = heightChange;
            
            // Debug logging for height changes
            if (Math.abs(heightChange) > 0.1) {
                console.log(`Height change: ${(data.altitude - this.baseAltitude).toFixed(3)}m -> ${heightChange.toFixed(2)} units`);
            }
        }
        
        // Position phone based on height (Y-axis is up/down in Three.js)
        this.phoneModel.position.y = heightChange;
        
        // Update height indicator line
        if (this.heightLine) {
            const points = this.heightLine.geometry.attributes.position;
            points.setY(0, 0); // Start at ground
            points.setY(1, heightChange); // End at phone height
            points.needsUpdate = true;
        }
        
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
            // const activity = gyroMagnitude * 10; // Scale for visibility
            // const intensity = Math.min(activity, 1);
            // this.phoneModel.material.emissive.setHex(0x111111 + Math.floor(intensity * 0x444444));
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
        
        // Update height display
        const heightChange = data.altitude ? (data.altitude - this.baseAltitude) : 0;
        document.getElementById('altitude').textContent = `${(data.altitude || 0).toFixed(1)} m (${heightChange.toFixed(2)} m change)`;
        document.getElementById('height-change').textContent = heightChange.toFixed(3);

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
            
            // Limit frame rate for smooth animation
            if (currentTime - this.lastFrameTime < this.frameInterval) {
                this.controls.update();
                this.renderer.render(this.scene, this.camera);
                return;
            }
            
            this.lastFrameTime = currentTime;
            
            const elapsedPlaybackTime = (currentTime - this.playbackStartTime) * this.playbackSpeed;
            const targetDataTime = this.dataStartTime + elapsedPlaybackTime;
            
            // Find the two data points to interpolate between
            let startIndex = 0;
            let endIndex = 0;
            let interpolationProgress = 0;
            
            // Find the data points that bracket our target time
            for (let i = 0; i < this.sensorData.length - 1; i++) {
                const currentTimestamp = this.sensorData[i].timestamp;
                const nextTimestamp = this.sensorData[i + 1].timestamp;
                
                if (targetDataTime >= currentTimestamp && targetDataTime <= nextTimestamp) {
                    startIndex = i;
                    endIndex = i + 1;
                    const timeDiff = nextTimestamp - currentTimestamp;
                    if (timeDiff > 0) {
                        interpolationProgress = (targetDataTime - currentTimestamp) / timeDiff;
                    }
                    break;
                } else if (targetDataTime < currentTimestamp) {
                    // Before first data point
                    startIndex = 0;
                    endIndex = 0;
                    break;
                } else if (i === this.sensorData.length - 2 && targetDataTime > nextTimestamp) {
                    // After last data point
                    startIndex = this.sensorData.length - 1;
                    endIndex = this.sensorData.length - 1;
                    break;
                }
            }
            
            // If we've reached the end, loop back to start
            if (targetDataTime > this.dataEndTime) {
                this.playbackStartTime = currentTime;
                startIndex = 0;
                endIndex = 0;
                interpolationProgress = 0;
            }
            
            // Update current index for timeline
            this.currentIndex = startIndex;
            
            // Interpolate between data points
            if (startIndex === endIndex) {
                // Single data point
                this.updatePhoneModel(this.sensorData[startIndex]);
                this.updateUI(this.sensorData[startIndex]);
            } else {
                // Interpolate between two data points
                const interpolatedData = this.interpolateData(
                    this.sensorData[startIndex],
                    this.sensorData[endIndex],
                    interpolationProgress
                );
                this.updatePhoneModel(interpolatedData);
                this.updateUI(interpolatedData);
            }
            
            // Update timeline during playback
            this.updateTimeline();
        }

        this.controls.update();
        this.renderer.render(this.scene, this.camera);
    }

    interpolateData(startData, endData, progress) {
        const interpolated = {};
        
        // Interpolate all numeric values, handling null values
        Object.keys(startData).forEach(key => {
            if (key === 'timestamp' || key === 'device_id' || key === 'type') {
                // Don't interpolate non-numeric fields
                interpolated[key] = endData[key];
            } else if (typeof startData[key] === 'number' && typeof endData[key] === 'number') {
                // Both values are numbers - interpolate
                interpolated[key] = startData[key] + (endData[key] - startData[key]) * progress;
            } else if (startData[key] === null && endData[key] !== null) {
                // Start is null, end is not - use end value
                interpolated[key] = endData[key];
            } else if (startData[key] !== null && endData[key] === null) {
                // Start is not null, end is null - use start value
                interpolated[key] = startData[key];
            } else if (startData[key] === null && endData[key] === null) {
                // Both are null - keep null
                interpolated[key] = null;
            } else {
                // Fallback - use target value
                interpolated[key] = endData[key];
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

    seekToPosition(event) {
        if (this.sensorData.length === 0) return;
        
        const rect = this.timelineTrack.getBoundingClientRect();
        const clickX = event.clientX - rect.left;
        const percentage = Math.max(0, Math.min(1, clickX / rect.width));
        
        this.seekToPercentage(percentage);
    }

    startDragging(event) {
        if (this.sensorData.length === 0) return;
        
        event.preventDefault();
        const handle = this.timelineHandle;
        const track = this.timelineTrack;
        
        const startX = event.clientX;
        const startLeft = parseFloat(handle.style.left) || 0;
        
        const onMouseMove = (e) => {
            const deltaX = e.clientX - startX;
            const rect = track.getBoundingClientRect();
            const newLeft = Math.max(0, Math.min(100, startLeft + (deltaX / rect.width) * 100));
            
            handle.style.left = newLeft + '%';
            this.timelineProgress.style.width = newLeft + '%';
            
            this.seekToPercentage(newLeft / 100);
        };
        
        const onMouseUp = () => {
            document.removeEventListener('mousemove', onMouseMove);
            document.removeEventListener('mouseup', onMouseUp);
        };
        
        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
    }

    seekToPercentage(percentage) {
        if (this.sensorData.length === 0) return;
        
        const totalDuration = this.dataEndTime - this.dataStartTime;
        const targetTime = this.dataStartTime + (totalDuration * percentage);
        
        // Find the closest data point
        let targetIndex = 0;
        for (let i = 0; i < this.sensorData.length; i++) {
            if (this.sensorData[i].timestamp >= targetTime) {
                targetIndex = i;
                break;
            }
        }
        
        this.currentIndex = targetIndex;
        this.updatePhoneModel(this.sensorData[this.currentIndex]);
        this.updateUI(this.sensorData[this.currentIndex]);
        this.updateTimeline();
    }

    updateTimeline() {
        if (this.sensorData.length === 0) return;
        
        const totalDuration = this.dataEndTime - this.dataStartTime;
        const currentTime = this.sensorData[this.currentIndex].timestamp - this.dataStartTime;
        const percentage = totalDuration > 0 ? (currentTime / totalDuration) : 0;
        
        // Update progress bar and handle
        this.timelineProgress.style.width = (percentage * 100) + '%';
        this.timelineHandle.style.left = (percentage * 100) + '%';
        
        // Update time displays
        const currentSeconds = Math.floor(currentTime / 1000);
        const totalSeconds = Math.floor(totalDuration / 1000);
        this.currentTimeDisplay.textContent = this.formatTime(currentSeconds);
        this.totalTimeDisplay.textContent = this.formatTime(totalSeconds);
        
        // Update timestamp display
        const currentData = this.sensorData[this.currentIndex];
        const timestamp = new Date(currentData.timestamp);
        this.timestampDisplay.textContent = timestamp.toISOString();
    }

    formatTime(seconds) {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
}

// Initialize the visualizer when the page loads
document.addEventListener('DOMContentLoaded', () => {
    new PhoneDataVisualizer();
}); 