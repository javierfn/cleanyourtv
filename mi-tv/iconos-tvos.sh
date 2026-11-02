#!/bin/bash
# Render the tvOS-style icons from iconos-tvos.html into the launcher and the weather add-on (PNG, 1.5x of 320x180 dp).
set -euo pipefail
cd "$(dirname "$0")/.."
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
SRC="file://$PWD/mi-tv/iconos-tvos.html"
shot() { "$CHROME" --headless --disable-gpu --hide-scrollbars --force-device-scale-factor="$4" --window-size="$2" \
           --screenshot="$3" "$SRC#$1" 2>/dev/null; }
shot ajustes 640,360 launcher/app/src/main/res/drawable-nodpi/banner_ajustes.png 1.5
shot envivo 640,360 launcher/app/src/main/res/drawable-nodpi/banner_envivo.png 1.5
shot tiempo 640,360 addons/weather/app/src/main/res/drawable-nodpi/banner.png 1.5
shot tiempo-icono 432,432 addons/weather/app/src/main/res/drawable-nodpi/icon.png 1
