#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR/android"

echo "Building debug APK..."
./gradlew :app:assembleDebug

APK="$SCRIPT_DIR/android/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "$APK" ]]; then
    echo "ERROR: APK not found at $APK" >&2
    exit 1
fi

echo "Installing on connected device..."
adb install -r "$APK"

echo "Done. App installed successfully."
