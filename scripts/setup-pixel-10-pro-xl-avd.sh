#!/usr/bin/env bash
set -euo pipefail

sdk_root="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
sdk_manager="$sdk_root/cmdline-tools/latest/bin/sdkmanager"
avd_manager="$sdk_root/cmdline-tools/latest/bin/avdmanager"
avd_name="WhiteNoise_Pixel_10_Pro_XL_API_36"
system_image="system-images;android-36;google_apis;arm64-v8a"

if [[ ! -x "$sdk_manager" || ! -x "$avd_manager" ]]; then
  echo "Android command-line tools were not found under $sdk_root." >&2
  exit 1
fi

image_dir="$sdk_root/system-images/android-36/google_apis/arm64-v8a"
if [[ -f "$image_dir/package.xml" && ! -f "$image_dir/system.img" ]]; then
  echo "Removing an incomplete API 36 system-image installation."
  "$sdk_manager" --uninstall "$system_image"
fi

if [[ ! -f "$image_dir/system.img" ]]; then
  set +o pipefail
  yes | "$sdk_manager" "$system_image"
  sdk_status="${PIPESTATUS[1]}"
  set -o pipefail
  if [[ "$sdk_status" -ne 0 ]]; then
    exit "$sdk_status"
  fi
fi

if [[ ! -f "$image_dir/system.img" ]]; then
  echo "The API 36 system image did not install completely." >&2
  exit 1
fi

if "$sdk_root/emulator/emulator" -list-avds | grep -Fxq "$avd_name"; then
  echo "$avd_name already exists."
  exit 0
fi
echo "no" | "$avd_manager" create avd \
  --force \
  --name "$avd_name" \
  --package "$system_image" \
  --device "pixel_10_pro_xl"

echo "Created $avd_name."
echo "Launch it with:"
echo "$sdk_root/emulator/emulator -avd $avd_name"
