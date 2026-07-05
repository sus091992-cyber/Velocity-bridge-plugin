#!/bin/bash

# AuthBridge Build Script (No Maven Required)
# This script compiles and packages AuthBridge without Maven

set -e

echo "🚀 AuthBridge Build Process (Standalone)"
echo "=========================================="

# تنظیمات
PROJECT_DIR="$HOME/Velocity-bridge-plugin"
BUILD_DIR="$PROJECT_DIR/target"
CLASSES_DIR="$BUILD_DIR/classes"
OUTPUT_JAR="$BUILD_DIR/authbridge-3.0.0.jar"

# Step 1: Check Java
echo "✓ Checking Java..."
java -version 2>&1 | head -n 1
javac -version 2>&1

# Step 2: Create directories
echo "✓ Creating directories..."
mkdir -p "$CLASSES_DIR"
cd "$PROJECT_DIR"

# Step 3: Compile Java files
echo "🔨 Compiling Java files..."
javac -d "$CLASSES_DIR" \
    -encoding UTF-8 \
    -source 11 -target 11 \
    $(find src/main/java -name "*.java" | tr '\n' ' ')

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi
echo "✓ Compilation successful!"

# Step 4: Copy resources
echo "📋 Copying resources..."
cp -r src/main/resources/* "$CLASSES_DIR/" 2>/dev/null || true

# Step 5: Create manifest
echo "📝 Creating manifest..."
mkdir -p "$BUILD_DIR/META-INF"
cat > "$BUILD_DIR/META-INF/MANIFEST.MF" << 'EOF'
Manifest-Version: 1.0
Implementation-Title: AuthBridge
Implementation-Version: 3.0.0
Implementation-Vendor: S1MPLE
Created-By: AuthBridge Build Script
EOF

# Step 6: Create JAR
echo "📦 Creating JAR file..."
cd "$CLASSES_DIR"
jar cfm "$OUTPUT_JAR" "$BUILD_DIR/META-INF/MANIFEST.MF" .

# Step 7: Verify
cd "$PROJECT_DIR"
if [ -f "$OUTPUT_JAR" ]; then
    echo ""
    echo "✅ BUILD SUCCESSFUL!"
    echo "📍 Output: $OUTPUT_JAR"
    echo ""
    ls -lh "$OUTPUT_JAR"
    echo ""
    echo "✓ Ready to deploy!"
else
    echo "❌ BUILD FAILED - JAR not created"
    exit 1
fi
