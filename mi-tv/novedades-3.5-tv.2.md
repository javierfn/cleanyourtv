# Novedades de esta rama 3.5-tv.2 (17 nov 2026): el dock con el fondo desenfocado

Segunda entrega sobre Clean Your TV 3.5. El launcher dice ahora `3.5-tv.2` en su versión.

## Qué cambia

- **`dock_blur`**: la banda de cristal del dock deja de transparentar el fondo nítido y muestra el fondo
  **desenfocado** detrás, como el dock del Apple TV. Activado en el tema `theme-glass-oscuro`
  (`"dock_blur": true`, junto a `dock_glass`), así que `rebuild.sh` y `actualizar.sh` lo mantienen.
- Funciona con nuestro foco tvOS: cuando el icono del extremo crece y la banda se ensancha, la parte nueva también
  sale desenfocada, con sus esquinas concéntricas.
- Es ligero: al cambiar de fondo se decodifica una copia pequeña (unos 240×135 px) y se desenfoca una vez fuera del
  hilo principal; la banda la dibuja escalada, sin vistas ni capas nuevas, así que desplazarse cuesta lo mismo.
  El APK crece 1,1 KB (680.010 B).

**Crédito:** la idea y el código son de **devSviat** (rama `launcher-dock-blur` de devSviat/cleanyourtv, propuesta a
Clean Your TV en la PR #3688, que se cerró sin fusionar). Aquí se adapta al foco tvOS.

## Cómo se integró

- Los dos commits de devSviat, aplicados sobre 3.5-tv.1 con su autoría. Choques con lo nuestro resueltos:
  `Config.java` (junto a `weather_now`), `DockDecoration.java` (también en la banda ensanchada del foco tvOS),
  `MainActivity.java` (el dock recibe a la vez el desenfoque y el foco tvOS) y `WallpaperPlayer.java`.
- `launcher-mods.patch` regenerado contra `v3.5.0`.
- Tests del launcher: 328 de 328 (3 nuevos de `DockBackdropTest`).

## Capturas

Mismo fondo (Las Cañadas del Teide), desactivado y activado: `docs/fork/dock-blur-off.jpg` y
`docs/fork/dock-blur-on.jpg`.

Para quitarlo: `"dock_blur": false` en `mi-tv/theme-glass-oscuro/config.json` y `mi-tv/rebuild.sh`.
