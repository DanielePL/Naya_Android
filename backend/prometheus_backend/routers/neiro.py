"""
Neiro Live Tracking Router - WebSocket-based barbell detection for Prometheus App

This router provides:
- WebSocket endpoint for real-time barbell tracking
- YOLO-based detection with position locking
- Coordinate smoothing and trajectory tracking

Usage from Android:
    Connect to: wss://your-render-domain.onrender.com/neiro/track
"""

import asyncio
import json
import logging
import time
from typing import Optional, Dict, Any
from pathlib import Path

import cv2
import numpy as np
from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from starlette.websockets import WebSocketState

# Configure logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

router = APIRouter(prefix="/neiro", tags=["neiro"])

# ═══════════════════════════════════════════════════════════════════════════════
# CONFIGURATION
# ═══════════════════════════════════════════════════════════════════════════════

MODEL_PATH = Path(__file__).parent.parent / "bestv2.pt"
ONNX_MODEL_PATH = Path(__file__).parent.parent / "bestv2.onnx"
CONFIDENCE_THRESHOLD = 0.25
IMGSZ = 640


# ═══════════════════════════════════════════════════════════════════════════════
# BARBELL TRACKER
# ═══════════════════════════════════════════════════════════════════════════════

class BarbellTracker:
    """YOLO-based barbell tracker with position locking and smoothing"""

    def __init__(self):
        self.model = None
        self.locked_position: Optional[tuple] = None
        self.lock_radius = 200
        self.ema_alpha = 0.4
        self.smoothed_x: Optional[float] = None
        self.smoothed_y: Optional[float] = None
        self.frame_count = 0
        self.detection_count = 0
        self.last_inference_ms = 0
        self._initialized = False

    def initialize(self) -> bool:
        """Lazy-load the YOLO model"""
        if self._initialized:
            return True

        try:
            from ultralytics import YOLO

            # Try .pt first, fall back to .onnx
            if MODEL_PATH.exists():
                logger.info(f"Loading YOLO model: {MODEL_PATH}")
                self.model = YOLO(str(MODEL_PATH))
            elif ONNX_MODEL_PATH.exists():
                logger.info(f"Loading ONNX model: {ONNX_MODEL_PATH}")
                self.model = YOLO(str(ONNX_MODEL_PATH))
            else:
                logger.error(f"No model found at {MODEL_PATH} or {ONNX_MODEL_PATH}")
                return False

            self._initialized = True
            logger.info("Model loaded successfully")
            return True

        except Exception as e:
            logger.error(f"Failed to load model: {e}")
            return False

    def lock_to_position(self, x: float, y: float):
        """Lock detection to a specific position (tap-to-lock)"""
        self.locked_position = (x, y)
        self.smoothed_x = x
        self.smoothed_y = y
        logger.info(f"Locked to position: ({x:.0f}, {y:.0f})")

    def unlock(self):
        """Unlock position tracking"""
        self.locked_position = None
        logger.info("Position unlocked")

    def reset(self):
        """Reset tracker state for new session"""
        self.locked_position = None
        self.smoothed_x = None
        self.smoothed_y = None
        self.frame_count = 0
        self.detection_count = 0
        logger.info("Tracker reset")

    def detect(self, frame: np.ndarray) -> Dict[str, Any]:
        """Run detection on a frame"""
        if not self._initialized or self.model is None:
            return {"error": "Model not initialized"}

        self.frame_count += 1
        start_time = time.time()

        try:
            results = self.model(frame, imgsz=IMGSZ, conf=CONFIDENCE_THRESHOLD, verbose=False)
            self.last_inference_ms = (time.time() - start_time) * 1000
        except Exception as e:
            logger.error(f"Detection error: {e}")
            return {"error": str(e), "inference_ms": 0}

        best_detection = None
        best_score = 0

        for result in results:
            boxes = result.boxes
            if boxes is None or len(boxes) == 0:
                continue

            for box in boxes:
                conf = float(box.conf[0])
                x1, y1, x2, y2 = box.xyxy[0].tolist()
                cx, cy = (x1 + x2) / 2, (y1 + y2) / 2
                w, h = x2 - x1, y2 - y1

                # If locked, only consider detections near lock position
                if self.locked_position:
                    dist = np.sqrt(
                        (cx - self.locked_position[0])**2 +
                        (cy - self.locked_position[1])**2
                    )
                    if dist > self.lock_radius:
                        continue
                    score = conf * (1 - dist / self.lock_radius)
                else:
                    score = conf

                if score > best_score:
                    best_score = score
                    best_detection = {"x": cx, "y": cy, "w": w, "h": h, "conf": conf}

        if best_detection:
            self.detection_count += 1

            # Apply EMA smoothing
            if self.smoothed_x is None:
                self.smoothed_x = best_detection["x"]
                self.smoothed_y = best_detection["y"]
            else:
                self.smoothed_x = self.ema_alpha * best_detection["x"] + (1 - self.ema_alpha) * self.smoothed_x
                self.smoothed_y = self.ema_alpha * best_detection["y"] + (1 - self.ema_alpha) * self.smoothed_y

            # Update lock position to follow barbell
            if self.locked_position:
                self.locked_position = (self.smoothed_x, self.smoothed_y)

            return {
                "x": self.smoothed_x,
                "y": self.smoothed_y,
                "w": best_detection["w"],
                "h": best_detection["h"],
                "conf": best_detection["conf"],
                "inference_ms": self.last_inference_ms
            }
        else:
            return {
                "x": 0,
                "y": 0,
                "w": 0,
                "h": 0,
                "conf": 0,
                "inference_ms": self.last_inference_ms
            }


# ═══════════════════════════════════════════════════════════════════════════════
# WEBSOCKET ENDPOINT
# ═══════════════════════════════════════════════════════════════════════════════

# Per-connection tracker instances
active_trackers: Dict[str, BarbellTracker] = {}


@router.websocket("/track")
async def websocket_track(websocket: WebSocket):
    """
    WebSocket endpoint for real-time barbell tracking

    Protocol:
    - Binary messages: JPEG-encoded frames -> returns JSON detection result
    - Text messages: JSON commands (lock, unlock, ping, reset)

    Commands:
    - {"action": "lock", "x": 100, "y": 200} - Lock to position
    - {"action": "unlock"} - Unlock position
    - {"action": "ping"} - Health check
    - {"action": "reset"} - Reset tracker state
    """
    await websocket.accept()

    client_id = f"{websocket.client.host}:{websocket.client.port}"
    logger.info(f"Client connected: {client_id}")

    # Create tracker for this connection
    tracker = BarbellTracker()
    if not tracker.initialize():
        await websocket.send_json({"error": "Failed to initialize model"})
        await websocket.close()
        return

    active_trackers[client_id] = tracker

    try:
        while True:
            message = await websocket.receive()

            if message["type"] == "websocket.disconnect":
                break

            # Handle binary data (JPEG frames)
            if "bytes" in message:
                try:
                    # Decode JPEG to numpy array
                    nparr = np.frombuffer(message["bytes"], np.uint8)
                    frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

                    if frame is None:
                        await websocket.send_json({"error": "Invalid image data"})
                        continue

                    # Run detection
                    result = tracker.detect(frame)
                    await websocket.send_json(result)

                except Exception as e:
                    logger.error(f"Frame processing error: {e}")
                    await websocket.send_json({"error": str(e)})

            # Handle text data (JSON commands)
            elif "text" in message:
                try:
                    cmd = json.loads(message["text"])
                    action = cmd.get("action", "")

                    if action == "lock":
                        x = cmd.get("x", 0)
                        y = cmd.get("y", 0)
                        tracker.lock_to_position(x, y)
                        await websocket.send_json({"status": "locked", "x": x, "y": y})

                    elif action == "unlock":
                        tracker.unlock()
                        await websocket.send_json({"status": "unlocked"})

                    elif action == "ping":
                        await websocket.send_json({
                            "status": "pong",
                            "frame_count": tracker.frame_count,
                            "detection_count": tracker.detection_count
                        })

                    elif action == "reset":
                        tracker.reset()
                        await websocket.send_json({"status": "reset"})

                    else:
                        await websocket.send_json({"error": f"Unknown action: {action}"})

                except json.JSONDecodeError:
                    await websocket.send_json({"error": "Invalid JSON"})
                except Exception as e:
                    await websocket.send_json({"error": str(e)})

    except WebSocketDisconnect:
        logger.info(f"Client disconnected: {client_id}")
    except Exception as e:
        logger.error(f"WebSocket error: {e}")
    finally:
        # Cleanup
        if client_id in active_trackers:
            del active_trackers[client_id]
        logger.info(f"Cleaned up tracker for: {client_id}")


# ═══════════════════════════════════════════════════════════════════════════════
# REST ENDPOINTS (for testing and status)
# ═══════════════════════════════════════════════════════════════════════════════

@router.get("/status")
async def neiro_status():
    """Get Neiro service status"""
    model_available = MODEL_PATH.exists() or ONNX_MODEL_PATH.exists()

    return {
        "service": "Neiro Live Tracking",
        "status": "running",
        "model_available": model_available,
        "model_path": str(MODEL_PATH) if MODEL_PATH.exists() else str(ONNX_MODEL_PATH),
        "active_connections": len(active_trackers),
        "websocket_endpoint": "/neiro/track"
    }


@router.get("/connections")
async def list_connections():
    """List active WebSocket connections"""
    return {
        "count": len(active_trackers),
        "connections": [
            {
                "client_id": client_id,
                "frame_count": tracker.frame_count,
                "detection_count": tracker.detection_count,
                "locked": tracker.locked_position is not None
            }
            for client_id, tracker in active_trackers.items()
        ]
    }