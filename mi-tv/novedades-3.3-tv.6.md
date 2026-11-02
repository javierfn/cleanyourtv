# Novedades de esta rama 3.3-tv.6 (2 nov 2026): fondos al azar

Basada en Clean Your TV 3.3; sexta entrega de esta rama sobre ella.

- ★ **Fondos al azar**: el tema `theme-glass-oscuro` (y el de Halloween) pasa de `"wallpaper_order": "sequential"` a
  `"shuffle"`, así que los fondos salen en orden aleatorio y nunca repiten el anterior.
- ★ **Cada 10 minutos** (`"wallpaper_rotation_minutes": 10`, antes 15).
- ★ **Una foto nueva al abrir Inicio**: hasta ahora, al volver al Inicio (al encender la tele o al salir de una app)
  se reanudaba la última foto y, si ya tocaba cambiarla, se veía la vieja un momento y luego fundía a la nueva. Ahora
  `WallpaperPlayer.showFirst` comprueba si ya pasó el intervalo y, si es así, empieza directamente con otra.
