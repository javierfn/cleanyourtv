package com.gathin.tvhome;

import android.animation.Animator;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.PathInterpolator;

/**
 * tvOS-style focus (config tvos_focus): the focused tile lifts onto a shadow, leans a few degrees towards the way
 * the D-pad moved and settles level, while a specular band sweeps across it and a soft glow stays at its top.
 * Everything is view properties and one gradient drawable, so it runs on the render thread with no bitmaps.
 * Each call cancels the animations before it, so a fast run of key presses retargets instead of stacking.
 */
final class TvFocus {
    private TvFocus() { }

    /**
     * The shadow a focused tile lifts onto. The lift is only this elevation: moving the tile up as well made it look
     * as if it grew upwards, where tvOS grows it evenly from its centre.
     */
    static final float SHADOW_DP = 28f;
    /**
     * How much a focused tile grows. With no highlight around it the growth is the focus, so it is a clear 25%: the
     * columns' gap and the dock band still leave room for it (one tile grows at a time).
     */
    static final float SCALE = 1.25f;
    private static final float TILT_DEG = 9f;
    private static final long FOCUS_MS = 260;
    private static final long UNFOCUS_MS = 180;
    private static final long SETTLE_MS = 560;
    /** A focus change this soon after a D-pad press was caused by it; later ones (reload, a menu closing) have no lean. */
    static final long KEY_WINDOW_MS = 400;

    private static final Interpolator FOCUS_EASE = new PathInterpolator(0.25f, 1f, 0.5f, 1f);
    private static final Interpolator UNFOCUS_EASE = new DecelerateInterpolator(1.5f);
    private static final Interpolator SETTLE_EASE = new DecelerateInterpolator(1.8f);

    private static int lastDirection;
    private static long lastKeyAt;

    /** MainActivity.dispatchKeyEvent: remembers the direction of every D-pad press, before focus moves. */
    static void noteKey(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT: case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP: case KeyEvent.KEYCODE_DPAD_DOWN:
                lastDirection = event.getKeyCode();
                lastKeyAt = SystemClock.uptimeMillis();
                break;
            default:
        }
    }

    /** True when a D-pad press just now moved focus. */
    static boolean movedByKey() {
        return SystemClock.uptimeMillis() - lastKeyAt < KEY_WINDOW_MS;
    }

    private static int direction() {
        return movedByKey() ? lastDirection : 0;
    }

    /**
     * Focus or unfocus a tile: scale from its centre, shadow (px), the lean and, with a {@link Shine} as its
     * foreground, the sweep.
     */
    static void apply(View card, boolean focused, float scale, float elevation) {
        float density = card.getResources().getDisplayMetrics().density;
        card.setCameraDistance(12000f * density);
        card.setPivotX(card.getWidth() / 2f);
        card.setPivotY(card.getHeight() / 2f);
        float s = focused ? scale : 1f;
        card.animate().scaleX(s).scaleY(s)
                .translationY(0f)
                .translationZ(focused ? elevation : 0f)
                .setDuration(focused ? FOCUS_MS : UNFOCUS_MS)
                .setInterpolator(focused ? FOCUS_EASE : UNFOCUS_EASE).setStartDelay(0)
                .start();

        int dir = focused ? direction() : 0;
        float rx = 0f, ry = 0f;
        switch (dir) {
            case KeyEvent.KEYCODE_DPAD_RIGHT: ry = -TILT_DEG; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: ry = TILT_DEG; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: rx = TILT_DEG; break;
            case KeyEvent.KEYCODE_DPAD_UP: rx = -TILT_DEG; break;
            default:
        }
        tilt(card, rx, ry, focused);
        if (card.getForeground() instanceof Shine) ((Shine) card.getForeground()).play(focused, dir);
    }

    /** A rebound tile: its resting state with no animation. */
    static void rest(View card, boolean focused) {
        Object prev = card.getTag(R.id.tv_focus_tilt);
        if (prev instanceof Animator) ((Animator) prev).cancel();
        card.setRotationX(0f);
        card.setRotationY(0f);
        if (card.getForeground() instanceof Shine) ((Shine) card.getForeground()).snap(focused);
    }

    /** Lean to (rx, ry) in the first fifth, then a slow return to level; unfocus just levels out. */
    private static void tilt(View card, float rx, float ry, boolean focused) {
        Object prev = card.getTag(R.id.tv_focus_tilt);
        if (prev instanceof Animator) ((Animator) prev).cancel();
        float cx = card.getRotationX(), cy = card.getRotationY();
        ObjectAnimator a;
        if (focused && (rx != 0f || ry != 0f)) {
            a = ObjectAnimator.ofPropertyValuesHolder(card,
                    PropertyValuesHolder.ofKeyframe(View.ROTATION_X, lean(cx, rx)),
                    PropertyValuesHolder.ofKeyframe(View.ROTATION_Y, lean(cy, ry)));
            a.setDuration(SETTLE_MS);
        } else {
            if (cx == 0f && cy == 0f) return;
            a = ObjectAnimator.ofPropertyValuesHolder(card,
                    PropertyValuesHolder.ofFloat(View.ROTATION_X, cx, 0f),
                    PropertyValuesHolder.ofFloat(View.ROTATION_Y, cy, 0f));
            a.setDuration(UNFOCUS_MS);
            a.setInterpolator(UNFOCUS_EASE);
        }
        card.setTag(R.id.tv_focus_tilt, a);
        a.start();
    }

    private static Keyframe[] lean(float from, float to) {
        Keyframe out = Keyframe.ofFloat(0.2f, to);
        out.setInterpolator(FOCUS_EASE);
        Keyframe back = Keyframe.ofFloat(1f, 0f);
        back.setInterpolator(SETTLE_EASE);
        return new Keyframe[]{Keyframe.ofFloat(0f, from), out, back};
    }

    /**
     * The light on a focused tile, set as its foreground: a white band that sweeps once across it in the D-pad's
     * direction (top-left to bottom-right when focus did not move by key), and a faint glow left at the top.
     * Clips itself to the tile's rounded corners, so it also works on views that do not clip to their outline.
     */
    static final class Shine extends Drawable {
        private static final int BAND_PEAK = 0x4DFFFFFF;
        private static final int GLOW_TOP = 0x1AFFFFFF;
        private final Paint band = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Matrix matrix = new Matrix();
        private final Path clip = new Path();
        private final RectF rect = new RectF();
        private final float corner;
        private float bandWidth;
        /** Sweep progress 0..1, or negative for no band. */
        private float sweep = -1f;
        private float ux = 1f, uy = 0f;
        private int glowAlpha;
        private ValueAnimator anim;

        Shine(float cornerPx) {
            corner = cornerPx;
        }

        void play(boolean focused, int dir) {
            if (anim != null) anim.cancel();
            if (!focused) {
                sweep = -1f;
                glowAlpha = 0;
                invalidateSelf();
                return;
            }
            switch (dir) {
                case KeyEvent.KEYCODE_DPAD_RIGHT: setDirection(1f, 0.25f); break;
                case KeyEvent.KEYCODE_DPAD_LEFT: setDirection(-1f, 0.25f); break;
                case KeyEvent.KEYCODE_DPAD_DOWN: setDirection(0.25f, 1f); break;
                case KeyEvent.KEYCODE_DPAD_UP: setDirection(0.25f, -1f); break;
                default: setDirection(1f, 0.6f);
            }
            final int from = glowAlpha;
            anim = ValueAnimator.ofFloat(0f, 1f);
            anim.setDuration(SETTLE_MS);
            anim.setInterpolator(SETTLE_EASE);
            anim.addUpdateListener(a -> {
                float t = (float) a.getAnimatedValue();
                sweep = t < 1f ? t : -1f;
                glowAlpha = Math.round(from + (255 - from) * Math.min(1f, t * 2f));
                invalidateSelf();
            });
            anim.start();
        }

        void snap(boolean focused) {
            if (anim != null) anim.cancel();
            sweep = -1f;
            glowAlpha = focused ? 255 : 0;
            invalidateSelf();
        }

        private void setDirection(float x, float y) {
            float len = (float) Math.hypot(x, y);
            ux = x / len;
            uy = y / len;
        }

        @Override
        protected void onBoundsChange(Rect b) {
            rect.set(b);
            clip.reset();
            clip.addRoundRect(rect, corner, corner, Path.Direction.CW);
            bandWidth = Math.max(1f, Math.min(b.width(), b.height()) * 0.9f);
            band.setShader(new LinearGradient(0f, 0f, bandWidth, 0f,
                    new int[]{0x00FFFFFF, BAND_PEAK, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            float r = Math.max(1f, b.width() * 0.75f);
            glow.setShader(new RadialGradient(b.exactCenterX(), b.top - b.height() * 0.15f, r,
                    new int[]{GLOW_TOP, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        }

        @Override
        public void draw(Canvas canvas) {
            if (glowAlpha == 0 && sweep < 0f) return;
            Rect b = getBounds();
            int save = canvas.save();
            canvas.clipPath(clip);
            if (glowAlpha > 0) {
                glow.setAlpha(glowAlpha);
                canvas.drawRect(rect, glow);
            }
            if (sweep >= 0f) {
                // The tile's extent along the sweep, so the band enters fully outside one edge and leaves the other.
                float lo = Math.min(0f, b.width() * ux) + Math.min(0f, b.height() * uy);
                float hi = Math.max(0f, b.width() * ux) + Math.max(0f, b.height() * uy);
                float at = lo - bandWidth + (hi - lo + bandWidth) * sweep;
                matrix.setRotate((float) Math.toDegrees(Math.atan2(uy, ux)));
                matrix.preTranslate(at, 0f);
                matrix.postTranslate(b.left, b.top);
                band.getShader().setLocalMatrix(matrix);
                band.setAlpha(Math.round(255 * (1f - sweep * 0.6f)));
                canvas.drawRect(rect, band);
            }
            canvas.restoreToCount(save);
        }

        @Override public void setAlpha(int alpha) { }
        @Override public void setColorFilter(ColorFilter filter) { }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
