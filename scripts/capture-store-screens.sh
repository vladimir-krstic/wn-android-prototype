#!/usr/bin/env bash
set -euo pipefail

project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
output_dir="$project_dir/screenshots/raw"
package_name="dev.ipf.whitenoise.screenshots"
expected_size="1344x2992"

mkdir -p "$output_dir"

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

physical_size="$(adb shell wm size | tr -d '\r' | awk -F': ' '/Physical size/ { print $2 }')"
if [[ "$physical_size" != "$expected_size" ]]; then
  echo "Expected Pixel 10 Pro XL resolution $expected_size, got ${physical_size:-unknown}." >&2
  exit 1
fi

cleanup() {
  adb shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null 2>&1 || true
  sleep 0.2
  "$project_dir/scripts/lock-emulator-clock.sh" >/dev/null 2>&1 || true
}
trap cleanup EXIT

cd "$project_dir"
./gradlew :app:installDebug

adb shell settings put system font_scale 1.0
adb shell settings put system accelerometer_rotation 0
adb shell settings put system user_rotation 0
adb shell settings put system time_12_24 24
adb shell cmd uimode night no

adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1815 >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e mobile hide >/dev/null
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false >/dev/null

capture_scene() {
  local route="$1"
  local filename="$2"
  local destination="$output_dir/$filename"

  adb shell am force-stop "$package_name"
  adb shell am start -W \
    -a android.intent.action.VIEW \
    -d "whitenoise-screenshots://scene/$route" \
    "$package_name" >/dev/null
  sleep 2
  adb exec-out screencap -p > "$destination"

  if command -v sips >/dev/null 2>&1; then
    local width
    local height
    width="$(sips -g pixelWidth "$destination" 2>/dev/null | awk '/pixelWidth/ { print $2 }')"
    height="$(sips -g pixelHeight "$destination" 2>/dev/null | awk '/pixelHeight/ { print $2 }')"
    if [[ "${width}x${height}" != "$expected_size" ]]; then
      echo "Unexpected capture size for $filename: ${width}x${height}" >&2
      exit 1
    fi
  fi

  echo "Captured $destination"
}

capture_scene "relays" "01-relays.png"
capture_scene "profile-switcher" "02-profile-switcher.png"
capture_scene "chats" "03-chats.png"
capture_scene "conversation" "04-conversation.png"
capture_scene "share-connect" "05-share-connect.png"
