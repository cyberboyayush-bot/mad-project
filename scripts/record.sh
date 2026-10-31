#!/usr/bin/env bash
# Records the emulator screen while running scripts/demo.py, then stitches the clips together.
# Usage: scripts/record.sh [output.mp4]
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
out="${1:-$root/artifacts/pocketledger-demo.mp4}"
adb="${ANDROID_HOME:-/opt/android-sdk}/platform-tools/adb"
work="$(mktemp -d)"
mkdir -p "$(dirname "$out")"

# screenrecord stops after 3 minutes, so record back-to-back segments until told to stop.
"$adb" shell 'rm -f /sdcard/seg_*.mp4; rm -f /sdcard/stop_rec'
"$adb" shell 'i=0; while [ ! -f /sdcard/stop_rec ]; do screenrecord --bit-rate 8000000 --time-limit 170 /sdcard/seg_$i.mp4; i=$((i+1)); done' &
recorder=$!
sleep 2

status=0
python3 "$root/scripts/demo.py" --shots "$root/artifacts/screenshots" || status=$?

sleep 2
"$adb" shell 'touch /sdcard/stop_rec; pkill -INT screenrecord' || true
wait "$recorder" || true
sleep 1

for seg in $("$adb" shell 'ls /sdcard/seg_*.mp4' | tr -d '\r' | sort -V); do
  "$adb" pull -q "$seg" "$work/"
  echo "file '$work/$(basename "$seg")'" >> "$work/list.txt"
done

ffmpeg -loglevel error -y -f concat -safe 0 -i "$work/list.txt" \
  -vf "scale=720:-2,fps=30" -c:v libx264 -preset slow -crf 22 -pix_fmt yuv420p -movflags +faststart "$out"
rm -rf "$work"
echo "Video: $out"
exit $status
