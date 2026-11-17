# Novedades de esta rama 3.5-tv.1 (17 nov 2026): pasa a Clean Your TV 3.5

Basada en Clean Your TV 3.5 (`v3.5.0`, 14 nov 2026); primera entrega de esta rama sobre ella (la numeración vuelve a
empezar con cada versión oficial: 3.4-tv.1 → 3.5-tv.1). El launcher dice ahora `3.5-tv.1` en su versión.

## Qué trae la 3.5 oficial (lo que afecta a esta tele)

- **Inicio más ligero** (#3804): cada cambio de ajustes dejaba la pantalla de inicio anterior en memoria, unos 15 MB
  cada vez. Ahora se libera: tras ocho cambios el inicio ocupa 114 MB en vez de 210 MB. Con los Ajustes estilo tvOS,
  que se tocan a menudo, se nota.
- **Reloj con cifras fijas** (#3746): las cifras del reloj tienen todas el mismo ancho, así que la fecha y el tiempo
  de debajo ya no se mueven un poco cada minuto.
- **Arreglos del mando**:
  - Atrás, Inicio, Recientes y Reposo en teclas propias del fabricante pasan por el servicio de teclas, como las
    demás (#3599).
  - Una tecla que está a la vez en `app_keys` y en las teclas de Ajustes abre solo su destino de `app_keys` (#3600).
  - Una tecla cuyo destino el launcher no puede abrir ya no deja parado el servicio de teclas (#3636).
- **Entradas legible** (#3437): la segunda línea del menú Entradas («Conectado», «Sin dispositivo») usa ahora la
  fuente normal en vez de la fina, que en la fila blanca enfocada casi no se leía. Aquí se mantiene nuestro color
  gris de los menús tvOS (`atv_menu_text_dim`) con la fuente nueva.
- **Sleep Timer con horario** (#3139): el complemento Sleep Timer puede encender y apagar la tele con un horario
  semanal, con selectores de fecha y hora que se manejan con el mando. No está instalado en esta tele; si se quiere:
  `addons/sleep-timer`.
- **Ajustes del tiempo en español** (#3593): la pantalla de ajustes del complemento del tiempo (unidad, días,
  humedad, viento) sale en español. Los textos que ya teníamos son los mismos que los oficiales, así que no hubo
  choque; se conserva lo nuestro (3 días, humedad y viento activados, «p. ej. Madrid»).
- Opciones nuevas, todas apagadas: `home_ignore_channel_keys` (los botones de canal no abren TV en directo desde el
  inicio), `volume_pauses_filter` y `settings_key_clear_task` (Atrás desde Ajustes vuelve al inicio en MediaTek).
- Ocho temas nuevos, tres complementos nuevos (Neon Dream, Koi Pond, tarjeta de Archivos), el README en español y
  472 modelos de tele.

**Aún no están en la 3.5.0** (se fusionaron el 16 nov, después de la versión, y llegarán con la siguiente):
- La sombra del pie de foto sin bordes duros (#4028).
- Editar dock conserva el sitio elegido y la cuadrícula iluminada (#3996).

## Cómo se integró

- Fusión de `v3.5.0` sobre esta rama. Tres choques, resueltos conservando lo nuestro:
  - `Config.java`: la opción nueva `settings_key_clear_task` se junta con las nuestras (`weather_now`,
    `weather_now_place`).
  - `MainActivity.dispatchKeyEvent`: primero el foco tvOS (`TvFocus.noteKey`), luego la opción nueva
    `home_ignore_channel_keys` y después el Centro de control.
  - `item_input.xml`: nuestro color de los menús tvOS con la fuente nueva de #3437.
- Los iconos tvOS del launcher (`banner_envivo`, `banner_ajustes`, `banner_tvenvivo`) pasan de PNG a WebP: el APK
  baja de 1,23 MB a 663 KB sin diferencia visible. `rebuild.sh` e `iconos-tvos.sh` ya generan WebP.
- El launcher oficial pesa 574 KB (el límite del proyecto es 576 KB); con lo nuestro (Liquid Glass, Centro de
  control, foco tvOS, Ajustes tvOS, Chromecast, el tiempo, modo noche…) son 663 KB. Para quedar bajo 576 KB habría
  que quitar funciones; en esta tele no hay problema de memoria.
- Parches de `mi-tv/` regenerados contra `v3.5.0` y comprobados con `git apply --check` sobre la 3.5 limpia.
- Tests: launcher 325 de 325; tiempo 10 de 10; Bluetooth Connect 1 de 1; Brightness 23 de 25 (fallan solo
  `DimmingTest` `offDrawsNothing…` y `nextChange…`, por los valores propios del modo noche, como siempre).

## dock_blur (aparte, sin fusionar)

`dock_blur` es una opción de tema de devSviat (PR #3688 a Clean Your TV, cerrada) que desenfoca el fondo detrás de
la banda del dock. Está en la rama `dock-blur`, sobre esta, para probarla; no forma parte de 3.5-tv.1.
