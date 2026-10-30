package com.gathin.tvhome;

import android.content.Context;

/**
 * The wallpaper's slow zoom and drift, on unless switched off in Launcher settings. Kept in its own preferences so a
 * theme push or "Follow theme" leaves the owner's choice alone.
 */
final class WallpaperMotion {
    static final long CYCLE_MS = 25_000L;
    /** The deepest zoom; wallpapers are decoded this much larger than the screen when the motion is on. */
    static final float MAX_SCALE = 1.2f;
    private static final String PREFS = "wallpaper_motion";
    private static final String KEY = "on";

    private WallpaperMotion() {
    }

    static boolean enabled(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true);
    }

    static void set(Context context, boolean on) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply();
    }
}
