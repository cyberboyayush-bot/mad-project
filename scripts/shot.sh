#!/usr/bin/env bash
# Captures a PNG screenshot from the running emulator: scripts/shot.sh <output.png>
set -euo pipefail
adb="${ANDROID_HOME:-/opt/android-sdk}/platform-tools/adb"
"$adb" exec-out screencap -p > "${1:?usage: shot.sh <output.png>}"
echo "$1"
