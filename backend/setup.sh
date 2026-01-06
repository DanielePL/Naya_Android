#!/bin/bash
# Prometheus Backend - Sports2D Service Setup Script

echo "🔥 Prometheus Backend - Sports2D Form Analysis Setup"
echo "=================================================="

# Check Python version
echo ""
echo "📋 Checking Python version..."
python_version=$(python3 --version 2>&1 | awk '{print $2}')
echo "Found Python: $python_version"

# Check if Python 3.10-3.12
if ! python3 -c 'import sys; exit(0 if (3,10) <= sys.version_info < (3,13) else 1)' 2>/dev/null; then
    echo "❌ Error: Python 3.10, 3.11, or 3.12 required"
    echo "   Your version: $python_version"
    exit 1
fi

# Create virtual environment
echo ""
echo "🔧 Creating virtual environment..."
python3 -m venv venv

# Activate virtual environment
echo ""
echo "🔧 Activating virtual environment..."
source venv/bin/activate

# Upgrade pip
echo ""
echo "📦 Upgrading pip..."
pip install --upgrade pip

# Install dependencies
echo ""
echo "📦 Installing dependencies..."
echo "   This may take a few minutes..."
pip install -r requirements.txt

# Create directories
echo ""
echo "📁 Creating upload and output directories..."
mkdir -p prometheus_backend/uploads
mkdir -p prometheus_backend/outputs

# Test installation
echo ""
echo "✅ Testing Sports2D installation..."
python3 -c "import sports2d; print('✅ Sports2D version:', sports2d.__version__)" || {
    echo "❌ Sports2D installation failed"
    exit 1
}

python3 -c "import fastapi; print('✅ FastAPI installed')" || {
    echo "❌ FastAPI installation failed"
    exit 1
}

echo ""
echo "=================================================="
echo "✅ Setup complete!"
echo ""
echo "To start the server:"
echo "  1. Activate virtual environment: source venv/bin/activate"
echo "  2. Run server: python prometheus_backend/main.py"
echo ""
echo "Server will be available at: http://localhost:8000"
echo "API documentation: http://localhost:8000/docs"
echo "=================================================="
