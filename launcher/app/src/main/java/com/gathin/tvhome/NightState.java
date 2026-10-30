package com.gathin.tvhome;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/** Night as the Brightness add-on sees it (its schedule and its moon button), or null when it is not installed. */
final class NightState {
    private static final Uri URI = Uri.parse("content://io.github.maxhayim.brightness.night");

    private NightState() {
    }

    static Boolean query(Context context) {
        try (Cursor c = context.getContentResolver().query(URI, null, null, null, null)) {
            if (c == null || !c.moveToFirst()) return null;
            return c.getInt(0) == 1;
        } catch (Exception e) {
            return null;
        }
    }
}
