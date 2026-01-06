"""
Pose Processing with MediaPipe
Simpler and more reliable than Sports2D for mobile videos
"""

import cv2
import mediapipe as mp
import json
import numpy as np
import math
import gc
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from prometheus_backend.calibration_manager import CalibrationManager
from prometheus_backend.movement_velocity_calculator import MovementVelocityCalculator


class PoseProcessor:
    # 🎨 PROMETHEUS BRAND COLORS
    ORANGE = (80, 157, 255)  # BGR format: #FF9D50
    GLOW_ORANGE = (94, 170, 255)  # BGR: #FFAA5E
    CYAN = (255, 217, 0)  # BGR: #00D9FF
    GREEN = (136, 255, 0)  # BGR: #00FF88
    WHITE = (255, 255, 255)
    DARK_BG = (16, 20, 26)  # BGR: #1A1410

    # KEY JOINT INDICES (MediaPipe Pose)
    KEY_JOINTS = {
        'LEFT_HIP': 23,
        'RIGHT_HIP': 24,
        'LEFT_KNEE': 25,
        'RIGHT_KNEE': 26,
        'LEFT_SHOULDER': 11,
        'RIGHT_SHOULDER': 12,
        'LEFT_WRIST': 15,
        'RIGHT_WRIST': 16,
        'LEFT_ELBOW': 13,
        'RIGHT_ELBOW': 14,
    }

    def __init__(self):
        self.mp_pose = mp.solutions.pose
        self.mp_drawing = mp.solutions.drawing_utils
        self.mp_drawing_styles = mp.solutions.drawing_styles
        self.frame_count = 0

    def draw_glow_circle(self, frame: np.ndarray, center: Tuple[int, int],
                         radius: int, color: Tuple[int, int, int],
                         glow_size: int = 6) -> None:
        """Draw circle with glow effect"""
        # Create overlay for glow
        overlay = frame.copy()

        # Draw outer glow (multiple layers for smooth effect)
        for i in range(glow_size, 0, -1):
            alpha = 0.3 * (glow_size - i) / glow_size
            glow_color = tuple(int(c * alpha) for c in color)
            cv2.circle(overlay, center, radius + i, color, -1, cv2.LINE_AA)

        # Blend overlay with original
        cv2.addWeighted(overlay, 0.6, frame, 0.4, 0, frame)

        # Draw main circle
        cv2.circle(frame, center, radius, color, -1, cv2.LINE_AA)

        # Draw inner white highlight
        cv2.circle(frame, center, max(2, radius - 4), self.WHITE, -1, cv2.LINE_AA)

    def draw_glow_line(self, frame: np.ndarray, p1: Tuple[int, int],
                       p2: Tuple[int, int], thickness: int,
                       color: Tuple[int, int, int]) -> None:
        """Draw line with glow effect"""
        # Create overlay for glow
        overlay = frame.copy()

        # Draw glow layer
        for i in range(4, 0, -1):
            alpha = 0.2 * (4 - i) / 4
            cv2.line(overlay, p1, p2, color, thickness + i * 2, cv2.LINE_AA)

        # Blend overlay
        cv2.addWeighted(overlay, 0.5, frame, 0.5, 0, frame)

        # Draw main line
        cv2.line(frame, p1, p2, color, thickness, cv2.LINE_AA)

    def draw_text_with_background(self, frame: np.ndarray, text: str,
                                   position: Tuple[int, int],
                                   font_scale: float = 0.6,
                                   color: Tuple[int, int, int] = None) -> None:
        """Draw text with dark background"""
        if color is None:
            color = self.WHITE

        font = cv2.FONT_HERSHEY_SIMPLEX
        thickness = 2

        # Get text size
        (text_width, text_height), baseline = cv2.getTextSize(
            text, font, font_scale, thickness
        )

        # Draw background rectangle with transparency
        x, y = position
        padding = 8

        overlay = frame.copy()
        cv2.rectangle(
            overlay,
            (x - padding, y - text_height - padding),
            (x + text_width + padding, y + baseline + padding),
            self.DARK_BG,
            -1
        )
        cv2.addWeighted(overlay, 0.7, frame, 0.3, 0, frame)

        # Draw text
        cv2.putText(frame, text, position, font, font_scale, color, thickness, cv2.LINE_AA)

    def draw_pulsing_circle(self, frame: np.ndarray, center: Tuple[int, int],
                           base_radius: int, color: Tuple[int, int, int]) -> None:
        """Draw pulsing circle animation"""
        pulse = abs(math.sin(self.frame_count * 0.15)) * 8 + base_radius

        # Draw pulsing outer ring
        overlay = frame.copy()
        cv2.circle(overlay, center, int(pulse), color, 2, cv2.LINE_AA)
        cv2.addWeighted(overlay, 0.6, frame, 0.4, 0, frame)

    def calculate_form_score(self, landmarks, exercise_type: str = "general") -> Tuple[float, List[str]]:
        """
        Calculate form score (0-10) based on pose landmarks.

        Returns:
            Tuple of (score, list of feedback messages)
        """
        score = 10.0
        feedback = []

        if not landmarks:
            return 5.0, ["No pose detected"]

        # Get key landmarks
        lm = landmarks.landmark

        # Helper to get landmark if visible
        def get_point(idx):
            if lm[idx].visibility > 0.5:
                return (lm[idx].x, lm[idx].y, lm[idx].z)
            return None

        # Calculate angles
        def calc_angle(p1, p2, p3):
            """Calculate angle at p2 between p1-p2-p3"""
            if not all([p1, p2, p3]):
                return None
            v1 = np.array([p1[0] - p2[0], p1[1] - p2[1]])
            v2 = np.array([p3[0] - p2[0], p3[1] - p2[1]])
            cos_angle = np.dot(v1, v2) / (np.linalg.norm(v1) * np.linalg.norm(v2) + 1e-6)
            return np.degrees(np.arccos(np.clip(cos_angle, -1, 1)))

        # Get key points
        left_hip = get_point(self.KEY_JOINTS['LEFT_HIP'])
        right_hip = get_point(self.KEY_JOINTS['RIGHT_HIP'])
        left_knee = get_point(self.KEY_JOINTS['LEFT_KNEE'])
        right_knee = get_point(self.KEY_JOINTS['RIGHT_KNEE'])
        left_shoulder = get_point(self.KEY_JOINTS['LEFT_SHOULDER'])
        right_shoulder = get_point(self.KEY_JOINTS['RIGHT_SHOULDER'])

        # 1. CHECK KNEE VALGUS (knees caving in)
        if all([left_hip, right_hip, left_knee, right_knee]):
            hip_width = abs(left_hip[0] - right_hip[0])
            knee_width = abs(left_knee[0] - right_knee[0])

            if hip_width > 0:
                knee_ratio = knee_width / hip_width
                if knee_ratio < 0.85:  # Knees significantly inside hips
                    score -= 1.5
                    feedback.append("Knee valgus detected - keep knees tracking over toes")
                elif knee_ratio < 0.95:
                    score -= 0.5
                    feedback.append("Slight knee cave - focus on knee position")

        # 2. CHECK BACK ANGLE (for squat/deadlift - excessive forward lean)
        if exercise_type.lower() in ["squat", "back_squat", "front_squat", "deadlift"]:
            if all([left_shoulder, right_shoulder, left_hip, right_hip]):
                # Calculate torso angle from vertical
                shoulder_mid = ((left_shoulder[0] + right_shoulder[0]) / 2,
                               (left_shoulder[1] + right_shoulder[1]) / 2)
                hip_mid = ((left_hip[0] + right_hip[0]) / 2,
                          (left_hip[1] + right_hip[1]) / 2)

                # Angle from vertical (0 = upright, 90 = horizontal)
                dx = shoulder_mid[0] - hip_mid[0]
                dy = shoulder_mid[1] - hip_mid[1]
                torso_angle = abs(np.degrees(np.arctan2(dx, -dy)))  # -dy because y increases downward

                if torso_angle > 60:
                    score -= 2.0
                    feedback.append("Excessive forward lean - maintain upright torso")
                elif torso_angle > 45:
                    score -= 1.0
                    feedback.append("Forward lean detected - keep chest up")

        # 3. CHECK SHOULDER SYMMETRY
        if left_shoulder and right_shoulder:
            shoulder_diff = abs(left_shoulder[1] - right_shoulder[1])
            if shoulder_diff > 0.05:  # Normalized coordinates
                score -= 0.5
                feedback.append("Uneven shoulders - check bar position")

        # 4. CHECK HIP SYMMETRY
        if left_hip and right_hip:
            hip_diff = abs(left_hip[1] - right_hip[1])
            if hip_diff > 0.05:
                score -= 0.5
                feedback.append("Uneven hips - check stance width")

        # Clamp score
        score = max(0.0, min(10.0, score))

        if not feedback:
            feedback.append("Good form!")

        return round(score, 1), feedback

    def draw_metrics_overlay(self, frame: np.ndarray, vbt_metrics: Dict,
                            current_rep: int = 0, form_score: float = None,
                            form_feedback: List[str] = None) -> None:
        """Draw live VBT metrics overlay"""
        height, width = frame.shape[:2]

        # Top-Left: Live Metrics
        y_offset = 40

        if vbt_metrics and vbt_metrics.get('reps_detected', 0) > 0:
            summary = vbt_metrics.get('summary', {})
            peak_vel = summary.get('avg_peak_velocity_ms', 0)
            self.draw_text_with_background(
                frame, f"Peak Vel: {peak_vel:.2f} m/s",
                (20, y_offset), color=self.CYAN
            )

            y_offset += 35
            rom = summary.get('avg_rom_m', 0)
            self.draw_text_with_background(
                frame, f"ROM: {rom*100:.1f} cm",
                (20, y_offset), color=self.GREEN
            )

            # Top-Right: Form Score (calculated dynamically)
            if form_score is not None:
                # Color based on score
                if form_score >= 8.0:
                    score_color = self.GREEN
                elif form_score >= 6.0:
                    score_color = self.ORANGE
                else:
                    score_color = (0, 0, 255)  # Red in BGR

                score_text = f"Form: {form_score}/10"
                score_x = width - 200
                self.draw_text_with_background(
                    frame, score_text, (score_x, 40),
                    font_scale=0.7, color=score_color
                )

                # Draw feedback below score
                if form_feedback:
                    for i, fb in enumerate(form_feedback[:2]):  # Max 2 feedback lines
                        self.draw_text_with_background(
                            frame, fb[:30],  # Truncate long messages
                            (score_x - 50, 80 + i * 30),
                            font_scale=0.5, color=self.WHITE
                        )

    def draw_enhanced_pose(self, frame: np.ndarray, landmarks,
                          vbt_metrics: Dict = None, current_rep: int = 0) -> None:
        """Draw pose with enhanced Prometheus style"""
        h, w = frame.shape[:2]

        # Convert landmarks to pixel coordinates
        points = {}
        for idx, landmark in enumerate(landmarks.landmark):
            if landmark.visibility > 0.5:
                x = int(landmark.x * w)
                y = int(landmark.y * h)
                points[idx] = (x, y)

        # Draw skeleton connections with glow
        connections = self.mp_pose.POSE_CONNECTIONS
        for connection in connections:
            start_idx, end_idx = connection

            if start_idx in points and end_idx in points:
                p1 = points[start_idx]
                p2 = points[end_idx]

                # Determine thickness and color based on connection importance
                is_important = (start_idx in self.KEY_JOINTS.values() and
                               end_idx in self.KEY_JOINTS.values())

                thickness = 8 if is_important else 6
                color = self.ORANGE

                self.draw_glow_line(frame, p1, p2, thickness, color)

        # Draw joints
        for idx, point in points.items():
            # Determine joint type and styling
            if idx in [self.KEY_JOINTS['LEFT_HIP'], self.KEY_JOINTS['RIGHT_HIP']]:
                # Hips - Cyan with pulse
                self.draw_pulsing_circle(frame, point, 20, self.CYAN)
                self.draw_glow_circle(frame, point, 10, self.CYAN, glow_size=8)

            elif idx in [self.KEY_JOINTS['LEFT_KNEE'], self.KEY_JOINTS['RIGHT_KNEE']]:
                # Knees - Green
                self.draw_glow_circle(frame, point, 10, self.GREEN, glow_size=6)

            elif idx in [self.KEY_JOINTS['LEFT_SHOULDER'], self.KEY_JOINTS['RIGHT_SHOULDER']]:
                # Shoulders - Orange
                self.draw_glow_circle(frame, point, 10, self.ORANGE, glow_size=6)

            elif idx in [self.KEY_JOINTS['LEFT_WRIST'], self.KEY_JOINTS['RIGHT_WRIST']]:
                # Wrists - Orange
                self.draw_glow_circle(frame, point, 8, self.ORANGE, glow_size=5)

            else:
                # Regular joints - smaller orange
                self.draw_glow_circle(frame, point, 6, self.ORANGE, glow_size=4)

        # Draw metrics overlay
        self.draw_metrics_overlay(frame, vbt_metrics, current_rep)

        self.frame_count += 1

    def process_video(
        self,
        video_path: Path,
        output_dir: Path,
        save_video: bool = True,
        save_pose_data: bool = True,
        exercise_type: str = "general"
    ) -> Dict:
        """
        Process video with MediaPipe Pose

        Returns:
            Dict with pose_data and output video path
        """
        output_dir.mkdir(exist_ok=True, parents=True)

        cap = cv2.VideoCapture(str(video_path))
        if not cap.isOpened():
            raise ValueError(f"Could not open video: {video_path}")

        # Get video properties
        fps = cap.get(cv2.CAP_PROP_FPS)
        original_width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        original_height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))

        # 🚀 MEMORY OPTIMIZATION: Downscale to max 720p (saves ~50% RAM)
        MAX_HEIGHT = 720
        if original_height > MAX_HEIGHT:
            scale = MAX_HEIGHT / original_height
            width = int(original_width * scale)
            height = MAX_HEIGHT
            print(f"📉 Downscaling video: {original_width}x{original_height} → {width}x{height} (saves RAM)")
        else:
            width = original_width
            height = original_height
            print(f"✅ Video resolution: {width}x{height} (no downscaling needed)")

        # Setup output video
        output_video_path = None
        out = None
        if save_video:
            output_video_path = output_dir / f"{video_path.stem}_analyzed.mp4"
            fourcc = cv2.VideoWriter_fourcc(*'mp4v')
            out = cv2.VideoWriter(str(output_video_path), fourcc, fps, (width, height))

        # Setup pose detection
        pose_data = {
            "frames": [],
            "fps": fps,
            "width": width,
            "height": height,
            "total_frames": total_frames
        }

        frame_idx = 0
        processed_frames = 0
        current_rep = 0

        # Reset frame counter for animations
        self.frame_count = 0

        # 🚀 MEMORY OPTIMIZATION: Frame sampling (process every Nth frame)
        FRAME_SAMPLE_RATE = 2  # Process every 2nd frame (saves 50% RAM & CPU)

        # Store last detected pose to draw on skipped frames (prevents flickering)
        last_landmarks = None

        with self.mp_pose.Pose(
            min_detection_confidence=0.5,
            min_tracking_confidence=0.5,
            model_complexity=1
        ) as pose:
            while cap.isOpened():
                success, image = cap.read()
                if not success:
                    break

                # 🚀 MEMORY OPTIMIZATION: Resize frame if downscaling
                if original_height != height:
                    image = cv2.resize(image, (width, height), interpolation=cv2.INTER_AREA)

                # Convert to RGB for consistent processing
                image_rgb = cv2.cvtColor(image, cv2.COLOR_BGR2RGB)
                image_bgr = cv2.cvtColor(image_rgb, cv2.COLOR_RGB2BGR)

                # 🚀 MEMORY OPTIMIZATION: Skip frames for sampling
                if frame_idx % FRAME_SAMPLE_RATE != 0:
                    # Draw last known pose on skipped frames (prevents flickering)
                    if last_landmarks is not None:
                        self.draw_enhanced_pose(image_bgr, last_landmarks)
                    if out:
                        out.write(image_bgr)
                    frame_idx += 1
                    self.frame_count += 1  # Keep animation in sync
                    continue

                # Process with MediaPipe (image_rgb already converted above)
                image_rgb.flags.writeable = False
                results = pose.process(image_rgb)
                processed_frames += 1

                # Prepare for drawing
                image_rgb.flags.writeable = True

                if results.pose_landmarks:
                    # Store landmarks for skipped frames
                    last_landmarks = results.pose_landmarks
                    # Save landmarks data
                    frame_data = {
                        "frame": frame_idx,
                        "timestamp": frame_idx / fps,
                        "landmarks": []
                    }

                    for idx, landmark in enumerate(results.pose_landmarks.landmark):
                        frame_data["landmarks"].append({
                            "id": idx,
                            "name": self.mp_pose.PoseLandmark(idx).name,
                            "x": landmark.x,
                            "y": landmark.y,
                            "z": landmark.z,
                            "visibility": landmark.visibility
                        })

                    pose_data["frames"].append(frame_data)

                    # Draw enhanced pose overlay on video frame
                    self.draw_enhanced_pose(image_bgr, results.pose_landmarks)

                # Write frame to output video
                if out:
                    out.write(image_bgr)

                frame_idx += 1

        cap.release()
        if out:
            out.release()

        # 🚀 MEMORY OPTIMIZATION: Explicit garbage collection
        gc.collect()
        print(f"🧹 Memory cleanup completed (processed {processed_frames}/{total_frames} frames)")

        # Save pose data to JSON
        pose_json_path = None
        if save_pose_data:
            pose_json_path = output_dir / f"{video_path.stem}_pose.json"
            with open(pose_json_path, 'w') as f:
                json.dump(pose_data, f, indent=2)

        # Calculate movement velocity metrics with honest calibration system
        print(f"\n{'='*60}\n📊 MOVEMENT VELOCITY ANALYSIS\n{'='*60}")

        # Initialize calibration manager (Tier 3: Relative mode for now)
        calibration_mgr = CalibrationManager(verbose=True)
        calibration_mgr.detect_calibration_method(frame=None)  # No depth data yet

        # Calculate velocity metrics
        velocity_calc = MovementVelocityCalculator(calibration_mgr, fps=fps, verbose=True)
        velocity_metrics = velocity_calc.calculate_movement_metrics(pose_data, exercise_type)

        print(f"{'='*60}\n")

        return {
            "pose_data": pose_data,
            "output_video": output_video_path,
            "pose_json": pose_json_path,
            "frames_processed": frame_idx,
            "velocity_metrics": velocity_metrics
        }
