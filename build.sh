#!/usr/bin/env bash
# Butuh: JDK 8+ dan Android SDK build-tools (d8). Hasil: appvolume-v1.0.0.zip
set -euo pipefail
cd "$(dirname "$0")"
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
D8="${D8:-$(ls -d "$SDK"/build-tools/*/d8 2>/dev/null | sort -V | tail -1)}"
[ -x "${D8:-}" ] || { echo "d8 tidak ditemukan. Set ANDROID_HOME atau D8=/path/ke/d8"; exit 1; }
LIB=$(ls -d "$SDK"/platforms/android-*/android.jar 2>/dev/null | sort -V | tail -1 || true)
rm -rf build && mkdir -p build/classes
javac -source 8 -target 8 -d build/classes java/com/appvol/Main.java
"$D8" --release --min-api 33 ${LIB:+--lib "$LIB"} --output build build/classes/com/appvol/*.class
cp build/classes.dex module/appvol.dex
rm -f appvolume-v1.0.0.zip
(cd module && zip -r ../appvolume-v1.0.0.zip . -x '.*')
echo "Selesai: appvolume-v1.0.0.zip"
