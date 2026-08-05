#!/usr/bin/env bash
set -euo pipefail

if ! command -v adb >/dev/null 2>&1; then
  sdk_root="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
  export PATH="$sdk_root/platform-tools:$PATH"
fi

if ! command -v adb >/dev/null 2>&1; then
  echo "adb was not found." >&2
  exit 1
fi

device_count="$(adb devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
if [[ "$device_count" -ne 1 ]]; then
  echo "Expected exactly one connected Android device, found $device_count." >&2
  exit 1
fi

adb shell settings put system time_12_24 24
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
sleep 0.1
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1815 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile hide >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null

echo "Emulator System UI clock locked at 18:15."
