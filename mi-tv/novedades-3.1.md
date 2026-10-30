# Novedades de CleanYourTV 3.1 (27 oct 2026)

Resumen de las notas oficiales de la versión, en español. Marcado con ★ lo que te puede servir en tu TV.

## Launcher 3.1 (todo viene apagado; se activa desde el tema)

- ★ **Entradas sin puertos vacíos**: solo salen los HDMI con algo conectado; los demás quedan en "Más entradas" (`inputs_connected_only`).
- ★ **Botón de VPN propio** junto al de red: verde con la VPN activa, rojo con "VPN off". Al pulsarlo abre tu app de VPN (`vpn_pill`).
- ★ **Inicio en cada encendido**, aunque la TV se apagara en un HDMI, en la TDT o en una app (`power_on_home_always`).
- ★ **Mantener pulsado Inicio** también abre el launcher, útil con mandos lentos (`home_long_press_home`).
- ★ **Botones del mando más potentes**:
  - el botón de TV en directo abre la guía de canales si ya estás viendo la tele;
  - los botones de apps del fabricante (como el de Netflix) se pueden reasignar a otra app;
  - un botón puede actuar al mantenerlo pulsado (`hold:`);
  - los botones de la barra superior respetan las acciones de lanzamiento.
- **Iconos en vez de banner** para apps cuyo banner se recorta mal (`icon_tiles`).
- **Reiniciar…** en el menú del botón de ajustes (pasa por el menú de apagado de la TV, que pide confirmación).
- **Idioma propio del launcher**, distinto del de la TV (`locale`).
- Fecha lunar en coreano.

### Arreglos del launcher
- El botón de Ajustes funciona en más TVs (abre los ajustes de Android si el fabricante oculta los suyos).
- El menú de Entradas abre bien en Android 9; los HDMI de un modelo Xiaomi abren el puerto correcto.
- ★ El salvapantallas de reloj recorta el fondo como Inicio y muestra la misma foto.
- Una rotación de fondos de 1 minuto ya no cambia cada 2.
- ★ Menús y diálogos totalmente opacos, sin texto que se transparente por detrás.
- El brillo de la barra vuelve tras pulsar Inicio dos veces.
- Hebreo en espejo en los paneles; la fecha lunar ya no muestra "día 0".
- El panel de carreras se oculta cuando acaba el calendario.

## Complementos nuevos (26 en total)
- ★ **TV Inspector**: muestra qué envía cada botón del mando (nombre, código) y qué puede reproducir la TV (resoluciones, HDR, códecs, audio). Sin permisos, ~18 KB.
- ★ **Bluetooth Connect**: altavoces y auriculares emparejados con un botón Conectar/Desconectar (los ajustes de la TV no lo tienen).
- **Bluetooth Toggle**: activar o desactivar Bluetooth de un toque (Android 12 o anterior; tu TV es 14).
- **Salvapantallas de noticias**: titulares de tus fuentes RSS con un código QR a cada artículo.
- **Digital Rain**: salvapantallas estilo Matrix.
- **Calendario judío**.

Mejoras en complementos existentes: Sources (ahora "Devices"), El tiempo y Auriculares en francés; World Clock funciona como salvapantallas de la TV; Frigate Glance mejorado; Brillo y Temporizador de apagado tienen una barra rápida para un botón del mando; Startup & Branding conserva tu imagen si falla una importación.

## Temas
- Nuevo tema **nature-calm**: 12 fotos de naturaleza de Wikimedia, una nueva cada 5 minutos, con lugar y fotógrafo.
- `theme.sh` comprueba los nombres de los fondos y rechaza un `config.json` que el launcher descartaría antes de tocar la TV.

## Herramientas y seguridad
- Los modos forzados (`FORCE_SETUPWRAITH`, etc.) avisan cuando están activos.
- Las comprobaciones de `disable.sh` ya no pasan sin comprobar y dicen qué paquetes quedaron activos.
- `restore.sh` deshace también lo que desactivaron `check.py --fix` o una segunda pasada.
- Aviso si la TV cambia de IP a mitad de una limpieza.
- Soporte para ADB cifrado (depuración inalámbrica ya emparejada).
- Mensajes más claros cuando el puerto ADB está cerrado o macOS bloquea la red local.
- La clave ADB se guarda con permisos privados.
- Listas de limpieza más seguras para algunos modelos de TCL, Sony y Kalley.

## Otros
- **Samsung, LG y Roku**: nueva guía en cleanyour.tv/not-android para quitar el rastreo con el mando.
- 41 modelos de TV más (341 en total, 93 marcas), entre ellos dos Panasonic 4KTV.
- Sueco en el launcher (29 idiomas); el español queda completo.
- README en neerlandés y chino tradicional.
- 140 proyectos en el escaparate.
- El launcher pesa ~532 KB.
