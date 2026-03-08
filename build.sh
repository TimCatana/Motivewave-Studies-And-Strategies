#!/bin/bash
# MotiveWave Custom Indicators - Build Script
# Compiles each study and strategy into separate JAR files for individual deployment

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
BIN_DIR="$SCRIPT_DIR/bin"
LIB_DIR="$SCRIPT_DIR/lib"
TEMP_DIR="$BIN_DIR/.build_temp"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}MotiveWave Custom Studies & Strategies Build${NC}"
echo "=============================================="

# Create output and temp directories
mkdir -p "$BIN_DIR"
mkdir -p "$TEMP_DIR"

# Function to build a single indicator/strategy
build_indicator() {
    local java_file="$1"
    local class_name="${java_file##*/}"
    class_name="${class_name%.java}"
    local classes_dir="$TEMP_DIR/$class_name"
    local jar_file="$BIN_DIR/$class_name.jar"
    
    mkdir -p "$classes_dir"
    
    echo -e "${BLUE}Compiling $class_name...${NC}"
    javac -d "$classes_dir" \
        -cp "$LIB_DIR/mwave_sdk.jar" \
        -encoding UTF-8 \
        -source 11 -target 11 \
        "$java_file" 2>/dev/null
    
    echo -e "${BLUE}Creating $class_name.jar...${NC}"
    jar cvf "$jar_file" -C "$classes_dir" . > /dev/null 2>&1
    
    echo -e "${GREEN}✓ $class_name.jar created${NC}"
}

# Build all studies
echo ""
echo -e "${YELLOW}Building Studies:${NC}"
for java_file in "$SRC_DIR"/custom_studies/*.java; do
    if [ -f "$java_file" ]; then
        build_indicator "$java_file"
    fi
done

# Build all strategies
echo ""
echo -e "${YELLOW}Building Strategies:${NC}"
for java_file in "$SRC_DIR"/custom_strategies/*.java; do
    if [ -f "$java_file" ]; then
        build_indicator "$java_file"
    fi
done

# Clean up temp directory
rm -rf "$TEMP_DIR"

# List all JAR files
echo ""
echo -e "${GREEN}Build Complete!${NC}"
echo -e "${YELLOW}Output JAR files:${NC}"
ls -1 "$BIN_DIR"/*.jar 2>/dev/null | xargs -n1 basename | sed 's/^/  ✓ /'

echo ""
echo "Deploy: Copy any .jar file to MotiveWave's indicators directory"


