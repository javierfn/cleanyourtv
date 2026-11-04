# Novedades de esta rama 3.3-tv.10 (4 nov 2026): Ajustes del lanzador estilo tvOS

Basada en Clean Your TV 3.3; décima entrega de esta rama sobre ella.

- ★ **Ajustes del lanzador con el aspecto de los Ajustes de tvOS** (opción de tema `tvos_settings`, nuevo
  `TvosSettings.java`):
  - El inicio se ve **desenfocado detrás**: al abrir los ajustes se toma una copia pequeña de la pantalla con
    `PixelCopy` (lo que de verdad se ve, cristal incluido) y se desenfoca. Si no se puede, el fondo queda oscuro liso.
  - **Título centrado** arriba.
  - A la izquierda, el **icono de Ajustes** grande y, debajo, la **explicación de la opción enfocada**. El icono está
    fijo: si la explicación es larga, solo crece hacia abajo.
  - A la derecha, las opciones **agrupadas** (Inicio, Fondo y reloj, Efectos, General) con su **valor en gris**.
  - La fila enfocada se pone **blanca** con el texto negro y crece un poco.
  - Las opciones con varios valores abren una **lista con ✓** en el mismo sitio, en lugar de los diálogos de Android.
    **Atrás** cierra la lista y vuelve a la fila; otro Atrás sale al inicio.
- Sin la opción, la pantalla de ajustes sigue igual que antes.
- Activada en `theme-glass-oscuro`. Textos en inglés y español. Sin permisos nuevos.
