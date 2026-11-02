# Novedades de esta rama 3.4 (2 nov 2026)

Cambios propios de esta rama, no de Clean Your TV. Marcado con ★ lo que afecta a esta TV.

## Liquid Glass
- ★ **Nueva opción `liquid_glass`** (apagada por defecto; el tema `theme-glass-oscuro` la enciende): el cristal del
  Centro de control se parece al de tvOS 26.
  - Sin caja alrededor: cada botón flota suelto sobre el fondo, que apenas se oscurece.
  - Cada botón es su propio cristal: deja ver el fondo desenfocado, el borde curva la imagen como una lente y el canto
    se ilumina arriba a la izquierda.
  - El botón enfocado se vuelve blanco con texto negro; el icono de Red sale en azul; lo que está activado lleva un
    cristal más claro.
- ★ El mismo cristal en los menús que cuelgan del Centro de control (Entradas, Ajustes, Tarjetas de módulos) y en el
  menú de las apps (mantener OK). Este último desenfoca una copia de la pantalla, así que los iconos de debajo se
  ven a través.
- El nombre de la foto de fondo se oculta mientras el Centro de control o uno de sus menús está abierto.
- El cristal se calcula una sola vez al abrir cada menú: en la TV, los fotogramas tardan lo mismo que antes.
- Arreglo: el cristal de Entradas y Ajustes (también sin `liquid_glass`) salía encogido en una esquina y dejaba ver
  el fondo sin desenfocar.
- No cambian las pantallas de Android ni de otras apps (Red, VPN, Desinstalar, Información de la aplicación…).

## Arreglos
- ★ **Foco estilo tvOS (`tvos_focus`)**, como en Apple TV:
  - Sin marco que se desplaza de una app a otra: el foco es solo el icono, que crece un 25 % (antes un 15 %) con
    sombra, inclinación y brillo.
  - Crece desde su centro, por igual hacia arriba, abajo y los lados. Antes además subía 10 dp y parecía crecer solo
    hacia arriba. El nombre baja lo justo para no tocarlo.
