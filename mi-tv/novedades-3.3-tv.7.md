# Novedades de esta rama 3.3-tv.7 (3 nov 2026): fondos desde el espacio

Basada en Clean Your TV 3.3; séptima entrega de esta rama sobre ella.

- ★ **10 fondos nocturnos desde el espacio**: fotos de astronautas de la NASA en la Estación Espacial Internacional,
  las mismas del tema nuevo `aerial` de Clean Your TV (Gathin23/cleanyourtv#3126, en `themes/aerial`):
  amanecer y aurora sobre el norte de Europa, el sur de Escandinavia, una aurora boreal roja y verde, la puesta de
  sol vista desde la órbita, el Nilo de Luxor a El Cairo, Moscú con una aurora y la Luna, la luminiscencia del aire
  bajo la Vía Láctea, una aurora austral sobre el océano Índico, las luces del nordeste de Europa y una aurora
  sobre Canadá.
- Solo de **noche** (modo noche, 21:00–8:00): se suman a la lista `night` de `wallpaper_moods` de
  `theme-glass-oscuro`, intercaladas con las 16 que ya había (Lofoten, Vía Láctea, París, Hong Kong, Venecia y las de
  España y Portugal). Siguen al azar y cada 10 minutos (3.3-tv.6); se oscurecen igual que las demás
  (`wallpaper_dim` y la capa del modo noche).
- `fotos.sh` las baja de Wikimedia Commons a **3840x2160** (JPEG 92), como el resto, en lugar de usar las de
  `themes/aerial` (1920x1080 y ya oscurecidas tras el reloj y el dock, que con nuestro oscurecido quedarían demasiado
  apagadas). Tres llevan el recorte desplazado para que se vean como en el tema `aerial`.
- `fotos.sh` se identifica ante Wikimedia con un User-Agent con la dirección del proyecto: con el genérico, Commons
  empezó a responder 429 (demasiadas peticiones).
- Pies de foto en español, terminados en «· desde la ISS»; créditos en `wallpapers/CREDITOS.txt`. Las fotos de la
  NASA son de **dominio público**; se cita a la NASA como fuente y la NASA no respalda este proyecto.
- Se incluye también la carpeta `themes/aerial` tal como está en Clean Your TV, para que la próxima versión oficial
  encaje sin choques.
