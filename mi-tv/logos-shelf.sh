#!/bin/bash
# Logos del top shelf (continue_watching_logo) desde mi-tv/logos-shelf.html, WebP con transparencia (brew install webp).
set -euo pipefail
cd "$(dirname "$0")/.."
CHROME="/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
T=$(mktemp -d)
for app in prime:766 plex:520 youtube:1180 movistar:534; do
  name=${app%%:*}; width=${app##*:}
  "$CHROME" --headless --disable-gpu --hide-scrollbars --default-background-color=00000000 --window-size=$width,240 \
    --virtual-time-budget=4000 --screenshot="$T/logo_$name.png" "file://$PWD/mi-tv/logos-shelf.html#$name" 2>/dev/null
  cwebp -quiet -q 92 -exact -alpha_q 100 "$T/logo_$name.png" -o "launcher/app/src/main/res/drawable-nodpi/shelf_logo_$name.webp"
done
rm -rf "$T"
