# Novedades de esta rama 3.3.1 (2 nov 2026)

Un arreglo traído de Clean Your TV antes de su siguiente versión. Marcado con ★ lo que afecta a esta TV.

## Añadir al dock
- **Añadir al dock ya no falla cuando la app elegida ocupa el hueco libre** (upstream
  [Gathin23/cleanyourtv#2923](https://github.com/Gathin23/cleanyourtv/pull/2923)). Con `fill_dock` en `false` y el dock
  sin llenar, al reordenar no se ven huecos vacíos y la primera app de la cuadrícula se pone en el sitio libre. Si se
  elegía ese sitio para esa misma app, no se guardaba nada y la app se quedaba fuera del dock.
- En esta TV el dock está lleno (seis apps), así que el caso no se daba, pero el arreglo sirve si un día queda un hueco.
  ★ Probado con el mando: Añadir al dock, Sustituir por otra app y Mover siguen funcionando, y el dock queda en su orden.
- Un test unitario nuevo en `DockSlotsTest`; los 309 tests del launcher pasan.
