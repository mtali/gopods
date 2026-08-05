#!/bin/bash

# Captures whatever is on the connected device right now and writes both the lossless
# png and the optimised jpeg the README and the Play Store listing use.
#
# Navigation is left to you on purpose: driving the app from a script turned out to be
# far more brittle than just opening the screen you want and running this.
#
# Usage:
#   ./scripts/capture_screenshot.sh 01-library
#   ./scripts/capture_screenshot.sh --lock 06-lockscreen
#
# The set the README expects:
#   01-library       subscription grid, with something playing so the mini player shows
#   02-discover      search results
#   03-details       podcast header and episode list, ideally with an episode playing
#   04-now-playing   player, opened from the mini player
#   05-settings      settings
#   06-lockscreen    lock screen transport controls, use --lock
#
# Shots are taken in the dark theme so the listing is consistent. Subscribe to a few
# podcasts first, otherwise the grid is empty.

set -euo pipefail

OUT_DIR="store/screenshots"
JPEG_QUALITY=82
PIN=1234

usage() {
  echo "usage: $0 [--lock] <name>" >&2
  exit 1
}

# One handler for everything, because a second trap on EXIT silently replaces the
# first and the screen lock would be left behind.
TMP=""
LOCK_SET=false
cleanup() {
  [[ -n "$TMP" ]] && rm -rf "$TMP"
  if [[ "$LOCK_SET" == true ]]; then
    echo "🔓 clearing the temporary screen lock"
    adb shell locksettings clear --old "$PIN" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

LOCK=false
if [[ "${1:-}" == "--lock" ]]; then
  LOCK=true
  shift
fi
NAME="${1:-}"
[[ -n "$NAME" ]] || usage

command -v adb >/dev/null || { echo "adb not on PATH" >&2; exit 1; }
command -v sips >/dev/null || { echo "sips not found, this script expects macOS" >&2; exit 1; }
adb get-state >/dev/null 2>&1 || { echo "no device or emulator attached" >&2; exit 1; }

mkdir -p "$OUT_DIR"

if [[ "$LOCK" == true ]]; then
  # Without a screen lock the emulator wakes straight back into the app, so the media
  # controls never appear on a lock screen. Set one, sleep, wake, shoot, then undo it.
  echo "🔒 setting a temporary screen lock"
  adb shell locksettings set-pin "$PIN" >/dev/null
  LOCK_SET=true
  adb shell input keyevent 26
  sleep 3
  adb shell input keyevent 26
  sleep 4
fi

echo "📸 capturing $NAME"
# The png is a working file. Keeping it alongside the jpeg was the same picture
# twice, so it is converted and thrown away.
TMP=$(mktemp -d)
PNG="$TMP/shot.png"
adb shell screencap -p /sdcard/capture.png
adb pull -q /sdcard/capture.png "$PNG" >/dev/null 2>&1 || adb pull /sdcard/capture.png "$PNG" >/dev/null
adb shell rm -f /sdcard/capture.png

sips -s format jpeg -s formatOptions "$JPEG_QUALITY" "$PNG" --out "$OUT_DIR/$NAME.jpg" >/dev/null

SIZE=$(du -h "$OUT_DIR/$NAME.jpg" | cut -f1)
DIMS=$(sips -g pixelWidth -g pixelHeight "$OUT_DIR/$NAME.jpg" \
  | awk '/pixelWidth/ {w=$2} /pixelHeight/ {h=$2} END {print w"x"h}')

echo "✅ $OUT_DIR/$NAME.jpg  $DIMS  $SIZE"

# The captures are the device's native size, which on a modern phone is taller than
# 9:16. If the Play Console objects to the aspect ratio, crop rather than scale so the
# pixels stay sharp:
#   sips -c 1920 1080 store/screenshots/NAME.jpg --out play-NAME.jpg
