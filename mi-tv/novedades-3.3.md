# Novedades de CleanYourTV 3.3 (1 nov 2026)

Resumen en español de las notas oficiales. Marcado con ★ lo que afecta a esta TV.

## Mantener OK para mover las apps
- ★ **El menú de las apps viene activado** (`tile_menu`): mantener OK en una app del inicio abre Mover, Quitar del inicio
  (con la biblioteca de apps) e Información. Esta rama ya lo tenía con estilo Apple TV y Desinstalar; se mantiene.
- Para volver a lo de antes: `"tile_menu": false` en el `config.json` del tema.

## Launcher 3.3 (todo lo nuevo apagado por defecto)
- Reloj de 12 o 24 horas (`clock_format`).
- Atrás en el inicio abre el salvapantallas del reloj (`back_opens_clock`).
- La app de arranque también se abre al encender la pantalla (`open_on_start_on_wake`).
- Un botón del mando para la biblioteca de apps (`app_keys` con `library`), y entradas `hold:` para teclas del fabricante.
- `glow_snap`: el brillo ambiental cambia de color de golpe en vez de fundirse.
- Arreglos: el resalte de cristal se coloca bien tras un arranque en frío; ★ la píldora de la VPN reposa sobre cristal
  oscuro para que su color se lea sobre un fondo claro.

## Complementos (31 en total)
- Nuevo: **Open on wake**, abre la app que elijas al encender la pantalla.
- ★ **El tiempo habla español** (nuestra PR #2602) y sus opciones se cambian con el mando.

## Temas
- Nuevos: minimal, blocky, storm y red-rain (16 en total). El disfraz de Halloween se apaga solo después del 1 de
  noviembre; el tema sigue disponible.

## Otros
- 47 modelos de TV más (407), asturiano en el launcher (31 idiomas), y unas 20 herramientas que explican con una frase
  cuándo no llegan a la TV.
