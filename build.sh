#!/bin/bash
# MotiveWave Custom Indicators - Build Script
# This script compiles all studies and strategies and packages them into a JAR file

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SRC_DIR="$SCRIPT_DIR/src"
BIN_DIR="$SCRIPT_DIR/bin"
CLASSES_DIR="$BIN_DIR/classes"
LIB_DIR="$SCRIPT_DIR/lib"
JAR_FILE="$BIN_DIR/motivewave-indicators.jar"

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}MotiveWave Custom Studies & Strategies Build${NC}"
echo "=============================================="

# Create output directories
mkdir -p "$CLASSES_DIR"

# Compile all studies and strategies
echo -e "${BLUE}Compiling custom studies...${NC}"
javac -d "$CLASSES_DIR" \
    -cp "$LIB_DIR/mwave_sdk.jar" \
    -encoding UTF-8 \
    -source 11 -target 11 \
    "$SRC_DIR"/custom_studies/*.java

echo -e "${GREEN}✓ Studies compiled${NC}"

echo -e "${BLUE}Compiling custom strategies...${NC}"
javac -d "$CLASSES_DIR" \
    -cp "$LIB_DIR/mwave_sdk.jar:$CLASSES_DIR" \
    -encoding UTF-8 \
    -source 11 -target 11 \
    "$SRC_DIR"/custom_strategies/*.java

echo -e "${GREEN}✓ Strategies compiled${NC}"

# Package into JAR
echo -e "${BLUE}Creating JAR package...${NC}"
jar cvf "$JAR_FILE" -C "$CLASSES_DIR" . > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ JAR created successfully: $JAR_FILE${NC}"
else
    echo "✗ JAR creation failed"
    exit 1
fi

echo ""
echo -e "${GREEN}Build Complete!${NC}"
echo "JAR file: $JAR_FILE"
echo ""
echo "Included:"
ls -1 "$CLASSES_DIR"/custom_studies/*.class 2>/dev/null | xargs -n1 basename | sed 's/\.class$//' | sed 's/^/  • /'
ls -1 "$CLASSES_DIR"/custom_strategies/*.class 2>/dev/null | xargs -n1 basename | sed 's/\.class$//' | sed 's/^/  • /'

