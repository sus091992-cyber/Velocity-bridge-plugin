#!/bin/bash

set -e

echo "🚀 Starting AuthBridge Build Process..."

# تنظیمات
PROJECT_DIR="$HOME/Velocity-bridge-plugin"
BUILD_DIR="$PROJECT_DIR/target"

# Step 1: Check if directory exists
if [ ! -d "$PROJECT_DIR" ]; then
    echo "❌ Project directory not found: $PROJECT_DIR"
    exit 1
fi

cd "$PROJECT_DIR"
echo "✓ Working directory: $(pwd)"

# Step 2: Clean previous builds
echo "🗑️  Cleaning previous builds..."
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/classes"

# Step 3: Create directories
echo "📁 Creating directory structure..."
mkdir -p src/main/java/com/niongroq/authbridge/{commands,listeners,managers,utils}

# Step 4: Compile
echo "🔨 Compiling Java files..."
javac -d "$BUILD_DIR/classes" \
    -cp ".:libs/*" \
    $(find src/main/java -name "*.java" 2>/dev/null || echo "")

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful!"
else
    echo "⚠️  Compilation had warnings or notes"
fi

# Step 5: Copy resources
echo "📋 Copying resources..."
cp -r src/main/resources/* "$BUILD_DIR/classes/" 2>/dev/null || true

# Step 6: Create JAR
echo "📦 Creating JAR file..."
jar cf "$BUILD_DIR/authbridge-3.0.0.jar" -C "$BUILD_DIR/classes" .

# Step 7: Verify
if [ -f "$BUILD_DIR/authbridge-3.0.0.jar" ]; then
    echo "✅ BUILD SUCCESSFUL!"
    echo "📍 JAR Location: $BUILD_DIR/authbridge-3.0.0.jar"
    ls -lh "$BUILD_DIR/authbridge-3.0.0.jar"
else
    echo "❌ BUILD FAILED!"
    exit 1
fi
