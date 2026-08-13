#!/usr/bin/env bash

set -eEuo pipefail

BUILD_DIR=build
CLASS_DIR=$BUILD_DIR/classes
SOURCE_LIST=$BUILD_DIR/sources.txt

rm -rf $BUILD_DIR
mkdir -p $CLASS_DIR

find src -name "*.java" > "$SOURCE_LIST"

echo "Building..."
javac -Xlint:deprecation -d $CLASS_DIR @$SOURCE_LIST
echo "Build complete!"
