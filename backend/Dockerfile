FROM python:3.11-slim

# Install system dependencies for OpenCV and MediaPipe
RUN apt-get update && apt-get install -y \
    ffmpeg \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxrender1 \
    libxext6 \
    libgomp1 \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

# Create directories for uploads and outputs
RUN mkdir -p uploads outputs

# Expose port
EXPOSE 8000

# Set Python path
ENV PYTHONPATH=/app:$PYTHONPATH

# Start command
CMD ["uvicorn", "prometheus_backend.main:app", "--host", "0.0.0.0", "--port", "8000"]
