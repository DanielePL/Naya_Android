#!/bin/bash
# Copy OpenGL libraries for opencv-python-headless (installed by mediapipe)

set -e

echo "🔍 Searching for OpenGL libraries in Nix store..."

# Create lib directory in app (persists to runtime)
mkdir -p /app/lib

# Find and copy libGL.so.1
if GL_LIB=$(find /nix/store -name 'libGL.so.1' -type f 2>/dev/null | head -1); then
    echo "✓ Found libGL.so.1: $GL_LIB"
    cp -L "$GL_LIB" /app/lib/
else
    echo "⚠️  libGL.so.1 not found"
fi

# Find and copy libGLdispatch.so.0
if GLDISPATCH=$(find /nix/store -name 'libGLdispatch.so.0' -type f 2>/dev/null | head -1); then
    echo "✓ Found libGLdispatch.so.0: $GLDISPATCH"
    cp -L "$GLDISPATCH" /app/lib/
else
    echo "⚠️  libGLdispatch.so.0 not found"
fi

# Find and copy libGLX.so.0
if GLX=$(find /nix/store -name 'libGLX.so.0' -type f 2>/dev/null | head -1); then
    echo "✓ Found libGLX.so.0: $GLX"
    cp -L "$GLX" /app/lib/
else
    echo "⚠️  libGLX.so.0 not found"
fi

# Find and copy libglib
if GLIB=$(find /nix/store -name 'libglib-2.0.so.0' -type f 2>/dev/null | head -1); then
    echo "✓ Found libglib-2.0.so.0: $GLIB"
    cp -L "$GLIB" /app/lib/
else
    echo "⚠️  libglib-2.0.so.0 not found"
fi

# Find and copy additional dependencies that might be needed
for lib in libX11.so.6 libXext.so.6 libXrender.so.1 libSM.so.6 libICE.so.6; do
    if LIBPATH=$(find /nix/store -name "$lib" -type f 2>/dev/null | head -1); then
        echo "✓ Found $lib: $LIBPATH"
        cp -L "$LIBPATH" /app/lib/ || true
    fi
done

echo "📦 Copied libraries to /app/lib:"
ls -lh /app/lib/
