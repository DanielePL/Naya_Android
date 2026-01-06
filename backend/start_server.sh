#!/bin/bash
# Start Prometheus Backend Server

cd "$(dirname "$0")"

# Check if virtual environment exists
if [ ! -d "venv" ]; then
    echo "❌ Virtual environment not found!"
    echo "Please run: ./setup.sh"
    exit 1
fi

# Activate virtual environment
source venv/bin/activate

# Get local IP for Android testing
echo "🔥 Starting Prometheus Backend Server"
echo "======================================"
echo ""
echo "Local access: http://localhost:8000"
echo "API docs: http://localhost:8000/docs"
echo ""

# Get local IP address
if command -v ifconfig &> /dev/null; then
    LOCAL_IP=$(ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}' | head -1)
    if [ ! -z "$LOCAL_IP" ]; then
        echo "Android device access (same network):"
        echo "  http://$LOCAL_IP:8000"
    fi
fi

echo ""
echo "======================================"
echo ""

# Start server
cd prometheus_backend
python main.py
