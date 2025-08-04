const express = require('express');
const path = require('path');
const app = express();
const PORT = process.env.PORT || 3000;

// Serve static files
app.use(express.static(__dirname));

// Serve the main page
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

// Health check endpoint
app.get('/health', (req, res) => {
    res.json({ status: 'ok', message: 'Phone Data Visualizer is running' });
});

app.listen(PORT, () => {
    console.log(`🚀 Phone Data Visualizer running on http://localhost:${PORT}`);
    console.log(`📁 Load your sensor data JSON files to visualize phone movement`);
}); 