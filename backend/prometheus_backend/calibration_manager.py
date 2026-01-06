"""
Calibration Manager - Honest 3-Tier Velocity Measurement System

TIER 1: LiDAR/ToF Depth (Premium - Exact m/s) - Future
TIER 2: Reference Object (Standard - Calibrated m/s) - Current
TIER 3: Relative Speed Index (Fallback - Honest arbitrary units)
"""

import numpy as np
from typing import Dict, Optional, Tuple
import cv2


class CalibrationManager:
    """Manages calibration for velocity measurements with honest tier system"""

    # Reference object sizes (in meters)
    REFERENCE_OBJECTS = {
        "weight_plate_45": 0.45,  # Standard 45lb/20kg plate diameter (Olympic)
        "weight_plate_35": 0.37,  # 35lb/15kg plate
        "weight_plate_25": 0.28,  # 25lb/10kg plate
        "barbell_plate": 0.45,    # Alias for Olympic plate
        "yoga_mat": 1.80,         # Standard yoga mat length
        "doorframe": 2.00,        # Standard door height
    }

    # Body proportions for height-based calibration
    # Based on anthropometric data (average adult)
    BODY_PROPORTIONS = {
        "shoulder_to_hip": 0.288,    # ~28.8% of height
        "hip_to_knee": 0.245,        # ~24.5% of height
        "shoulder_width": 0.259,     # ~25.9% of height (biacromial)
        "arm_length": 0.440,         # ~44% of height (shoulder to wrist)
    }

    def __init__(self, verbose: bool = True):
        """Initialize calibration manager"""
        self.calibration_method = None
        self.calibration_tier = "tier3_relative"  # Default
        self.pixels_per_meter = None
        self.confidence = None
        self.reference_object = None
        self.user_height_m = None
        self.verbose = verbose

    def detect_calibration_method(
        self,
        frame: np.ndarray,
        depth_data: Optional[Dict] = None,
        user_reference: Optional[Dict] = None
    ) -> str:
        """
        Detect best available calibration method

        Args:
            frame: Video frame for reference detection
            depth_data: Optional LiDAR/ToF depth data (future)
            user_reference: Optional user-provided reference (e.g., {"type": "weight_plate_45", "pixels": 120})

        Returns:
            Calibration tier: "lidar", "reference", or "relative"
        """

        # TIER 1: Check for depth data (future - iOS/Android depth APIs)
        if depth_data is not None:
            return self._calibrate_with_depth(depth_data)

        # TIER 2: User-provided reference
        if user_reference is not None:
            return self._calibrate_with_user_reference(user_reference)

        # TIER 2: Auto-detect reference objects (future - YOLO weight plate detector)
        # reference = self._detect_reference_object(frame)
        # if reference:
        #     return self._calibrate_with_reference(reference)

        # TIER 3: No calibration possible - use relative speed
        return self._use_relative_speed()

    def _calibrate_with_depth(self, depth_data: Dict) -> str:
        """
        Use LiDAR/ToF depth for exact calibration

        Future implementation for:
        - iOS: ARKit Depth API (iPhone 12 Pro+)
        - Android: ARCore Depth API (Samsung S20+, Pixel 4+)
        """
        distance_to_person = depth_data.get('average_depth', 2.0)  # meters
        focal_length = depth_data.get('focal_length', 1000)  # pixels

        # Calculate pixels per meter based on depth and focal length
        # ppm = focal_length / distance
        self.pixels_per_meter = focal_length / distance_to_person
        self.calibration_method = "lidar"
        self.confidence = 0.95

        if self.verbose:
            print(f"✅ TIER 1 Active: LiDAR calibration ({self.pixels_per_meter:.1f} px/m)")

        return "lidar"

    def _calibrate_with_user_reference(self, user_reference: Dict) -> str:
        """
        Calibrate using user-provided reference object

        Args:
            user_reference: {"type": "weight_plate_45", "diameter_pixels": 120}
        """
        ref_type = user_reference.get("type")
        pixels = user_reference.get("diameter_pixels") or user_reference.get("height_pixels")

        if ref_type not in self.REFERENCE_OBJECTS:
            if self.verbose:
                print(f"⚠️ Unknown reference object: {ref_type}, falling back to relative")
            return self._use_relative_speed()

        meters = self.REFERENCE_OBJECTS[ref_type]
        self.pixels_per_meter = pixels / meters
        self.calibration_method = "reference"
        self.confidence = 0.80
        self.reference_object = ref_type

        if self.verbose:
            print(f"✅ TIER 2 Active: Reference calibration using {ref_type} ({self.pixels_per_meter:.1f} px/m)")

        return "reference"

    def _detect_reference_object(self, frame: np.ndarray) -> Optional[Dict]:
        """
        Auto-detect reference objects using YOLO

        Future implementation:
        - Detect weight plates
        - Detect yoga mats
        - Detect standard equipment
        """
        # TODO: Implement YOLO weight plate detector
        # detections = self.yolo_model.predict(frame)
        # for det in detections:
        #     if det['class'] == 'weight_plate_45':
        #         return {
        #             "type": "weight_plate_45",
        #             "diameter_pixels": det['width']
        #         }
        return None

    def _use_relative_speed(self) -> str:
        """
        Fallback: No calibration possible, use relative speed index

        HONEST APPROACH:
        - Output speed index (0-100) instead of fake m/s
        - Still useful for tracking consistency and velocity drop
        - Clear communication that values are relative
        """
        self.pixels_per_meter = None
        self.calibration_method = "relative"
        self.confidence = 0.50

        if self.verbose:
            print(f"⚠️ TIER 3 Active: Relative speed mode (no absolute m/s)")

        return "relative"

    def get_calibration_info(self) -> Dict:
        """Get calibration information for API response"""

        if self.calibration_method == "lidar":
            return {
                "tier": "pro",
                "method": "LiDAR Depth Tracking",
                "confidence": self.confidence,
                "unit": "m/s",
                "badge": {
                    "icon": "⚡",
                    "text": "Pro Mode - LiDAR Active",
                    "color": "#00FF88"
                },
                "note": "Accurate velocity measurements using depth sensor"
            }

        elif self.calibration_method == "reference":
            return {
                "tier": "calibrated",
                "method": f"Reference Object ({self.reference_object})",
                "confidence": self.confidence,
                "unit": "m/s",
                "badge": {
                    "icon": "📏",
                    "text": "Calibrated Mode",
                    "color": "#FFAA5E"
                },
                "note": "Velocity calibrated using reference object"
            }

        else:  # relative
            return {
                "tier": "relative",
                "method": "Relative Speed Index",
                "confidence": self.confidence,
                "unit": "speed_index",
                "badge": {
                    "icon": "📊",
                    "text": "Relative Speed Mode",
                    "color": "#FF9D50"
                },
                "note": "Values are relative for consistency tracking, not absolute m/s"
            }

    def convert_pixels_to_meters(self, pixels: float) -> float:
        """
        Convert pixel distance to meters

        Returns 0 if no calibration available (use relative speed instead)
        """
        if self.pixels_per_meter is None:
            return 0.0
        return pixels / self.pixels_per_meter

    def is_calibrated(self) -> bool:
        """Check if we have valid calibration (not relative mode)"""
        return self.pixels_per_meter is not None

    def calibrate_from_barbell(self, box_height_pixels: float, plate_diameter_m: float = 0.45) -> bool:
        """
        Calibrate using detected barbell/plate box height.

        This is the preferred calibration method when YOLO barbell detection is available.
        Standard Olympic plate diameter is 450mm (45cm).

        Args:
            box_height_pixels: Height of the YOLO detection box in pixels
            plate_diameter_m: Known diameter of the plate (default: 0.45m for Olympic)

        Returns:
            True if calibration successful
        """
        if box_height_pixels <= 20:
            if self.verbose:
                print(f"⚠️ Box height too small ({box_height_pixels}px), skipping calibration")
            return False

        self.pixels_per_meter = box_height_pixels / plate_diameter_m
        self.calibration_method = "barbell"
        self.calibration_tier = "tier2_reference"
        self.confidence = 0.85
        self.reference_object = "barbell_plate"

        if self.verbose:
            print(f"✅ TIER 2 Active: Barbell calibration ({self.pixels_per_meter:.1f} px/m)")
            print(f"   Box height: {box_height_pixels:.0f}px = {plate_diameter_m:.2f}m plate")

        return True

    def calibrate_from_user_height(
        self,
        user_height_cm: float,
        shoulder_to_hip_pixels: float
    ) -> bool:
        """
        Calibrate using user's body proportions when no barbell is detected.

        Uses anthropometric data: shoulder-to-hip distance is ~28.8% of total height.

        Args:
            user_height_cm: User's height from profile (in cm)
            shoulder_to_hip_pixels: Measured shoulder-to-hip distance in pixels (from pose)

        Returns:
            True if calibration successful
        """
        if shoulder_to_hip_pixels <= 20:
            if self.verbose:
                print(f"⚠️ Shoulder-to-hip distance too small ({shoulder_to_hip_pixels}px), skipping")
            return False

        if user_height_cm <= 0:
            if self.verbose:
                print(f"⚠️ Invalid user height ({user_height_cm}cm), skipping")
            return False

        self.user_height_m = user_height_cm / 100.0

        # Calculate expected shoulder-to-hip distance in meters
        expected_shoulder_to_hip_m = self.user_height_m * self.BODY_PROPORTIONS["shoulder_to_hip"]

        # Calculate pixels per meter
        self.pixels_per_meter = shoulder_to_hip_pixels / expected_shoulder_to_hip_m
        self.calibration_method = "user_height"
        self.calibration_tier = "tier2_reference"
        self.confidence = 0.70  # Lower confidence than barbell (body proportions vary)
        self.reference_object = "body_proportion"

        if self.verbose:
            print(f"✅ TIER 2 Active: User height calibration ({self.pixels_per_meter:.1f} px/m)")
            print(f"   User height: {user_height_cm:.0f}cm, shoulder-to-hip: {shoulder_to_hip_pixels:.0f}px")

        return True

    def calibrate_auto(
        self,
        barbell_box_height: Optional[float] = None,
        user_height_cm: Optional[float] = None,
        shoulder_to_hip_pixels: Optional[float] = None,
        depth_data: Optional[Dict] = None
    ) -> str:
        """
        Automatically choose the best calibration method available.

        Priority:
        1. LiDAR depth data (most accurate)
        2. Barbell detection (very accurate, standardized reference)
        3. User height + body proportions (fallback)
        4. Relative mode (no absolute m/s)

        Returns:
            Calibration tier: "tier1_lidar", "tier2_reference", or "tier3_relative"
        """
        # TIER 1: LiDAR
        if depth_data is not None:
            self._calibrate_with_depth(depth_data)
            self.calibration_tier = "tier1_lidar"
            return self.calibration_tier

        # TIER 2a: Barbell (preferred)
        if barbell_box_height is not None and barbell_box_height > 20:
            if self.calibrate_from_barbell(barbell_box_height):
                return self.calibration_tier

        # TIER 2b: User height (fallback)
        if user_height_cm is not None and shoulder_to_hip_pixels is not None:
            if self.calibrate_from_user_height(user_height_cm, shoulder_to_hip_pixels):
                return self.calibration_tier

        # TIER 3: Relative
        self._use_relative_speed()
        self.calibration_tier = "tier3_relative"
        return self.calibration_tier
