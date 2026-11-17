# Novedades de esta rama 3.5-tv.3 (17 nov 2026): top shelf como el de Apple TV

Tercera entrega sobre Clean Your TV 3.5. El launcher dice ahora `3.5-tv.3` en su versión.

## Qué cambia

Al enfocar una app del dock, la parte de arriba de Inicio enseña lo que esa app tiene para ti, como el
*top shelf* de tvOS:

- **Seguir viendo y sugerencias** (`continue_watching`): las series que tienes a medias, con su barra de
  progreso, y detrás los recomendados de la propia app; hasta 12 tarjetas, cuatro justas a lo ancho del dock.
  Se leen de lo que cada app publica para la pantalla de inicio de Android TV (Watch Next y sus canales de
  vista previa), con el permiso `READ_TV_LISTINGS`.
- **Vista grande**: desde la app, ▲ entra en las tarjetas y la imagen de la enfocada llena la pantalla con su
  título, temporada y episodio y la sinopsis. ◀▶ cambia de tarjeta, OK la abre en la app y ▼ o Atrás vuelve.
- **Qué listas de cada app** (`continue_watching_channels`): YouTube solo enseña «Suscripciones» (nada de
  Tendencias ni de recomendaciones genéricas). Si viene vacía, no hay fila.
- **Fondos por app**, en 4K, dibujados para esta rama:
  - con sugerencias (`continue_watching_backdrop`): el fondo de la app sin logo, y encima su logo pequeño en lugar
    del nombre (`continue_watching_logo`);
  - sin sugerencias (`continue_watching_static`): el mismo fondo con el logo grande en el centro.
  - Prime Video (azul marino), YouTube (collage de colores), Plex (negro con chevrones dorados) y Movistar Plus+
    (negro con cintas azul-violeta). TV en Vivo y Apple TV siguen con el paisaje y su nombre.
  - Con `dock_blur`, la banda del dock difumina el fondo de la app en vez del paisaje, y el pie de foto del
    paisaje se oculta mientras se ve el fondo de la app.
- **Sin Internet en el launcher**: las imágenes `https` de las tarjetas las trae el complemento **TV Posters**
  (`addons/posters`), firmado igual que el launcher, que las guarda en caché (las últimas 40).

## Qué apps tienen datos en esta tele

Prime Video (Seguir viendo y recomendados) y YouTube (solo publica sus listas sin cuenta, así que
«Suscripciones» viene vacía). Plex manda su «Seguir viendo» solo a Google (Engage), que ninguna otra app puede
leer; Movistar+ y el Apple TV por HDMI no publican nada.

## Créditos

- **ProfetGit** (rama `tora-shelf`): la fila de Seguir viendo bajo el nombre y la vista grande. Aplicado con su
  autoría y adaptado.
- **burnhamrobertp** (rama `feature/continue-watching-row`, issue #3114 de Clean Your TV): el complemento TV
  Posters, para que el launcher siga sin `INTERNET`.
- Idea original del top shelf en Clean Your TV: **bjrb20** (PR #363) y **Mehdi-Ks**.
- Logos de Prime Video, Plex y Movistar Plus+: SVG de Wikimedia Commons; son marcas de sus dueños. Los fondos son
  diseño propio (`mi-tv/fondo-*.html`, se generan con `mi-tv/fondo-youtube.sh <app> [sinlogo]` y
  `mi-tv/logos-shelf.sh`).

## Cómo se integró

- Rama `top-shelf` desde 3.5-tv.2, fusionada en `javi-tv`.
- `launcher-mods.patch` regenerado contra `v3.5.0`, y nuevo `addon-posters.patch` (el complemento entero).
- `rebuild.sh` compila e instala TV Posters y da `READ_TV_LISTINGS` al launcher.
- Tests del launcher: 338 de 338 (uno nuevo de recomendados en `WatchNextShelfTest`).
- El APK pasa de 680 KB a unos 1,45 MB por los ocho fondos y cuatro logos en 4K.

## Capturas

`capturas/top-shelf/`: fila y vista grande de Prime, YouTube, Plex y Movistar+ con su fondo.

Para quitarlo: `"continue_watching": false` en `mi-tv/theme-glass-oscuro/config.json` y `mi-tv/rebuild.sh`.
