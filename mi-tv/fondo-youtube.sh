#!/bin/bash
# Fondos del top shelf (continue_watching_static) desde mi-tv/fondo-<app>.html, en WebP (brew install webp).
#   mi-tv/fondo-youtube.sh            -> shelf_youtube.webp
#   mi-tv/fondo-youtube.sh plex       -> shelf_plex.webp
set -euo pipefail
cd "$(dirname "$0")/.."
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
APP="${1:-youtube}"
T=$(mktemp -d)
"$CHROME" --headless --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --window-size=1920,1080 \
  --virtual-time-budget=4000 --screenshot="$T/shelf_$APP.png" "file://$PWD/mi-tv/fondo-$APP.html" 2>/dev/null
cwebp -quiet -q 88 -m 6 "$T/shelf_$APP.png" -o launcher/app/src/main/res/drawable-nodpi/shelf_$APP.webp
rm -rf "$T"
