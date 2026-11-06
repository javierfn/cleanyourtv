# Novedades de esta rama 3.4-tv.1 (6 nov 2026): pasa a Clean Your TV 3.4

Basada en Clean Your TV 3.4; primera entrega de esta rama sobre ella (la numeración vuelve a empezar con cada
versión oficial: 3.3-tv.10 → 3.4-tv.1).

## Qué trae la 3.4 oficial (lo que afecta a esta tele)

- **Relojes que no se paran**: el reloj del inicio y el del salvapantallas se redibujan cada minuto y al volver de
  reposo, aunque el sistema deje de avisar del cambio de minuto (#3044).
- **Botón del mando asignado a una entrada** (`app_keys` con `input:`): abre la entrada a través de la app de TV en
  directo, como ya hacía el menú Entradas (#3173).
- **`inputs_cec_state_only`** (nueva opción, apagada): en teles que dicen mal en qué HDMI hay algo conectado, solo
  muestran estado los aparatos que hablan HDMI-CEC (#2998).
- **Añadir al dock** funciona aunque la app ya esté en el hueco libre (#2923, el arreglo que propusimos desde esta
  rama: ahora viene de serie).
- **`show_stats`** muestra la memoria en núcleos antiguos sin MemAvailable (#2994).
- Cinco temas nuevos (`aerial`, `christmas`, `stockholm`, `dock-glass`, `weeb`), dos complementos nuevos (Tatami,
  Lịch Vạn Niên), 442 modelos de tele y mejoras de seguridad en las herramientas (`manage.py`, pruebas aisladas).
  El tema `aerial` ya lo usábamos desde 3.3-tv.7 para los fondos nocturnos desde el espacio: ahora es el oficial,
  sin diferencias.

## Cómo se integró

- Fusión de `v3.4.0` sobre esta rama. Un solo choque, en `Config.java`: el constructor junta la opción nueva
  `inputs_cec_state_only` con las nuestras (`weather_now`, `weather_now_place` al final).
- Todo lo propio se mantiene: Centro de control con Liquid Glass, foco tvOS, Ajustes estilo tvOS, Chromecast en el
  Centro de control, el tiempo bajo el reloj con lluvia y nieve, modo noche, fondos al azar, iconos tvOS, VPN y
  el resto de `mi-tv/`.
- Compilado el launcher y los complementos (tiempo, Brightness, Bluetooth Connect); los tests del launcher pasan.
  En Brightness fallan solo `DimmingTest` `offDrawsNothing…` y `nextChange…`, por los valores por defecto propios
  del modo noche (encendido, 8:00), igual que antes.
