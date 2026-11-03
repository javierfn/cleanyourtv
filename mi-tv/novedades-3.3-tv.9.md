# Novedades de esta rama 3.3-tv.9 (3 nov 2026): Chromecast en el Centro de control

Basada en Clean Your TV 3.3; novena entrega de esta rama sobre ella.

- ★ **Píldora Chromecast** en el Centro de control, junto a Entradas y con su mismo estilo (también en Liquid Glass):
  - «Listo · <nombre>»: el nombre que ven los móviles al enviar. Es el nombre del dispositivo de la TV
    (`Settings.Global` `device_name`), el mismo que da la página `eureka_info` del receptor; el launcher sigue sin
    permiso INTERNET, así que no la consulta. Si no cabe, el nombre se desplaza mientras la píldora está enfocada.
  - «No disponible» en gris si el receptor de Cast no responde.
  - No sale si el receptor de Cast (`com.google.android.apps.mediashell`) está desactivado o no existe.
- **Cómo lo sabe**: el launcher se enlaza a `CastReceiverService` del receptor (`CastStatus.java`), como hace
  [HALtheWise/ambient-tv](https://github.com/HALtheWise/ambient-tv). Mientras el enlace responde, Cast está listo, y
  además Android 14 no lo congela mientras Inicio está vivo (si estaba parado, el enlace lo arranca).
- **Tarjeta** al pulsarla, en el sitio del panel y con el mismo cristal: el nombre, «En el móvil, pulsa Enviar y
  elige esta tele» y el botón **Ajustes de Chromecast**, que abre la pantalla propia de Google Cast
  (`com.google.android.settings.CAST_RECEIVER_SETTINGS`: dejar que otros controlen lo que se envía, versión…) o,
  donde no exista, la ficha de la app en Ajustes.
- Opción de tema `cast_pill` (por defecto apagada), activada en `theme-glass-oscuro`. Textos en inglés y español.
- **Sin permisos nuevos**: solo se declara el paquete del receptor en `<queries>` para poder verlo. APK +3.936 B.
- No muestra «Enviando · <app>»: saber qué se está enviando necesitaría el acceso a las notificaciones.
