"""
Prometheus Backend - Modular FastAPI Application
Refactored from monolithic main.py into separate routers
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pathlib import Path
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Import routers
from .routers import form_analysis, ai_coach, admin, partner

app = FastAPI(
    title="Prometheus Form Analysis API",
    description="2D pose estimation and kinematics analysis for workout videos",
    version="2.0.0"  # Major version bump for modular refactor
)

# CORS middleware for Android app
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Create directories
Path("uploads").mkdir(exist_ok=True)
Path("outputs").mkdir(exist_ok=True)


# ═══════════════════════════════════════════════════════════════════════════════
# ROOT ENDPOINT
# ═══════════════════════════════════════════════════════════════════════════════

@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "service": "Prometheus Form Analysis API",
        "status": "running",
        "version": "2.0.0",
        "modules": ["form_analysis", "ai_coach", "admin", "partner"]
    }


# ═══════════════════════════════════════════════════════════════════════════════
# REGISTER ROUTERS
# ═══════════════════════════════════════════════════════════════════════════════

# Form Analysis Router (MediaPipe Pose)
app.include_router(form_analysis.router)

# AI Coach Router
app.include_router(ai_coach.router)
app.include_router(ai_coach.legacy_router)  # Legacy /ai-coach endpoint

# Admin Dashboard Router
app.include_router(admin.router)

# Partner Portal Router
app.include_router(partner.router)


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)