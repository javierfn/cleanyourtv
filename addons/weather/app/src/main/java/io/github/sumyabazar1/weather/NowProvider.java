package io.github.sumyabazar1.weather;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The weather now, for Clean Your TV's Home (its weather_now key, contract in docs/modules.md): one row with the
 * place, the temperature in this app's unit, the WMO code and day or night. Read-only and exported, so it holds
 * nothing private: the place's name is what Home shows anyway, and the coordinates stay in the app.
 *
 * It answers from the last reading, the page's or its own. When that is older than {@link Forecast#REFRESH_MS} it
 * reads Open-Meteo once, off the caller's thread, and tells the launcher with notifyChange. So nothing runs in the
 * background: a read happens only while the weather page is open or Home asks for the line.
 */
public final class NowProvider extends ContentProvider {
    static final String AUTHORITY = "io.github.sumyabazar1.weather.now";
    static final Uri URI = Uri.parse("content://" + AUTHORITY + "/now");
    static final String[] COLUMNS = {"place", "temperature", "unit", "code", "is_day", "updated_ms", "sky", "humidity"};
    private static final String PREFS = "now";

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private volatile long lastTry;
    /** The place lastTry was for: a new city is read at once, not after the old one's retry wait. */
    private volatile String lastTryWhere = "";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        Context c = getContext();
        MatrixCursor out = new MatrixCursor(COLUMNS);
        if (c == null) return out;
        SharedPreferences place = c.getSharedPreferences("place", Context.MODE_PRIVATE);
        if (!place.contains("name")) return out;   // no city chosen yet: no line on Home
        SharedPreferences now = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        long lat = place.getLong("lat", 0), lon = place.getLong("lon", 0);
        // A reading for another place (the city changed since) is no reading for this one.
        boolean here = now.getLong("lat", 1) == lat && now.getLong("lon", 1) == lon;
        long updated = here ? now.getLong("updated_ms", 0) : 0;
        long time = System.currentTimeMillis();
        String where = lat + "," + lon;
        if (time - updated > Forecast.REFRESH_MS && (!where.equals(lastTryWhere) || time - lastTry > Forecast.RETRY_MS)) {
            lastTry = time;
            lastTryWhere = where;
            io.execute(() -> {
                try {
                    publish(c, Forecast.now(Double.longBitsToDouble(lat), Double.longBitsToDouble(lon)), lat, lon);
                } catch (Exception e) {
                    // Offline: Home keeps the last reading, and the next query after RETRY_MS tries again.
                }
            });
        }
        if (updated == 0) return out;
        boolean fahrenheit = fahrenheit(c);
        out.addRow(new Object[] {place.getString("name", ""),
                Forecast.degrees(Double.longBitsToDouble(now.getLong("temp_c", 0)), fahrenheit),
                fahrenheit ? "F" : "C", now.getInt("code", 0), now.getBoolean("day", true) ? 1 : 0, updated,
                // The sky in words, in the TV's language, and the relative humidity in % (null when unknown).
                c.getString(Forecast.sky(now.getInt("code", 0), now.getBoolean("day", true))),
                now.contains("humidity") ? now.getInt("humidity", 0) : null});
        return out;
    }

    /**
     * A new reading, from the page or from a query, for the place at these coordinates (raw long bits, as the place
     * preferences keep them). A reading that comes back after the city changed is dropped, so Home never shows one
     * city's weather under another's name.
     */
    static synchronized void publish(Context c, Forecast.Report r, long lat, long lon) {
        SharedPreferences place = c.getSharedPreferences("place", Context.MODE_PRIVATE);
        if (place.getLong("lat", 0) != lat || place.getLong("lon", 0) != lon) return;
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong("lat", lat)
                .putLong("lon", lon)
                .putLong("temp_c", Double.doubleToRawLongBits(r.tempC))
                .putInt("code", r.code)
                .putInt("humidity", r.humidity)
                .putBoolean("day", r.day)
                .putLong("updated_ms", System.currentTimeMillis())
                .commit();
        c.getContentResolver().notifyChange(URI, null);
    }

    /** A new place was saved: Home asks again, and the query reads the new one at once. */
    static void placeChanged(Context c) {
        c.getContentResolver().notifyChange(URI, null);
    }

    private static boolean fahrenheit(Context c) {
        return "f".equalsIgnoreCase(c.getString(R.string.temperature_unit).trim());
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] args) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] args) {
        throw new UnsupportedOperationException("read-only");
    }
}
