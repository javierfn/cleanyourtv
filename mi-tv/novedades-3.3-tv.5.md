# Novedades de esta rama 3.3-tv.5 (2 nov 2026): iconos tvOS

Basada en Clean Your TV 3.3; quinta entrega de esta rama sobre ella (antes numeradas 3.3.1, 3.4, 3.4.1 y fondos).

Iconos propios de esta rama, no de Clean Your TV. Marcado con ★ lo que afecta a esta TV.

## Iconos nuevos
- ★ **TV en Vivo** (`banner_envivo`): un televisor de cristal translúcido con el canto de aluminio pulido, la pantalla
  iluminada de magenta a azul con un botón de reproducir de cristal, antena de metal y una luz roja de directo con
  halo y ondas, sobre azul noche. El tema `theme-glass-oscuro` lo usa en lugar del magenta `banner_tv`.
- ★ **Ajustes** (`banner_ajustes`): un engranaje de metal pulido (brillo cónico, biselado, canto especular, centro
  hundido con piñón) con un engranaje de cristal esmerilado detrás, sobre grafito con viñeteado. Lo pone el tema con
  `app_banners` (`com.android.tv.settings=banner_ajustes`).
- ★ **El tiempo** (complemento `addons/weather`, tarjeta e icono): cielo de azul profundo a cian, sol con núcleo
  cálido, corona y halo, y dos nubes de cristal translúcido con borde luminoso por las que se ve el sol.
- ★ **Botón de Ajustes del Centro de control** (`ic_settings`, también en los menús): un engranaje más fino, con
  anillo dentado delgado y buje hueco.

## Cómo se hacen
- Diseño propio, sin arte de terceros. La fuente es `mi-tv/iconos-tvos.html` (capas como un icono de tvOS: fondo,
  cristal y objeto delantero) y `mi-tv/iconos-tvos.sh` los renderiza con Chrome sin cabeza a PNG de 960×540 dentro
  del launcher (`res/drawable-nodpi`) y del complemento. Son PNG y no vectoriales porque VectorDrawable no tiene
  desenfoque ni degradados cónicos.
- Para que salgan en la cuadrícula hay que aplicar el tema (`tools/theme.sh mi-tv/theme-glass-oscuro`) después de
  instalar el launcher; `mi-tv/rebuild.sh` ya lo hace.
