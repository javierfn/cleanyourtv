package com.gathin.tvhome;

import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * weather_now (config.xml or a theme): the weather now under the clock, a sky glyph, the temperature and the place
 * ("☁  14°  Madrid"), from an add-on such as Weather. It takes the stats line's place, so it shows only without
 * show_stats. The launcher only reads what the add-on publishes and stays offline. Contract: docs/modules.md.
 */
final class WeatherNow {
    static final String META = "com.gathin.tvhome.weather";
    /** A reading older than this is not the weather now any more: the line hides until a newer one. */
    static final long STALE_MS = 3 * 60 * 60_000L;
    /** Below this a chance of rain or snow is noise under the clock, as in LostInInaka's header_weather. */
    static final int MIN_CHANCE = 30;
    private static final String[] COLUMNS = {"place", "temperature", "unit", "code", "is_day", "updated_ms", "sky", "humidity",
            "precipitation_chance", "snow_chance"};

    /** The words of the line, from the launcher's strings in the TV's language. */
    static final class Words {
        final String humidity;
        final String rain;
        final String snow;
        final String chance;
        final String rainWord;
        final String snowWord;

        /** Formats with one %d ("Humedad %1$d %%", "Lluvia %1$d %%", "Nieve %1$d %%", "%1$d %%"), then the two words. */
        Words(String humidity, String rain, String snow, String chance, String rainWord, String snowWord) {
            this.humidity = humidity;
            this.rain = rain;
            this.snow = snow;
            this.chance = chance;
            this.rainWord = rainWord;
            this.snowWord = snowWord;
        }

        static Words of(Context ctx) {
            return new Words(ctx.getString(R.string.weather_humidity), ctx.getString(R.string.weather_rain),
                    ctx.getString(R.string.weather_snow), ctx.getString(R.string.weather_chance),
                    ctx.getString(R.string.weather_rain_word), ctx.getString(R.string.weather_snow_word));
        }
    }

    /** One reading, as the add-on publishes it. */
    static final class Reading {
        final String place;
        final int temperature;
        final int code;
        final boolean day;
        final long updatedMs;
        /** The sky in words from the add-on ("Nublado"), empty when it sends none. */
        final String skyText;
        /** Relative humidity in %, or -1 when the add-on sends none. */
        final int humidity;
        /** Chance of precipitation in %, or -1 when the add-on sends none. */
        final int precipChance;
        /** Chance of snow in %, sent only when the forecast says snow; -1 otherwise. */
        final int snowChance;

        Reading(String place, int temperature, int code, boolean day, long updatedMs, String skyText, int humidity,
                int precipChance, int snowChance) {
            this.place = place;
            this.temperature = temperature;
            this.code = code;
            this.day = day;
            this.updatedMs = updatedMs;
            this.skyText = skyText;
            this.humidity = humidity;
            this.precipChance = precipChance;
            this.snowChance = snowChance;
        }

        /**
         * "14° · Lluvia · 70 % · Humedad 90 % · Madrid"; the unit is the add-on's, and each part after the degrees may
         * be missing. The chance shows from {@link #MIN_CHANCE} up, snow wins over rain, a snow sky never shows a rain
         * figure, and the word is left out when the sky already says it. Whether the sky is snow comes from the WMO
         * code too, since the add-on writes the sky in the TV's language.
         */
        String text(Words w, boolean withPlace) {
            StringBuilder b = new StringBuilder().append(temperature).append('°');
            if (!skyText.isEmpty()) b.append(" · ").append(skyText);
            boolean snowSky = isSnow(code) || says(skyText, w.snowWord);
            if (snowChance >= MIN_CHANCE) {
                b.append(" · ").append(String.format(snowSky ? w.chance : w.snow, snowChance));
            } else if (precipChance >= MIN_CHANCE && !snowSky) {
                b.append(" · ").append(String.format(says(skyText, w.rainWord) ? w.chance : w.rain, precipChance));
            }
            if (humidity >= 0) b.append(" · ").append(String.format(w.humidity, humidity));
            if (withPlace && !place.isEmpty()) b.append(" · ").append(place);
            return b.toString();
        }
    }

    private final Context ctx;
    private final TextView view;
    private final Uri uri;
    private final Handler main;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ContentObserver observer;
    private boolean watching;
    private Reading reading;
    private boolean showPlace = true;
    /** While Home is on screen; a result or a change that arrives after stop() is dropped (main thread only). */
    private boolean active;
    /** Bumped on each start and stop, so a query from an earlier session never lands in a later one. */
    private int session;
    /**
     * While Home stays on screen, ask again a little after the add-on's own 30 minutes, so it fetches a new reading
     * and the line never goes stale on a Home left open all afternoon.
     */
    static final long ASK_AGAIN_MS = 31 * 60_000L;
    private final Runnable askAgain = new Runnable() {
        @Override
        public void run() {
            if (!active) return;
            read();
            main.postDelayed(this, ASK_AGAIN_MS);
        }
    };

    private WeatherNow(Context ctx, TextView view, Uri uri, Handler main) {
        this.ctx = ctx;
        this.view = view;
        this.uri = uri;
        this.main = main;
        observer = new ContentObserver(main) {
            @Override
            public void onChange(boolean selfChange) {
                if (active) read();
            }
        };
    }

    /**
     * Null when the key is empty, the package is not installed, or it names no provider of its own in a
     * {@value #META} meta-data. Naming the package is the owner's opt-in.
     */
    static WeatherNow create(Context ctx, String pkg, TextView view, Handler main) {
        if (pkg == null || pkg.trim().isEmpty()) return null;
        try {
            PackageManager pm = ctx.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(pkg.trim(), PackageManager.GET_META_DATA);
            String authority = info.metaData == null ? null : info.metaData.getString(META);
            if (authority == null || authority.trim().isEmpty()) return null;
            // The provider must be the named package's own, not another app's that claims the authority.
            ProviderInfo provider = pm.resolveContentProvider(authority.trim(), 0);
            if (provider == null || !pkg.trim().equals(provider.packageName)) return null;
            Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(authority.trim()).appendPath("now").build();
            float dp = ctx.getResources().getDisplayMetrics().density;
            // The same look as the wallpaper's caption on the right: 15 sp, regular, the same white and shadow.
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            view.setTextColor(0xE6FFFFFF);
            view.setShadowLayer(2 * dp, 0, dp, 0x99000000);
            try {
                view.setTypeface(UiFont.get(ctx, R.string.font_regular));
            } catch (Exception ignored) {
                // the system font is fine
            }
            view.setPadding(0, Math.round(4 * dp), 0, 0);
            return new WeatherNow(ctx, view, uri, main);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /** Home is on screen: show the last reading, ask for a new one and watch for changes. */
    void start() {
        active = true;
        session++;
        main.removeCallbacks(askAgain);
        main.postDelayed(askAgain, ASK_AGAIN_MS);
        if (!watching) {
            try {
                ctx.getContentResolver().registerContentObserver(uri, false, observer);
                watching = true;
            } catch (SecurityException e) {
                // The add-on's provider went away; the line keeps what it had.
            }
        }
        show();
        read();
    }

    /** Home is off screen: nothing runs until it is back. */
    void stop() {
        active = false;
        session++;
        main.removeCallbacks(askAgain);
        if (watching) ctx.getContentResolver().unregisterContentObserver(observer);
        watching = false;
    }

    void shutdown() {
        stop();
        io.shutdownNow();
    }

    /** One query off the main thread (the provider lives in the add-on's process), then the line on the main thread. */
    private void read() {
        if (!active) return;
        int asked = session;
        try {
            io.execute(() -> {
                Object[] row = null;
                try (Cursor c = ctx.getContentResolver().query(uri, COLUMNS, null, null, null)) {
                    if (c != null && c.moveToFirst()) {
                        // By name, as the contract says: a provider may answer in another order or with fewer columns.
                        row = new Object[COLUMNS.length];
                        for (int i = 0; i < COLUMNS.length; i++) {
                            int at = c.getColumnIndex(COLUMNS[i]);
                            if (at < 0 || c.isNull(at)) continue;   // getInt answers 0 for NULL: keep it missing
                            row[i] = c.getType(at) == Cursor.FIELD_TYPE_STRING ? c.getString(at) : c.getLong(at);
                        }
                    }
                } catch (RuntimeException gone) {
                    // Uninstalled or crashed mid-query: keep what is on screen.
                    return;
                }
                Reading parsed = fromRow(row);
                main.post(() -> {
                    if (!active || session != asked) return;   // Home left, or came back and asked again since
                    reading = parsed;
                    show();
                });
            });
        } catch (RejectedExecutionException shutDown) {
            // Home is being destroyed.
        }
    }

    /** weather_now_place: false leaves the place off the end of the line. */
    void showPlace(boolean on) {
        if (showPlace == on) return;
        showPlace = on;
        show();
    }

    private void show() {
        Reading r = reading;
        if (r == null || System.currentTimeMillis() - r.updatedMs > STALE_MS) {
            view.setVisibility(View.GONE);
            return;
        }
        String text = r.text(Words.of(ctx), showPlace);
        String glyph = sky(r.code, r.day);
        view.setText(glyph.isEmpty() ? text : glyph + "  " + text);
        view.setContentDescription(text);
        view.setVisibility(View.VISIBLE);
    }

    /** The add-on's row as a reading; null for no row, no temperature or no time. */
    static Reading fromRow(Object[] r) {
        if (r == null || !(r[1] instanceof Number) || !(r[5] instanceof Number)) return null;
        long updated = ((Number) r[5]).longValue();
        if (updated <= 0) return null;
        String place = r[0] instanceof String ? ((String) r[0]).trim() : "";
        int code = r[3] instanceof Number ? ((Number) r[3]).intValue() : -1;
        boolean day = !(r[4] instanceof Number) || ((Number) r[4]).intValue() != 0;
        String skyText = r.length > 6 && r[6] instanceof String ? ((String) r[6]).trim() : "";
        int humidity = r.length > 7 && r[7] instanceof Number ? ((Number) r[7]).intValue() : -1;
        return new Reading(place, (int) Math.round(((Number) r[1]).doubleValue()), code, day, updated, skyText,
                percent(r, 7), percent(r, 8), percent(r, 9));
    }

    /** An optional percentage column, or -1 when it is missing or outside 0..100 (a wrong figure, not a clamp). */
    private static int percent(Object[] r, int i) {
        if (r.length <= i || !(r[i] instanceof Number)) return -1;
        int v = ((Number) r[i]).intValue();
        return v >= 0 && v <= 100 ? v : -1;
    }

    /** The WMO codes that are snow, whatever words the add-on uses for them. */
    static boolean isSnow(int code) {
        return (code >= 71 && code <= 77) || code == 85 || code == 86;
    }

    /** Whether the sky's words already say this one ("Nieve débil" says "nieve"), case aside. */
    private static boolean says(String sky, String word) {
        return !word.isEmpty() && sky.toLowerCase(java.util.Locale.ROOT).contains(word.toLowerCase(java.util.Locale.ROOT));
    }

    /**
     * WMO weather code to a sky symbol, drawn as text (U+FE0E asks for the plain glyph, not a colour emoji). The same
     * table as LostInInaka's header_weather, so both forks draw one symbol per code; a code it does not know draws none.
     */
    static String sky(int code, boolean day) {
        switch (code) {
            case 0: case 1: return day ? "\u2600\uFE0E" : "\u263E";                  // ☀ clear, mainly clear; ☾ at night
            case 2: return "\u26C5\uFE0E";                                             // ⛅ partly cloudy, day and night
            case 3: return "\u2601\uFE0E";                                             // ☁ overcast
            case 45: case 48: return "\u2261";                                         // ≡ fog
            case 51: case 53: case 55: case 56: case 57: return "\u2602\uFE0E";        // ☂ drizzle
            case 61: case 63: case 65: case 66: case 67:
            case 80: case 81: case 82: return "\u2614\uFE0E";                          // ☔ rain, rain showers
            case 71: case 73: case 75: case 77: case 85: case 86: return "\u2744\uFE0E"; // ❄ snow, snow showers
            case 95: case 96: case 99: return "\u26A1\uFE0E";                          // ⚡ thunderstorm
            default: return "";
        }
    }
}
