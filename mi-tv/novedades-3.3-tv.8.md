# Novedades de esta rama 3.3-tv.8 (3 nov 2026): el tiempo, igual que header_weather

Basada en Clean Your TV 3.3; octava entrega de esta rama sobre ella. Igualada con `header_weather` / Weather Glance
de LostInInaka (Gathin23/cleanyourtv#2786), para que un mismo complemento del tiempo funcione con los dos launchers.

- ★ **Probabilidad de lluvia o nieve** en la línea del tiempo: «☔ 11° · Lluvia · 80 % · Humedad 90 %».
  - Solo desde el **30 %**: por debajo es ruido bajo el reloj.
  - La **nieve gana a la lluvia**, y un cielo de nieve nunca muestra la cifra de lluvia.
  - **No repite la palabra** si el cielo ya la dice («Nieve · 70 %», no «Nieve · Nieve 70 %»); que el cielo es de
    nieve se decide también por el código WMO, porque el complemento escribe el cielo en el idioma de la TV.
  - Las palabras («Lluvia», «Nieve») están en los textos del launcher, en inglés y en español.
- **Complemento El tiempo**: pide a Open-Meteo `precipitation_probability` y `snowfall` y publica dos columnas nuevas,
  `precipitation_chance` y `snow_chance`. Open-Meteo no da probabilidad de nieve, así que `snow_chance` solo lleva la
  cifra cuando la lectura dice nieve (código de nieve, también tras la corrección de lluvia fría a nieve, o
  nevada mayor que cero) y va vacía en otro caso.
- **Símbolos de `header_weather`**: ≡ niebla, ☂ llovizna, ☔ lluvia y chubascos, ❄ nieve, ⚡ tormenta, ⛅
  parcialmente nublado también de noche; un código desconocido no dibuja símbolo. Siguen como texto (U+FE0E), no
  como emoji en color.
- El launcher lee **todas las columnas por nombre** (antes las seis básicas iban por posición), una columna vacía ya
  no cuenta como 0, y una humedad o probabilidad fuera de 0–100 se descarta en vez de recortarse.
- **Contrato completo** en `docs/modules.md` («The weather now on Home»): columnas, reglas de la línea, la ciudad
  como opcional (`weather_now_place`) y la tabla de símbolos compartida. `weather_now` y `weather_now_place` tienen
  ya su fila en `themes/README.md`.
- Tests nuevos: `WeatherNowTest` (launcher) y `SnowChanceTest` (complemento).
