#!/bin/bash

set -e

echo "🚀 AuthBridge Build Process with Dependencies"
echo "============================================="

# تنظیمات
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$PROJECT_DIR/target"
CLASSES_DIR="$BUILD_DIR/classes"
OUTPUT_JAR="$BUILD_DIR/authbridge-3.0.0.jar"

# Step 1: Check dependencies
echo "✓ Checking dependencies..."
if [ ! -d "$PROJECT_DIR/libs" ] || [ -z "$(ls -A $PROJECT_DIR/libs 2>/dev/null)" ]; then
    echo "⚠️  Dependencies not found. Downloading..."
    bash "$PROJECT_DIR/download-deps.sh"
fi

# Step 2: Build classpath
echo "✓ Building classpath..."
CP="$PROJECT_DIR/libs/*"
for jar in $(find "$PROJECT_DIR/libs" -name "*.jar" 2>/dev/null); do
    CP="$CP:$jar"
done

echo "✓ Classpath ready"

# Step 3: Check Java
echo "✓ Checking Java..."
java -version 2>&1 | head -n 1
javac -version 2>&1 | head -n 1

# Step 4: Create directories
echo "✓ Creating directories..."
mkdir -p "$CLASSES_DIR"

# Step 5: Compile Java files
echo "🔨 Compiling Java files..."
cd "$PROJECT_DIR"

javac -d "$CLASSES_DIR" \
    -encoding UTF-8 \
    -cp "$CP" \
    $(find src/main/java -name "*.java" | tr '\n' ' ')

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful!"
else
    echo "❌ Compilation failed!"
    exit 1
fi

# Step 6: Copy resources
echo "📋 Copying resources..."
cp -r src/main/resources/* "$CLASSES_DIR/" 2>/dev/null || true

# Step 7: Create manifest
echo "📝 Creating manifest..."
mkdir -p "$BUILD_DIR/META-INF"
cat > "$BUILD_DIR/META-INF/MANIFEST.MF" << 'EOF'
Manifest-Version: 1.0
Implementation-Title: AuthBridge
Implementation-Version: 3.0.0
Implementation-Vendor: S1MPLE
Created-By: AuthBridge Build Script
EOF

# Step 8: Create JAR
echo "📦 Creating JAR file..."
cd "$CLASSES_DIR"
jar cfm "$OUTPUT_JAR" "$BUILD_DIR/META-INF/MANIFEST.MF" .

# Step 9: Verify
cd "$PROJECT_DIR"
if [ -f "$OUTPUT_JAR" ]; then
    echo ""
    echo "✅ BUILD SUCCESSFUL!"
    echo "📍 Output: $OUTPUT_JAR"
    echo ""
    ls -lh "$OUTPUT_JAR"
    echo ""
    echo "✓ Ready to deploy!"
    echo ""
    echo "Next steps:"
    echo "1. git tag v3.0.0"
    echo "2. git push origin v3.0.0"
    echo "3. Create release on GitHub and upload JAR"
else
    echo "❌ BUILD FAILED - JAR not created"
    exit 1
fi
