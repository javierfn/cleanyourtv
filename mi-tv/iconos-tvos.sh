#!/bin/bash
# Render the tvOS-style icons from iconos-tvos.html into the launcher and the weather add-on (1.5x of 320x180 dp).
# The launcher's two go in as WebP (cwebp -q 92, brew install webp): as PNG they made the APK 600 KB bigger.
set -euo pipefail
cd "$(dirname "$0")/.."
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
SRC="file://$PWD/mi-tv/iconos-tvos.html"
shot() { "$CHROME" --headless --disable-gpu --hide-scrollbars --force-device-scale-factor="$4" --window-size="$2" \
           --screenshot="$3" "$SRC#$1" 2>/dev/null; }
L=launcher/app/src/main/res/drawable-nodpi; T=$(mktemp -d)
for n in ajustes envivo; do
  shot $n 640,360 "$T/banner_$n.png" 1.5
  cwebp -quiet -q 92 -m 6 -sharp_yuv "$T/banner_$n.png" -o "$L/banner_$n.webp" && rm -f "$L/banner_$n.png"
done
rm -rf "$T"
shot tiempo 640,360 addons/weather/app/src/main/res/drawable-nodpi/banner.png 1.5
shot tiempo-icono 432,432 addons/weather/app/src/main/res/drawable-nodpi/icon.png 1
