#!/bin/bash

# Lego Studio 3D - Build and Run Script
# Usage: ./run.sh [build|run|clean]

set -e

cd "$(dirname "$0")"

case "${1:-run}" in
    build)
        echo "Building Lego Studio..."
        mvn clean compile -q
        echo "Build complete!"
        ;;
    run)
        echo "Building and running Lego Studio..."
        mvn clean compile exec:exec -q
        ;;
    package)
        echo "Packaging Lego Studio..."
        mvn clean package -q
        echo "JAR created in target/"
        ;;
    clean)
        echo "Cleaning build files..."
        mvn clean -q
        echo "Clean complete!"
        ;;
    *)
        echo "Usage: ./run.sh [build|run|package|clean]"
        echo "  build   - Compile the project"
        echo "  run     - Build and run (default)"
        echo "  package - Create executable JAR"
        echo "  clean   - Remove build files"
        exit 1
        ;;
esac
