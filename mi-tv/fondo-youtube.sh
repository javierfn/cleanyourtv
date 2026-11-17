#!/bin/bash
# Fondo del top shelf de YouTube (continue_watching_static) desde mi-tv/fondo-youtube.html, en WebP (brew install webp).
set -euo pipefail
cd "$(dirname "$0")/.."
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
T=$(mktemp -d)
"$CHROME" --headless --disable-gpu --hide-scrollbars --force-device-scale-factor=1 --window-size=1920,1080 \
  --virtual-time-budget=4000 --screenshot="$T/shelf_youtube.png" "file://$PWD/mi-tv/fondo-youtube.html" 2>/dev/null
cwebp -quiet -q 88 -m 6 "$T/shelf_youtube.png" -o launcher/app/src/main/res/drawable-nodpi/shelf_youtube.webp
rm -rf "$T"
