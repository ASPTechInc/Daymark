#!/usr/bin/env bash

# This script is used to run an Android application on a connected device or an emulator.
# To make the script executable, run: $ chmod +x tools/run-android.sh

set -e

echo "Package: $APP_PACKAGE_NAME"

if adb devices | awk '$2=="device" && $1 !~ /^emulator-/' | grep -q .; then
    echo "Using connected Android device."
else
    echo "No physical device detected."

    if adb devices | grep -q "^emulator-.*device"; then
        echo "Using existing emulator."
    else
        echo "Starting emulator: $EMULATOR_NAME"
        "$EMULATOR_COMMAND" -avd "$EMULATOR_NAME" -no-snapshot &

        echo "Waiting for emulator..."

        while ! adb devices | grep -q "^emulator-.*device"; do
            sleep 5
        done

        while [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" != "1" ]; do
            echo "Waiting for Android boot..."
            sleep 5
        done

        echo "Emulator ready."
    fi
fi

echo "Installing application..."
./gradlew installDebug

echo "Launching application..."
adb shell am start -n "$APP_PACKAGE_NAME/$MAIN_ACTIVITY"