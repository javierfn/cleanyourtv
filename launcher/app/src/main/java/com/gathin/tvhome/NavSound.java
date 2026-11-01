package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.SystemClock;

/**
 * Home's navigation sounds: a soft tick as the D-pad moves focus (res/raw/nav_tick.wav) and a pop as an app opens
 * (res/raw/open_pop.wav), both synthesised for this project (tools/nav_sounds.py), so free of anyone's copyright.
 * Off unless the theme's config.json has "nav_sounds": true or Launcher settings turns them on; the settings choice
 * lives in its own preferences, so a theme push leaves it alone. Loaded only while on.
 */
final class NavSound {
    private static final String PREFS = "nav_sounds";
    private static final String KEY = "on";
    /** Holding a key repeats focus moves faster than ticks stay pleasant; drop ones closer than this. */
    private static final long MIN_GAP_MS = 45;
    private static final float TICK_VOLUME = 0.35f;
    private static final float POP_VOLUME = 0.45f;

    private final SoundPool pool;
    private int tick;
    private int pop;
    private boolean tickLoaded;
    private boolean popLoaded;
    private long lastTickAt;

    static boolean enabled(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return p.contains(KEY) ? p.getBoolean(KEY, false) : WeatherLine.configFlag(context, "nav_sounds");
    }

    static void set(Context context, boolean on) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply();
    }

    /** The sounds if they are on, or null. */
    static NavSound create(Context context) {
        return enabled(context) ? new NavSound(context) : null;
    }

    private NavSound(Context context) {
        pool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        pool.setOnLoadCompleteListener((p, id, status) -> {
            if (id == tick) tickLoaded = status == 0;
            else if (id == pop) popLoaded = status == 0;
        });
        tick = load(context, "nav_tick.wav", R.raw.nav_tick);
        pop = load(context, "open_pop.wav", R.raw.open_pop);
    }

    /** A WAV of that name next to config.json (adb push, to try another sound) replaces the built-in one. */
    private int load(Context context, String name, int builtIn) {
        java.io.File dir = Config.dir(context);
        java.io.File custom = dir == null ? null : new java.io.File(dir, name);
        return custom != null && custom.isFile() && custom.length() > 44
                ? pool.load(custom.getPath(), 1) : pool.load(context, builtIn, 1);
    }

    /** Focus landed on a new view: ticks only when a D-pad press just moved it. */
    void onFocusMoved() {
        long now = SystemClock.uptimeMillis();
        if (!tickLoaded || !TvFocus.movedByKey() || now - lastTickAt < MIN_GAP_MS) return;
        lastTickAt = now;
        pool.play(tick, TICK_VOLUME, TICK_VOLUME, 1, 0, 1f);
    }

    void onAppOpened() {
        if (popLoaded) pool.play(pop, POP_VOLUME, POP_VOLUME, 2, 0, 1f);
    }

    void release() {
        pool.release();
    }
}
