# Una TV con estilo Apple TV, sobre Clean Your TV 3.2

Configuración personal de un Panasonic Google TV LCD (kowloon, Vestel/MediaTek, Android 14) con el launcher de
[Clean Your TV](https://github.com/Gathin23/cleanyourtv). Esta rama es la versión 3.2.0 con los cambios ya
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
- La VPN como píldora con texto, el nombre del fondo sin recuadro y menús que no se activan solos con el OK
  mantenido de los mandos de infrarrojos.

## Carpeta mi-tv

| Ruta | Qué es |
|---|---|
| `theme-glass-oscuro/` | El tema: dock, nombres, iconos, 6 columnas, cristal oscuro, fondos de día y de noche (`wallpaper_moods`), rotación cada 15 minutos. Las fotos no están en git: `fotos.sh` las baja de Wikimedia Commons (créditos en `wallpapers/CREDITOS.txt`) |
| `theme-halloween/` | El mismo tema con los fondos y el disfraz de Halloween de Clean Your TV |
| `banner_tvenvivo.png` / `.html` | Icono de "TV en Vivo" (se copia dentro del launcher antes de compilar) |
| `hdmi-appletv/` | Mini-app "Apple TV" que abre la entrada HDMI 1 |
| `debloat.txt` | Apps desactivadas en esta TV |
| `rebuild.sh`, `fotos.sh` | Compilar e instalar todo; bajar los fondos |
| `novedades-3.1.md`, `novedades-3.2.md` | Resumen en español de las notas de cada versión |

`vpn_app` en el tema es un ejemplo (`com.example.vpn`): pon el paquete de tu app de VPN. La ciudad del tiempo se
elige en la app "El tiempo" de la TV.

## Traducciones / Translations

Los textos nuevos están en español y en inglés; cualquier otro idioma ve el inglés. Las traducciones son
bienvenidas: copia las cadenas de `launcher/app/src/main/res/values/strings.xml` a la carpeta de tu idioma y abre
una PR.

*The new texts are in Spanish and English; any other language shows the English. Translations are welcome: copy
the strings from `launcher/app/src/main/res/values/strings.xml` into your language's folder and open a PR.*
