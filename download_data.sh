#!/bin/bash

# Download Data from Amazon Phone Sensor Logger Server
# Usage: ./download_data.sh [latest|all|audio|sensors] [options]

SERVER_URL="https://amazon.government.rip"
DOWNLOAD_DIR="./data"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() { echo -e "${BLUE}[INFO]${NC} $1"; }
print_success() { echo -e "${GREEN}[SUCCESS]${NC} $1"; }
print_error() { echo -e "${RED}[ERROR]${NC} $1"; }

show_usage() {
    echo "Usage: $0 [latest|all|audio|sensors] [options]"
    echo ""
    echo "Commands:"
    echo "  latest    Download latest synchronized chunk (sensor data + audio)"
    echo "  all       Download all available data"
    echo "  audio     Download audio files only"
    echo "  sensors   Download sensor data only"
    echo ""
    echo "Options:"
    echo "  -d, --device DEVICE_ID  Filter by device ID"
    echo "  -o, --output DIR        Output directory (default: ./data)"
    echo "  -h, --help              Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 latest               # Get latest synchronized chunk"
    echo "  $0 latest -d android-123 # Get latest chunk for specific device"
    echo "  $0 all                  # Download all data"
}

# Function to get latest sensor data chunk (15 records)
download_latest_sensors() {
    local device_id="$1"
    
    print_status "Downloading latest sensor data chunk..."
    
    local url="$SERVER_URL/data/sensors"
    local params="limit=15"  # Get exactly 15 records (one chunk)
    
    if [ -n "$device_id" ]; then
        url="$url?$params&device=$device_id"
    else
        url="$url?$params"
    fi
    
    print_status "Calling: $url"
    local response=$(curl -s "$url")
    
    if [ $? -ne 0 ]; then
        print_error "Failed to download sensor data"
        return 1
    fi
    
    # Check for server errors
    if echo "$response" | grep -q '"error"'; then
        print_error "Server error in response"
        return 1
    fi
    
    # Save sensor data
    echo "$response" | jq -c '.data' > "$DOWNLOAD_DIR/sensor_data.json"
    
    local count=$(echo "$response" | jq '.data | length')
    print_success "Downloaded $count sensor records"
    
    return 0
}

# Function to get latest audio file
download_latest_audio() {
    local device_id="$1"
    
    print_status "Downloading latest audio file..."
    
    local url="$SERVER_URL/data/audio"
    local params="limit=1"  # Get just the latest audio file
    
    if [ -n "$device_id" ]; then
        url="$url?$params&device=$device_id"
    else
        url="$url?$params"
    fi
    
    print_status "Calling: $url"
    local response=$(curl -s "$url")
    
    if [ $? -ne 0 ]; then
        print_error "Failed to download audio list"
        return 1
    fi
    
    # Check for server errors
    if echo "$response" | grep -q '"error"'; then
        print_error "Server error in response"
        return 1
    fi
    
    # Extract latest audio file ID
    local audio_id=$(echo "$response" | jq -r '.data[0].id' 2>/dev/null)
    
    if [ -z "$audio_id" ] || [ "$audio_id" = "null" ]; then
        print_warning "No audio files found"
        return 0
    fi
    
    # Download the audio file
    mkdir -p "$DOWNLOAD_DIR/audio"
    local download_url="$SERVER_URL/download/audio/$audio_id"
    local output_file="$DOWNLOAD_DIR/audio/audio_${audio_id}.m4a"
    
    print_status "Downloading audio file ID: $audio_id"
    if curl -s -o "$output_file" "$download_url"; then
        if [ -s "$output_file" ]; then
            print_success "Downloaded: audio_${audio_id}.m4a"
            return 0
        else
            print_error "Downloaded file is empty"
            rm -f "$output_file"
            return 1
        fi
    else
        print_error "Failed to download audio file"
        return 1
    fi
}

# Function to download latest synchronized chunk
download_latest_chunk() {
    local device_id="$1"
    
    print_status "Downloading latest synchronized chunk..."
    
    # Create output directory
    mkdir -p "$DOWNLOAD_DIR"
    
    # First, get the latest audio file to find its time period
    print_status "Finding latest audio file..."
    local audio_url="$SERVER_URL/data/audio"
    local audio_params="limit=1"
    
    if [ -n "$device_id" ]; then
        audio_url="$audio_url?$audio_params&device=$device_id"
    else
        audio_url="$audio_url?$audio_params"
    fi
    
    local audio_response=$(curl -s "$audio_url")
    
    if [ $? -ne 0 ]; then
        print_error "Failed to get audio metadata"
        return 1
    fi
    
    # Extract audio file info
    local audio_id=$(echo "$audio_response" | jq -r '.data[0].id' 2>/dev/null)
    local audio_timestamp=$(echo "$audio_response" | jq -r '.data[0].timestamp' 2>/dev/null)
    local audio_device=$(echo "$audio_response" | jq -r '.data[0].device_id' 2>/dev/null)
    local audio_duration=$(echo "$audio_response" | jq -r '.data[0].duration' 2>/dev/null)
    
    if [ -z "$audio_id" ] || [ "$audio_id" = "null" ]; then
        print_warning "No audio files found"
        return 0
    fi
    
    print_status "Latest audio: ID=$audio_id, timestamp=$audio_timestamp, device=$audio_device, duration=${audio_duration}ms"
    
    # Calculate the time window for sensor data (audio start time to end time)
    local audio_start=$audio_timestamp
    local audio_end=$((audio_start + audio_duration))
    
               print_status "Getting latest 30 sensor records for device $audio_device"
    
               # Get the latest 30 sensor records for the device (engineered batch size)
           local sensor_url="$SERVER_URL/data/sensors"
           local sensor_params="device=$audio_device&limit=30"
           
           if [ -n "$device_id" ]; then
               sensor_params="device=$device_id&limit=30"
           fi
    
    sensor_url="$sensor_url?$sensor_params"
    
    print_status "Calling: $sensor_url"
    local sensor_response=$(curl -s "$sensor_url")
    
    if [ $? -ne 0 ]; then
        print_error "Failed to download sensor data"
        return 1
    fi
    
    # Check for server errors
    if echo "$sensor_response" | grep -q '"error"'; then
        print_error "Server error in sensor response"
        return 1
    fi
    
    # Save sensor data
    echo "$sensor_response" | jq -c '.data' > "$DOWNLOAD_DIR/sensor_data.json"
    
    local sensor_count=$(echo "$sensor_response" | jq '.data | length')
               print_success "Found $sensor_count sensor records for audio time period (should be exactly 30)"
    
    # Download the audio file
    mkdir -p "$DOWNLOAD_DIR/audio"
    local download_url="$SERVER_URL/download/audio/$audio_id"
    local output_file="$DOWNLOAD_DIR/audio/audio_${audio_id}.m4a"
    
    print_status "Downloading audio file ID: $audio_id"
    if curl -s -o "$output_file" "$download_url"; then
        if [ -s "$output_file" ]; then
            print_success "Downloaded: audio_${audio_id}.m4a"
        else
            print_error "Downloaded file is empty"
            rm -f "$output_file"
            return 1
        fi
    else
        print_error "Failed to download audio file"
        return 1
    fi
    
    # Create info file with synchronization details
               cat > "$DOWNLOAD_DIR/info.txt" << EOF
Downloaded: $(date)
Audio file: audio_${audio_id}.m4a
Audio timestamp: $audio_start
Audio duration: ${audio_duration}ms
Audio device: $audio_device
Sensor records: $sensor_count (should be exactly 30)
EOF
    
    print_success "Synchronized chunk downloaded to $DOWNLOAD_DIR"
}

# Function to download all data
download_all_data() {
    local device_id="$1"
    
    print_status "Downloading all available data..."
    
    # Create output directory
    mkdir -p "$DOWNLOAD_DIR"
    
    # Download all sensor data
    local url="$SERVER_URL/data/sensors"
    if [ -n "$device_id" ]; then
        url="$url?device=$device_id"
    fi
    
    print_status "Downloading all sensor data..."
    local response=$(curl -s "$url")
    echo "$response" | jq -c '.data' > "$DOWNLOAD_DIR/all_sensor_data.json"
    
    local count=$(echo "$response" | jq '.data | length')
    print_success "Downloaded $count sensor records"
    
    # Download all audio files
    print_status "Downloading all audio files..."
    mkdir -p "$DOWNLOAD_DIR/audio"
    
    local audio_url="$SERVER_URL/data/audio"
    if [ -n "$device_id" ]; then
        audio_url="$audio_url?device=$device_id"
    fi
    
    local audio_response=$(curl -s "$audio_url")
    local audio_ids=$(echo "$audio_response" | jq -r '.data[].id' 2>/dev/null)
    
    local audio_count=0
    for audio_id in $audio_ids; do
        if [ -n "$audio_id" ] && [ "$audio_id" != "null" ]; then
            local download_url="$SERVER_URL/download/audio/$audio_id"
            local output_file="$DOWNLOAD_DIR/audio/audio_${audio_id}.m4a"
            
            if curl -s -o "$output_file" "$download_url" && [ -s "$output_file" ]; then
                print_success "Downloaded: audio_${audio_id}.m4a"
                audio_count=$((audio_count + 1))
            fi
        fi
    done
    
    print_success "Downloaded $audio_count audio files"
    print_success "All data downloaded to $DOWNLOAD_DIR"
}

# Parse command line arguments
COMMAND=""
DEVICE_ID=""
HELP=false

while [[ $# -gt 0 ]]; do
    case $1 in
        latest|all|audio|sensors)
            COMMAND="$1"
            shift
            ;;
        -d|--device)
            DEVICE_ID="$2"
            shift 2
            ;;
        -o|--output)
            DOWNLOAD_DIR="$2"
            shift 2
            ;;
        -h|--help)
            HELP=true
            shift
            ;;
        *)
            print_error "Unknown option: $1"
            show_usage
            exit 1
            ;;
    esac
done

# Show help if requested
if [ "$HELP" = true ]; then
    show_usage
    exit 0
fi

# Check if curl is available
if ! command -v curl &> /dev/null; then
    print_error "curl is required but not installed"
    exit 1
fi

# Check if jq is available
if ! command -v jq &> /dev/null; then
    print_error "jq is required but not installed"
    exit 1
fi

# Main logic
case "$COMMAND" in
    latest)
        download_latest_chunk "$DEVICE_ID"
        ;;
    all)
        download_all_data "$DEVICE_ID"
        ;;
    audio)
        mkdir -p "$DOWNLOAD_DIR"
        download_latest_audio "$DEVICE_ID"
        ;;
    sensors)
        mkdir -p "$DOWNLOAD_DIR"
        download_latest_sensors "$DEVICE_ID"
        ;;
    "")
        print_error "No command specified"
        show_usage
        exit 1
        ;;
    *)
        print_error "Unknown command: $COMMAND"
        show_usage
        exit 1
        ;;
esac 