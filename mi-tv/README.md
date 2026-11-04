# Una TV con estilo Apple TV, sobre Clean Your TV 3.3

Configuración personal de un Panasonic Google TV LCD (kowloon, Vestel/MediaTek, Android 14) con el launcher de
[Clean Your TV](https://github.com/Gathin23/cleanyourtv). Esta rama está basada en Clean Your TV 3.3 (versión de la rama: 3.3-tv.10) con los cambios ya
aplicados en el código; la carpeta `mi-tv/` tiene el tema, los iconos, una mini-app y los scripts.

```bash
TV_HOST=<ip de la TV> mi-tv/rebuild.sh
```

Compila e instala el launcher, la mini-app del HDMI 1 y los complementos del tiempo, Brightness y Bluetooth Connect,
descarga los fondos la primera vez (`fotos.sh`) y aplica el tema. Necesita JDK 17, el SDK de Android 34 y
`tools/venv` (ver el README del proyecto). Para el tema de Halloween: `TEMA=theme-halloween mi-tv/rebuild.sh`.

## Qué cambia en el launcher

- **Menú de ajustes al estilo Apple TV**: la rueda abre un único menú (ajustes del televisor y del lanzador,
  tarjetas, dock, reordenar, apagar), con la fila enfocada en blanco.
- **Movimiento de los fondos** (Ken Burns): acercamientos lentos a puntos al azar, sin desplazamiento lateral;
  se activa o desactiva en Ajustes del lanzador. Los fondos se cargan con margen para el zoom y en color completo.
- **El tiempo bajo el reloj** (`weather_now`) y el **botón de sol y luna** del modo noche, que leen los complementos
  del tiempo y Brightness sin que el launcher se conecte a internet. El botón cambia a día o a noche hasta el
  siguiente cambio de horario; mantener pulsado abre un menú (volver al horario, noche más clara u oscura).
  Con `"weather_now_place": false` en el tema, la línea del tiempo no muestra la ciudad (así está el tema oscuro).
  Desde 3.3-tv.8 la línea muestra también la probabilidad de lluvia o nieve desde el 30 % («☔ 11° · Lluvia · 80 % ·
  Humedad 90 %») y usa los mismos símbolos y columnas que `header_weather` de LostInInaka (contrato en
  `docs/modules.md`).
- **Fondos de España y Portugal**: el tema trae 25 fotos más (Alhambra, Catedrales, Gaztelugatxe, Teide, Sintra,
  Oporto…; de noche Cuenca, Segovia, Lisboa, Gran Vía, Sagrada Família…) mezcladas con las de siempre.
- **Fondos desde el espacio** (3.3-tv.7): 10 fotos nocturnas de la NASA tomadas desde la Estación Espacial
  (auroras, el Nilo, Moscú con la Luna, la Vía Láctea…; dominio público), las del tema `aerial` de Clean Your TV,
  pero a 3840x2160 y solo en el modo noche, mezcladas con las demás.
- **Chromecast en el Centro de control** (`cast_pill`, 3.3-tv.9): una píldora junto a Entradas con «Listo · <nombre
  que ven los móviles>», o «No disponible» en gris si el receptor de Cast no responde; al pulsarla, una tarjeta con
  el nombre, cómo enviar desde el móvil y un botón a los ajustes de Google Cast. Sin permisos nuevos (ver
  `novedades-3.3-tv.9.md`).
- **Ajustes del lanzador estilo tvOS** (`tvos_settings`, 3.3-tv.10): el inicio desenfocado detrás, título centrado,
  el icono de Ajustes fijo a la izquierda con la explicación de la opción enfocada, las opciones agrupadas a la derecha
  con su valor en gris, la fila enfocada en blanco y listas con ✓ en lugar de los diálogos de Android (ver
  `novedades-3.3-tv.10.md`).
- La VPN como píldora con texto, el nombre del fondo sin recuadro y menús que no se activan solos con el OK
  mantenido de los mandos de infrarrojos.

## Carpeta mi-tv

| Ruta | Qué es |
|---|---|
| `theme-glass-oscuro/` | El tema: dock, nombres, iconos, 6 columnas, cristal oscuro, fondos de día y de noche (`wallpaper_moods`): 19 de día y 26 de noche, 25 de ellos de lugares emblemáticos de España y Portugal (ver `novedades-fondos.md`) y 10 nocturnos desde el espacio de la NASA (3.3-tv.7), mezclados y al azar, uno nuevo cada 10 minutos y también al abrir Inicio si ya tocaba (3.3-tv.6). Las fotos no están en git: `fotos.sh` las baja de Wikimedia Commons (créditos en `wallpapers/CREDITOS.txt`) |
| `theme-halloween/` | El mismo tema con los fondos y el disfraz de Halloween de Clean Your TV |
| `banner_tvenvivo.png` / `.html` | Icono de "TV en Vivo" propio (se copia dentro del launcher antes de compilar; el tema oscuro usa ahora `banner_envivo`, ver `iconos-tvos.html`, y el de Halloween, este) |
| `iconos-tvos.html` / `.sh` | Fuente de los iconos estilo tvOS (diseño propio): `banner_envivo.png` (TV en Vivo vía `app_banners`: televisor de cristal con canto de aluminio pulido, pantalla iluminada magenta-azul con botón de reproducir, antena de metal y luz roja de directo con ondas, sobre azul noche), `banner_ajustes.png` (tarjeta Ajustes vía `app_banners`: engranaje de metal pulido con biselado y centro hundido, y un engranaje de cristal esmerilado detrás, sobre grafito con viñeteado) y `banner.png`/`icon.png` del complemento El tiempo (cielo profundo azul a cian, sol con corona y halo, dos nubes de cristal translúcido con borde luminoso). `mi-tv/iconos-tvos.sh` los renderiza con Chrome sin cabeza a PNG (960×540) dentro del launcher y del complemento. El glifo `ic_settings` (Centro de control y menús) es vectorial |
| (opción del launcher) `tvos_focus` | Foco estilo tvOS: la app enfocada se eleva con sombra, se inclina hacia donde se mueve el mando y le pasa un brillo. Viene apagado: se activa con `"tvos_focus": true` en `config.json` |
| (opción del launcher) `liquid_glass` | Cristal estilo tvOS 26 en el Centro de control, sus menús (Entradas, Ajustes, Tarjetas de módulos) y el menú de las apps: sin caja, cada botón es su propio cristal con el fondo desenfocado, un borde que curva la imagen como una lente y el canto iluminado. Viene apagado: se activa con `"liquid_glass": true` en `config.json` (este tema lo trae encendido) |
| (opción del launcher) `tvos_settings` | Ajustes del lanzador con el aspecto de los Ajustes de tvOS: el inicio desenfocado detrás, título centrado, icono grande a la izquierda con la explicación de la opción enfocada, opciones agrupadas a la derecha con su valor en gris; la fila enfocada se pone blanca y crece un poco. Las opciones con varios valores abren una lista con ✓. |
| `hdmi-appletv/` | Mini-app "Apple TV" que abre la entrada HDMI 1 |
| `debloat.txt` | Apps desactivadas en esta TV |
| `rebuild.sh`, `fotos.sh` | Compilar e instalar todo; bajar los fondos |
| `novedades-3.1.md`, `novedades-3.2.md`, `novedades-3.3.md`, `novedades-3.3.1.md`, `novedades-3.4.md`, `novedades-3.4.1.md`, `novedades-3.3-tv.5.md`, `novedades-3.3-tv.6.md`, `novedades-3.3-tv.7.md`, `novedades-3.3-tv.8.md` | Resumen en español de las notas de cada versión. Numeración: la versión oficial es la de Clean Your TV; detrás, `-tv.N` cuenta las entregas de esta rama sobre ella (3.3.1 = 3.3-tv.1, 3.4 = 3.3-tv.2, 3.4.1 = 3.3-tv.3, fondos de España y Portugal = 3.3-tv.4, iconos tvOS = 3.3-tv.5, fondos al azar = 3.3-tv.6, fondos desde el espacio = 3.3-tv.7, el tiempo con lluvia y nieve = 3.3-tv.8; los archivos `novedades-*` antiguos conservan su nombre). Con una versión oficial nueva vuelve a empezar (3.4-tv.1). |

`vpn_app` en el tema es un ejemplo (`com.example.vpn`): pon el paquete de tu app de VPN. La ciudad del tiempo se
elige en la app "El tiempo" de la TV.

## Traducciones / Translations

Los textos nuevos están en español y en inglés; cualquier otro idioma ve el inglés. Las traducciones son
bienvenidas: copia las cadenas de `launcher/app/src/main/res/values/strings.xml` a la carpeta de tu idioma y abre
una PR.

*The new texts are in Spanish and English; any other language shows the English. Translations are welcome: copy
the strings from `launcher/app/src/main/res/values/strings.xml` into your language's folder and open a PR.*
