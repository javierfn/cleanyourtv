# Weather and night on Home (a fork of Clean Your TV)

Two opt-in Home switches for the reference launcher, fed by two of this repo's add-ons, as proposed in
[#2603](https://github.com/Gathin23/cleanyourtv/pull/2603), [#2604](https://github.com/Gathin23/cleanyourtv/pull/2604),
[#2611](https://github.com/Gathin23/cleanyourtv/pull/2611) and [#2612](https://github.com/Gathin23/cleanyourtv/pull/2612)
and kept here, since the launcher upstream has no room for them. The launcher stays offline: it only reads one row
from each add-on and starts the add-on's own switch.

![weather_now and night_mode on a Panasonic at night](docs/switches/night_mode.jpg)

- **`weather_now`**: the weather now under the clock ("☁  14°  Vigo"), from the Weather add-on
  (`io.github.sumyabazar1.weather`). The add-on keeps its last reading in a read-only provider and, when Home asks
  and the reading is older than 30 minutes, reads the weather now once more (one small Open-Meteo request). A
  reading older than three hours hides the line; one for a city no longer chosen is never shown.
- **`night_mode`**: a moon button in the top bar by day and a sun at night, from the Brightness add-on
  (`io.github.maxhayim.brightness`). One press gives the other side until the add-on's schedule changes it back
  (day at a scheduled night, night by day); holding OK opens Brightness. With `wallpaper_moods`, Home's night
  pictures follow Brightness's night rather than the clock, and change at once.

Both are off unless a theme sets them:

```json
{ "weather_now": "io.github.sumyabazar1.weather", "night_mode": "io.github.maxhayim.brightness" }
```

The contracts are in [`docs/modules.md`](docs/modules.md), the keys in [`themes/README.md`](themes/README.md).

## Build and install

As upstream, from this branch (`tv-extras`): `TV_HOST=<tv ip> launcher/build-and-install.sh`, then build and install
`addons/weather` and `addons/brightness` (each add-on's README), and push a theme with the keys above
(`tools/theme.sh`). Choose the city in the Weather app once.

## Permissions

The launcher asks for nothing new and has no `INTERNET` permission. Weather keeps its one permission, `INTERNET`,
for Open-Meteo; Brightness asks for nothing new. Both new providers are exported read-only and hold only what Home
shows: the city's name, temperature and sky, and whether it is night.

## Tested on

Panasonic Google TV LCD (OZUM_Panasonic, kowloon, Vestel, MediaTek), Android 14, build UKNV.260729.001, Spain,
since 28 Oct 2026: the weather line under the clock with the Weather page never opened, the sun and moon button
switching Brightness to day and back at a scheduled night, the night pictures following it at once, and the
launcher and both add-ons back by themselves after a reboot.

Issues: [javierfn/cleanyourtv/issues](https://github.com/javierfn/cleanyourtv/issues). Upstream:
[Gathin23/cleanyourtv](https://github.com/Gathin23/cleanyourtv).
