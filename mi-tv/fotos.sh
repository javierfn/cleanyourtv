#!/usr/bin/env bash
# Downloads the theme's wallpapers from Wikimedia Commons (credits in theme-glass-oscuro/wallpapers/CREDITOS.txt)
# at 3840x2160, and copies the Halloween theme's from themes/halloween. The photos are not in this branch: each keeps
# its own licence. Needs curl and macOS's sips.
#   mi-tv/fotos.sh
set -euo pipefail
cd "$(dirname "$0")"
W=theme-glass-oscuro/wallpapers
# Wikimedia answers 429 to generic user agents: name the project (https://meta.wikimedia.org/wiki/User-Agent_policy)
UA="cleanyourtv-wallpapers/1.0 (https://github.com/javierfn/cleanyourtv)"
TMP=$(mktemp -d)
get() { t="$1"; out="$2"; top="${3:-}"
  [ -f "$W/$out.jpg" ] && return
  u=$(curl -s -A "$UA" -G "https://commons.wikimedia.org/w/api.php" --data-urlencode "titles=$t" \
      -d action=query -d prop=imageinfo -d iiprop=url -d format=json \
      | python3 -c "import sys,json;d=json.load(sys.stdin);print(list(d['query']['pages'].values())[0]['imageinfo'][0]['url'])")
  curl -s -A "$UA" -o "$TMP/big.jpg" "$u"
  w=$(sips -g pixelWidth "$TMP/big.jpg" | awk '/pixelWidth/{print $2}'); h=$(sips -g pixelHeight "$TMP/big.jpg" | awk '/pixelHeight/{print $2}')
  if [ $((w*9)) -gt $((h*16)) ]; then sips --resampleHeight 2160 "$TMP/big.jpg" --out "$TMP/a.jpg" >/dev/null
  else sips --resampleWidth 3840 "$TMP/big.jpg" --out "$TMP/a.jpg" >/dev/null; fi
  # The crop is centred; a third argument moves it to that many pixels from the top (a tall subject).
  if [ -n "$top" ]; then sips -c 2160 3840 --cropOffset "$top" 0 "$TMP/a.jpg" --out "$TMP/b.jpg" >/dev/null
  else sips -c 2160 3840 "$TMP/a.jpg" --out "$TMP/b.jpg" >/dev/null; fi
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
# Spain and Portugal, by day and by night
get "File:Granada - View from Mirador de San Nicolás - 02.jpg" 06-alhambra
get "File:Praia de Augas Santas ou das Catedrais. Devesa. Ribadeo. Galiza-20.jpg" 07-catedrales
get "File:Gaztelugache, Bermeo, País Vasco, España, 2019-08-13, DD 14.jpg" 08-gaztelugatxe
get "File:Ronda - View from Puente Nuevo - 5.jpg" 09-ronda
get "File:Plaza de España (Sevilla) - 01.jpg" 10-plaza-espana
get "File:Teide Canadas.jpg" 11-teide
get "File:Parque nacional de Ordesa y Monte Perdido, Huesca, España, 2015-01-07, DD 01.JPG" 12-ordesa
get "File:Montserrat Natural Park 12.jpg" 13-montserrat
get "File:Centro Histórico, 16.9 -- 2023 -- Córdoba, España.jpg" 14-cordoba
get "File:Toledo - 02.jpg" 15-toledo
get "File:2010-Catedral de Santiago de Compostela-Galicia (Spain) 3.jpg" 16-santiago
get "File:Palacio Nacional da Pena, Sintra, Portugal, 2019-05-25, DD 84.jpg" 17-sintra
get "File:Rabelo boats and Ribeira seen from Cais de Gaia, 20250605 1623 9879.jpg" 18-oporto
get "File:Lago verde, isla de San Miguel, Azores, Portugal, 2020-07-29, DD 16.jpg" 19-azores
get "File:Dusk Charles V Palace Alhambra Granada Andalusia Spain.jpg" n6-alhambra
get "File:Casas colgadas, Cuenca, España, 2017-01-03, DD 121-123 HDR.jpg" n7-cuenca
get "File:Roman Aqueduct Segovia night 2012 Spain.jpg" n8-segovia
get "File:Sevilla - Torre del Oro and river Guadalquivir at night.jpg" n9-sevilla
get "File:El Hemisférico, Ciudad de las Artes y las Ciencias, Valencia, España, 2014-06-29, DD 60-62 HDR.JPG" n10-valencia
get "File:Puente de Don Luis I, Oporto, Portugal, 2019-06-02, DD 29-31 HDR.jpg" n11-oporto
get "File:25 de Abril Bridge (36211658443).jpg" n12-lisboa
get "File:Skyline view of Madrid, Spain at night - 50317258357.jpg" n13-madrid
get "File:Sagrada Familia at night 01.jpg" n14-sagrada-familia 33
get "File:Road to the stars.jpg" n15-teide
get "File:1 toledo spain evening sunset 2014 DXR edit.jpg" n16-toledo
# From space: NASA photos from the International Space Station (public domain), night only
get "File:ISS-43 Earth sunrise, aurora and sparling cities in northern Europe.jpg" n17-aurora-norte-europa
get "File:ISS-43 Southern Scandinavia.jpg" n18-escandinavia
get "File:The aurora borealis blankets the Earth (iss072e159172).jpg" n19-aurora-boreal 390
get "File:Sunset from the ISS.JPG" n20-atardecer-orbital 330
get "File:Egypt’s Nile River stretches from Luxor to Cairo (iss073e0422271).jpg" n21-nilo
get "File:ISS-39 Moscow, Aurora borealis and Moon.jpg" n22-moscu-aurora-luna
get "File:A reddish airglow blankets Earth beneath the Milky Way (iss073e0982696).jpg" n23-luminiscencia
get "File:ISS-62 Aurora australis over the Indian Ocean.jpg" n24-aurora-austral
get "File:ISS-62 City lights of northeastern Europe.jpg" n25-europa-nordeste 390
get "File:ISS-46 Aurora over nocturnal Canada.jpg" n26-aurora-canada
[ -f theme-glass-oscuro/wallpaper.jpg ] || cp "$W/01-lysefjord.jpg" theme-glass-oscuro/wallpaper.jpg
cp -n ../themes/halloween/wallpapers/*.jpg theme-halloween/wallpapers/ 2>/dev/null || true
rm -rf "$TMP"
