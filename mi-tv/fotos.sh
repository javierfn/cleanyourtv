#!/usr/bin/env bash
# Downloads the theme's wallpapers from Wikimedia Commons (credits in theme-glass-oscuro/wallpapers/CREDITOS.txt)
# at 3840x2160, and copies the Halloween theme's from themes/halloween. The photos are not in this branch: each keeps
# its own licence. Needs curl and macOS's sips.
#   mi-tv/fotos.sh
set -euo pipefail
cd "$(dirname "$0")"
W=theme-glass-oscuro/wallpapers
TMP=$(mktemp -d)
get() { t="$1"; out="$2"
  [ -f "$W/$out.jpg" ] && return
  u=$(curl -s -A "cleanyourtv-wallpapers" -G "https://commons.wikimedia.org/w/api.php" --data-urlencode "titles=$t" \
      -d action=query -d prop=imageinfo -d iiprop=url -d format=json \
      | python3 -c "import sys,json;d=json.load(sys.stdin);print(list(d['query']['pages'].values())[0]['imageinfo'][0]['url'])")
  curl -s -A "cleanyourtv-wallpapers" -o "$TMP/big.jpg" "$u"
  w=$(sips -g pixelWidth "$TMP/big.jpg" | awk '/pixelWidth/{print $2}'); h=$(sips -g pixelHeight "$TMP/big.jpg" | awk '/pixelHeight/{print $2}')
  if [ $((w*9)) -gt $((h*16)) ]; then sips --resampleHeight 2160 "$TMP/big.jpg" --out "$TMP/a.jpg" >/dev/null
  else sips --resampleWidth 3840 "$TMP/big.jpg" --out "$TMP/a.jpg" >/dev/null; fi
  sips -c 2160 3840 "$TMP/a.jpg" --out "$TMP/b.jpg" >/dev/null
  sips -s format jpeg -s formatOptions 92 "$TMP/b.jpg" --out "$W/$out.jpg" >/dev/null
  echo "$out"
}
get "File:View of Lysefjord from Preikestolen.jpg" 01-lysefjord
get "File:Kirkjufell, Iceland, 20240714 1631 0713.jpg" 02-kirkjufell
get "File:Chola Mountain Lake, Nepal.jpg" 03-himalaya
get "File:Sossusvlei, Namibia, 2018-08-06, DD 040.jpg" 04-sossusvlei
get "File:La chapelle Notre-Dame-de-la-Garde et les falaises d'Etretat.jpg" 05-etretat
get "File:Lofoten, Norway (Unsplash).jpg" n1-lofoten
get "File:018 Human looking at the stars during Perseids with the Milky Way in the background Photo by Giles Laurent.jpg" n2-via-lactea
get "File:Paris Night.jpg" n3-paris
get "File:Hong Kong Harbour Night 2019-06-11.jpg" n4-hong-kong
get "File:Santa Maria della Salute from Hotel Monaco nightview.jpg" n5-venecia
[ -f theme-glass-oscuro/wallpaper.jpg ] || cp "$W/01-lysefjord.jpg" theme-glass-oscuro/wallpaper.jpg
cp -n ../themes/halloween/wallpapers/*.jpg theme-halloween/wallpapers/ 2>/dev/null || true
rm -rf "$TMP"
