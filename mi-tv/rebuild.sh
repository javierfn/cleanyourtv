#!/usr/bin/env bash
# Rebuild and reinstall this setup on the TV (CleanYourTV launcher + the custom bits in mi-tv/).
# Run after updating the repo (git pull) or whenever the launcher has to be recompiled.
#   mi-tv/rebuild.sh
set -euo pipefail
cd "$(dirname "$0")/.."
export HOME_KEY_SERVICE=1  # needed for "Reiniciar…" in the settings menu
export TV_HOST="${TV_HOST:?set TV_HOST to the TV IP address, e.g. TV_HOST=192.168.1.50 mi-tv/rebuild.sh}"
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
adb connect "$TV_HOST:5555" >/dev/null || true

# 0. The wallpapers are not in git: fetch them once (Wikimedia Commons, credits in the theme folder).
ls mi-tv/theme-glass-oscuro/wallpapers/*.jpg >/dev/null 2>&1 || mi-tv/fotos.sh

# 1. Custom Live TV banner, bundled into the launcher (themes can only name bundled banner_* drawables).
mkdir -p launcher/app/src/main/res/drawable-nodpi
cp mi-tv/banner_tvenvivo.png launcher/app/src/main/res/drawable-nodpi/banner_tvenvivo.png

# 1a. Build folders outside the synced Desktop: its sync makes "X 2.xml" copies that break the build.
C="$HOME/Library/Caches/cleanyourtv-build"
for d in launcher/app/build launcher/build launcher/.gradle addons/weather/app/build addons/weather/.gradle addons/brightness/app/build addons/brightness/.gradle \
         addons/bluetooth-connect/app/build addons/bluetooth-connect/.gradle mi-tv/hdmi-appletv/app/build mi-tv/hdmi-appletv/.gradle; do
  [ -L "$d" ] && continue
  rm -rf "$d"; mkdir -p "$C/$d"; ln -s "$C/$d" "$d"
done

# 1b. Small launcher fixes of ours (caption without the shadow box). Skipped if already applied or upstream changed.
shopt -s nullglob   # this branch carries the changes already; patches are only for a fresh release
for patch in mi-tv/*.patch; do
  if git apply --check "$patch" 2>/dev/null; then git apply "$patch"; echo "aplicado $patch"
  elif git apply --check -R "$patch" 2>/dev/null; then echo "ya aplicado $patch"
  else echo "AVISO: $patch no encaja con esta versión del launcher, revisar" >&2; fi
done

# 2. Launcher: build, install, take Home.
(cd launcher && ./build-and-install.sh)

# 3. Apple TV shortcut app (switches to HDMI 1).
(cd mi-tv/hdmi-appletv && echo "sdk.dir=$ANDROID_HOME" > local.properties && ./gradlew assembleRelease -q --console=plain)
adb -s "$TV_HOST:5555" install -r mi-tv/hdmi-appletv/app/build/outputs/apk/release/app-release.apk

# 3b. Weather add-on (module card), Spanish, 3 days + humidity + wind + °C (addon-weather-options.patch). The city is chosen on the TV.
(cd addons/weather && echo "sdk.dir=$ANDROID_HOME" > local.properties && ./gradlew assembleRelease -q --console=plain)
adb -s "$TV_HOST:5555" install -r addons/weather/app/build/outputs/apk/release/app-release.apk

# 3c. Bluetooth Connect add-on (connect/disconnect paired speakers and headphones).
addons/bluetooth-connect/install.sh

# 3d. Brightness add-on: night mode 21:00-08:00 at 60 %, warmth 30 %, day undimmed; its toggle flips day/night
#     until the next schedule change (addon-brightness.patch). The launcher's moon button (night_button) calls it.
addons/brightness/install.sh

# 4. Theme: dock order, labels, banners, wallpaper, hidden apps, 6 columns...
tools/theme.sh "mi-tv/${TEMA:-theme-glass-oscuro}"   # otro tema: TEMA=theme-halloween mi-tv/rebuild.sh
echo "Listo. Pulsa Inicio en el mando."
