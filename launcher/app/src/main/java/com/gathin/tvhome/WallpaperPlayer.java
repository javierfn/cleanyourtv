package com.gathin.tvhome;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Displays and rotates a theme's local wallpaper images while Home is visible. */
final class WallpaperPlayer {
    private final FrameLayout root;
    private final ImageView image;
    private final int backgroundColor;
    private final List<File> files;
    private final long interval;
    private final String order;
    private final SharedPreferences preferences;
    private final ExecutorService decoder = Executors.newSingleThreadExecutor();
    private final Random random = new Random();
    private final int width;
    /** config wallpaper_true_color: ARGB_8888 instead of RGB_565. */
    private final Bitmap.Config bitmapConfig;
    private final int height;
    /** wallpaper_dim as an ARGB black, painted into each picture once instead of a full-screen shade view. */
    private final int dimColor;
    /** The picture on screen, released once the next one has been drawn. */
    private Bitmap current;
    private boolean active;
    private boolean closed;
    private boolean loading;
    private String displayed = "";
    /** wallpaper_captions: the shown picture's name from wallpapers/captions.txt; null when off or there are none. */
    private final TextView caption;
    private final Map<String, String> names;
    /** wallpaper_moods: the collection's groups by time of day; empty rotates through all of it. */
    private final Map<String, Set<String>> moods;
    /** config.xml latitude and longitude for the sun's times, and today's {sunrise, sunset} once worked out. */
    private final String latitude, longitude;
    private final Context context;
    private LocalDate sunDay;
    private int[] sun;

    WallpaperPlayer(Context context, FrameLayout root, Config config) {
        this.context = context.getApplicationContext();
        this.root = root;
        backgroundColor = config.backgroundColor;
        files = new ArrayList<>(config.wallpapers);
        interval = config.wallpaperRotationMinutes * 60_000L;
        bitmapConfig = config.wallpaperTrueColor ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
        // The dim is painted into the picture (decode), so no full-screen shade layer is drawn on every frame.
        dimColor = config.wallpaperDim > 0 ? (config.wallpaperDim * 255 / 100) << 24 : 0;
        order = config.wallpaperOrder;
        preferences = context.getSharedPreferences("wallpaper_rotation", Context.MODE_PRIVATE);
        moods = config.wallpaperMoods;
        latitude = moods.isEmpty() ? "" : context.getString(R.string.latitude);
        longitude = moods.isEmpty() ? "" : context.getString(R.string.longitude);
        // Playback state belongs to one collection. Changing the collection starts from its first
        // picture instead of resuming a filename that belonged to the previous one.
        String collection = collectionKey(files);
        if (!collection.equals(preferences.getString("collection", ""))) {
            preferences.edit().putString("collection", collection)
                    .remove("image").remove("changed").apply();
        }
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        width = metrics.widthPixels;
        height = metrics.heightPixels;
        image = new ImageView(context);
        image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        names = config.wallpaperCaptions && config.wallpaperEnabled && !files.isEmpty()
                ? WallpaperCaptions.read(files.get(0).getParentFile()) : Collections.emptyMap();
        caption = names.isEmpty() ? null : makeCaption(context, root);
        if (config.wallpaperEnabled && !files.isEmpty()) {
            root.addView(image, 0, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            showFirst();
        }
    }

    /**
     * The picture Home opens with is decoded right here, on the main thread, as the single wallpaper always was:
     * Home's first frame already has it instead of drawing the plain colour and the picture popping in later.
     * Only the later changes of a rotation decode in the background.
     */
    private void showFirst() {
        String previous = preferences.getString("image", "");
        List<File> playable = playable();
        File first = WallpaperRotation.find(playable, previous);
        if (first == null) first = WallpaperRotation.pick(playable, previous, order, random);
        while (first != null) {
            Bitmap bitmap = decode(first);
            if (bitmap != null) {
                show(bitmap);
                // The picture covers the whole screen: the root's plain colour under it is overdraw.
                root.setBackground(null);
                displayed = first.getName();
                showCaption(false);
                if (!displayed.equals(previous)) {
                    preferences.edit().putString("image", displayed)
                            .putLong("changed", System.currentTimeMillis()).apply();
                }
                return;
            }
            files.remove(first);   // unreadable or over 4K: try another, or keep the plain colour
            playable = playable();
            first = WallpaperRotation.pick(playable, previous, order, random);
        }
    }

    void start() {
        active = true;
        update();
        startMotion();
    }

    void stop() {
        active = false;
        stopMotion();
    }

    /**
     * Slow zoom towards points of the picture ("Ken Burns"), switched in Launcher settings (WallpaperMotion).
     * The motion is a chain of random legs, never a back-and-forth: zoom in towards a random point (the zoom centre
     * drifting a little), then ease most of the way out while the centre moves to the next random point, and so on.
     * The centre (pivot, as fractions of the screen) moves mostly while the picture is near its widest, so it reads
     * as a new approach rather than a pan. With the pivot inside the screen and a scale above 1 the picture always
     * covers the screen.
     */
    private android.animation.ValueAnimator motion;
    private float motionScale = 1.02f;
    private float pivotX = 0.5f;
    private float pivotY = 0.5f;

    private void startMotion() {
        if (!WallpaperMotion.enabled(context)) {
            resetMotion();
            return;
        }
        if (motion != null || image.getParent() == null) return;
        nextLeg();
    }

    private void nextLeg() {
        boolean zoomIn = motionScale < 1.08f;
        float fromScale = motionScale, fromX = pivotX, fromY = pivotY;
        float toScale, toX, toY;
        long duration;
        if (zoomIn) {
            toScale = 1.12f + (WallpaperMotion.MAX_SCALE - 1.12f) * random.nextFloat();
            toX = clamp(fromX + (random.nextFloat() - 0.5f) * 0.16f, 0.25f, 0.75f);
            toY = clamp(fromY + (random.nextFloat() - 0.5f) * 0.12f, 0.3f, 0.66f);
            duration = (long) (WallpaperMotion.CYCLE_MS * (0.6f + 0.2f * random.nextFloat()));
        } else {
            toScale = 1.02f + 0.03f * random.nextFloat();
            float[] p = randomFocus(fromX, fromY);
            toX = p[0];
            toY = p[1];
            duration = (long) (WallpaperMotion.CYCLE_MS * (0.45f + 0.15f * random.nextFloat()));
        }
        android.animation.ValueAnimator leg = android.animation.ValueAnimator.ofFloat(0f, 1f);
        leg.setDuration(duration);
        leg.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        leg.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            // Zooming out, the centre moves late, once the picture is nearly at its widest.
            float m = zoomIn ? v : v * v;
            applyMotion(fromScale + (toScale - fromScale) * v,
                    fromX + (toX - fromX) * m, fromY + (toY - fromY) * m);
        });
        leg.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (motion != leg) return;
                motion = null;
                nextLeg();
            }
        });
        motion = leg;
        leg.start();
    }

    private void applyMotion(float scale, float x, float y) {
        motionScale = scale;
        pivotX = x;
        pivotY = y;
        image.setTranslationX(0f);
        image.setTranslationY(0f);
        image.setPivotX(x * image.getWidth());
        image.setPivotY(y * image.getHeight());
        image.setScaleX(scale);
        image.setScaleY(scale);
    }

    /** A random focus point away from the edges, at least a little away from the current one. */
    private float[] randomFocus(float fromX, float fromY) {
        float x = 0.5f, y = 0.5f;
        for (int i = 0; i < 6; i++) {
            x = 0.28f + 0.44f * random.nextFloat();
            y = 0.32f + 0.32f * random.nextFloat();
            if (Math.abs(x - fromX) + Math.abs(y - fromY) > 0.15f) break;
        }
        return new float[] {x, y};
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    /** A new picture: start from its widest view at a random focus point. */
    private void nextRoute() {
        float[] p = randomFocus(pivotX, pivotY);
        boolean running = motion != null;
        stopMotion();
        motionScale = 1.02f;
        pivotX = p[0];
        pivotY = p[1];
        if (running) startMotion();
    }

    private void stopMotion() {
        if (motion == null) return;
        android.animation.ValueAnimator running = motion;
        motion = null; // first, so the cancelled leg does not chain the next one
        running.cancel();
    }

    /** Settings changed while Home was hidden: start or stop the motion on the next start(). */
    void resetMotion() {
        stopMotion();
        image.setScaleX(1f);
        image.setScaleY(1f);
        image.setTranslationX(0f);
        image.setTranslationY(0f);
    }

    /** Drops the wallpaper texture when Home is hidden and Android asks the process to free memory. */
    void releaseForMemoryPressure() {
        if (current == null) return;
        image.setImageDrawable(null);
        // The renderer may still have a queued frame that references this hardware bitmap. Drop our references and
        // let Android reclaim its native buffer after rendering releases it instead of recycling it mid-frame.
        current = null;
        displayed = "";
        root.setBackgroundColor(backgroundColor);
    }

    /** The pictures the rotation picks from now: wallpaper_moods' group for the time of day, or the whole collection. */
    private List<File> playable() {
        if (moods.isEmpty()) return files;
        LocalDateTime now = LocalDateTime.now();
        if (!now.toLocalDate().equals(sunDay)) {
            sunDay = now.toLocalDate();
            sun = WallpaperMood.sun(sunDay, latitude, longitude, ZoneId.systemDefault());
        }
        String mood = WallpaperMood.timeOfDay(now.getHour() * 60 + now.getMinute(), sun[0], sun[1]);
        // With the Brightness add-on installed, its night mode (schedule or moon button) decides night instead.
        Boolean night = NightState.query(context);
        if (night != null) mood = night ? WallpaperMood.NIGHT
                : (WallpaperMood.NIGHT.equals(mood) ? WallpaperMood.MORNING : mood);
        return WallpaperMood.select(files, moods, mood);
    }

    /** For the Control Center's glass: the picture on screen and the view that shows it (with its zoom). */
    Bitmap currentBitmap() {
        return current;
    }

    ImageView imageView() {
        return image;
    }

    void close() {
        active = false;
        closed = true;
        decoder.shutdownNow();
    }

    void update() {
        if (!active || closed || loading || files.isEmpty()) return;
        String previous = preferences.getString("image", "");
        // A picture outside the time of day's group (the group changed) is replaced at once, not after the interval.
        List<File> playable = playable();
        File selected = WallpaperRotation.find(playable, previous);
        long changed = preferences.getLong("changed", 0);
        if (selected == null || WallpaperRotation.due(changed, System.currentTimeMillis(), interval)) {
            selected = WallpaperRotation.pick(playable, previous, order, random);
        }
        if (selected == null || selected.getName().equals(displayed)) return;
        File next = selected;
        loading = true;
        decoder.execute(() -> {
            Bitmap bitmap = decode(next);
            root.post(() -> {
                loading = false;
                if (closed || !active) {
                    if (bitmap != null) bitmap.recycle();
                    return;
                }
                if (bitmap == null) {
                    files.remove(next);
                    Log.w("TvHome.Wallpaper", "Skipping unreadable image: " + next.getName());
                    update();
                    return;
                }
                show(bitmap);
                displayed = next.getName();
                showCaption(true);
                if (!displayed.equals(previous) || changed == 0) {
                    preferences.edit().putString("image", displayed)
                            .putLong("changed", System.currentTimeMillis()).apply();
                }
            });
        });
    }

    /**
     * Small light text under the top-right buttons, above everything else and never focusable. It sits where the
     * top bar ends in every layout, so it does not cover the clock, the stats line or the tiles.
     */
    private static TextView makeCaption(Context context, FrameLayout root) {
        float dp = context.getResources().getDisplayMetrics().density;
        // No offscreen layer for the fade: on this TV the layer clipped the blurred shadow into a visible box.
        TextView text = new TextView(context) {
            @Override
            public boolean hasOverlappingRendering() {
                return false;
            }
        };
        text.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        text.setTextColor(0xE6FFFFFF);
        text.setShadowLayer(2 * dp, 0, dp, 0x99000000);
        text.setSingleLine(true);
        text.setEllipsize(TextUtils.TruncateAt.END);
        text.setMaxWidth(Math.round(520 * dp));
        text.setFocusable(false);
        text.setAlpha(0f);
        try {
            text.setTypeface(UiFont.get(context, R.string.font_regular));
        } catch (Exception ignored) {
            // the system font is fine
        }
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP | Gravity.END);
        params.topMargin = Math.round(110 * dp);
        params.setMarginEnd(Math.round(62 * dp));
        root.addView(text, params);
        return text;
    }

    /**
     * Puts the caption on the same line as another text on the other side (the weather under the clock): their
     * centres level, whatever the header's layout. Follows it when it moves; nothing changes while it is hidden.
     */
    void alignCaptionWith(View other) {
        if (caption == null || other == null) return;
        View.OnLayoutChangeListener align = (v, l, t, r, b, ol, ot, orr, ob) -> {
            if (other.getVisibility() != View.VISIBLE || caption.getHeight() == 0) return;
            int[] at = new int[2], rootAt = new int[2], capAt = new int[2];
            other.getLocationInWindow(at);
            root.getLocationInWindow(rootAt);
            caption.getLocationInWindow(capAt);
            float otherCentre = at[1] - rootAt[1] + other.getHeight() / 2f;
            float captionCentre = capAt[1] - rootAt[1] - caption.getTranslationY() + caption.getHeight() / 2f;
            caption.setTranslationY(otherCentre - captionCentre);
        };
        other.addOnLayoutChangeListener(align);
        caption.addOnLayoutChangeListener(align);
    }

    /** The displayed picture's name, faded across a change; a picture without a name hides the caption. */
    private void showCaption(boolean fade) {
        if (caption == null) return;
        String name = names.get(displayed);
        caption.animate().cancel();
        if (name == null) {
            caption.setAlpha(0f);
            return;
        }
        caption.setContentDescription(name);
        if (!fade || caption.getAlpha() == 0f) {
            caption.setText(name);
            caption.setAlpha(1f);
            return;
        }
        caption.animate().alpha(0f).setDuration(250).withEndAction(() -> {
            caption.setText(name);
            caption.animate().alpha(1f).setDuration(350).start();
        }).start();
    }

    /** Names plus sizes: enough to notice a different collection without hashing every image. */
    private static String collectionKey(List<File> images) {
        StringBuilder key = new StringBuilder();
        for (File file : images) key.append(file.getName()).append(':').append(file.length()).append(';');
        return key.toString();
    }

    /** Puts a picture on screen and releases the previous one after a frame has drawn the new one. */
    private void show(Bitmap bitmap) {
        Bitmap old = current;
        current = bitmap;
        // The wallpaper covers the whole root; avoid drawing the fallback colour under it.
        root.setBackground(null);
        image.setImageBitmap(bitmap);
        if (old != bitmap) nextRoute();
        if (old != null && old != bitmap) image.postDelayed(old::recycle, 500);
    }

    private Bitmap decode(File file) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getPath(), options);
            int width = options.outWidth, height = options.outHeight;
            if (width <= 0 || height <= 0) return null;
            // With the motion on, decode with room for its deepest zoom so the zoomed picture keeps real detail.
            float room = WallpaperMotion.enabled(context) ? WallpaperMotion.MAX_SCALE : 1f;
            int targetW = Math.round(this.width * room), targetH = Math.round(this.height * room);
            int sample = WallpaperSize.sampleSize(width, height, targetW, targetH);
            if (!WallpaperSize.accepted(width / sample, height / sample)) return null;
            options.inJustDecodeBounds = false;
            options.inSampleSize = sample;
            int[] scale = WallpaperSize.coverScale(width / sample, height / sample, targetW, targetH);
            if (scale != null) {
                options.inScaled = true;
                options.inDensity = scale[0];
                options.inTargetDensity = scale[1];
            }
            options.inPreferredConfig = bitmapConfig;
            options.inMutable = dimColor != 0;
            Bitmap decoded = BitmapFactory.decodeFile(file.getPath(), options);
            if (decoded == null) return null;
            if (dimColor != 0) new Canvas(decoded).drawColor(dimColor);
            // One copy in GPU memory instead of a software bitmap plus its texture; the decode stays off the UI
            // thread for rotations, and Home's return no longer uploads 4-8 MB to the GPU.
            Bitmap gpu = decoded.copy(Bitmap.Config.HARDWARE, false);
            if (gpu == null) return decoded;
            decoded.recycle();
            return gpu;
        } catch (Exception | OutOfMemoryError failure) {
            return null;
        }
    }
}
