package com.gathin.tvhome;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/**
 * liquid_glass (config.json): one piece of tvOS 26-style glass for a Control Center tile. The wallpaper under the tile
 * is blurred and lifted, and near the edge it bends like a lens (sampled from further in, a little apart per colour);
 * a bright rim lights the top left edge and fades towards the bottom right. All of it is drawn once, when the panel
 * opens, into one small bitmap: nothing is worked out per frame.
 */
final class LiquidGlass extends Drawable {
    /** Work at half the screen's pixels: the picture is soft anyway, and the rim is drawn sharp on top. */
    private static final float WORK = 0.5f;

    private final Bitmap glass;
    private final float radius;
    private final float rimWidth;
    private final boolean circle;
    private final int tint;
    private final Paint picture = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final Matrix fit = new Matrix();

    /**
     * @param source the blurred wallpaper around the tile: the tile's w x h plus margin on every side, at WORK scale
     * @param radius the tile's corner radius in pixels, or -1 for a circle
     * @param tint   a fill over the glass (brighter for something switched on)
     */
    LiquidGlass(Bitmap source, int margin, int w, int h, float radius, float density, int tint) {
        this.circle = radius < 0;
        this.radius = circle ? Math.min(w, h) / 2f : radius;
        this.rimWidth = Math.max(1f, 1.2f * density);
        this.tint = tint;
        this.glass = source == null ? null : bend(source, margin, w, h, this.radius, density);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(rimWidth);
        shine.setStyle(Paint.Style.STROKE);
        shine.setStrokeWidth(rimWidth * 3f);
        fill.setColor(tint);
    }

    /**
     * The lens: inside a band along the edge, each pixel takes its colour from further in, more so the closer it is
     * to the edge, so the picture there looks magnified and curved; red and blue bend a little more and less than
     * green. The band is a bit under half of the tile's short side.
     */
    private static Bitmap bend(Bitmap src, int margin, int w, int h, float radius, float density) {
        int ow = Math.max(4, Math.round(w * WORK)), oh = Math.max(4, Math.round(h * WORK));
        int sw = src.getWidth(), sh = src.getHeight();
        int[] in = new int[sw * sh];
        src.getPixels(in, 0, sw, 0, 0, sw, sh);
        int[] out = new int[ow * oh];
        float m = margin * WORK, r = radius * WORK;
        float hx = ow / 2f, hy = oh / 2f;
        float band = Math.min(Math.min(ow, oh) * 0.42f, 30 * density * WORK);
        float pull = band * 1.5f;
        for (int y = 0; y < oh; y++) {
            for (int x = 0; x < ow; x++) {
                float px = x + 0.5f - hx, py = y + 0.5f - hy;
                float d = -sdf(px, py, hx, hy, r);   // distance inside the edge
                float sx = x + 0.5f + m, sy = y + 0.5f + m;
                int rr, gg, bb;
                if (d < band) {
                    float t = 1f - Math.max(0f, d) / band;
                    float k = t * t * pull;
                    // Outward normal from the shape's gradient.
                    float e = 0.75f;
                    float nx = sdf(px + e, py, hx, hy, r) - sdf(px - e, py, hx, hy, r);
                    float ny = sdf(px, py + e, hx, hy, r) - sdf(px, py - e, hx, hy, r);
                    float len = (float) Math.sqrt(nx * nx + ny * ny);
                    if (len > 0) { nx /= len; ny /= len; }
                    int cr = sample(in, sw, sh, sx - nx * k * 1.08f, sy - ny * k * 1.08f);
                    int cg = sample(in, sw, sh, sx - nx * k, sy - ny * k);
                    int cb = sample(in, sw, sh, sx - nx * k * 0.92f, sy - ny * k * 0.92f);
                    // The curved edge also catches more light.
                    float lift = 1f + 0.18f * t;
                    rr = clamp(((cr >> 16) & 0xFF) * lift);
                    gg = clamp(((cg >> 8) & 0xFF) * lift);
                    bb = clamp((cb & 0xFF) * lift);
                } else {
                    int c = sample(in, sw, sh, sx, sy);
                    rr = (c >> 16) & 0xFF; gg = (c >> 8) & 0xFF; bb = c & 0xFF;
                }
                out[y * ow + x] = 0xFF000000 | (rr << 16) | (gg << 8) | bb;
            }
        }
        return Bitmap.createBitmap(out, ow, oh, Bitmap.Config.ARGB_8888);
    }

    /** Signed distance to a rounded rectangle centred on 0 with half sizes hx, hy: negative inside. */
    private static float sdf(float x, float y, float hx, float hy, float r) {
        float qx = Math.abs(x) - (hx - r), qy = Math.abs(y) - (hy - r);
        float ox = Math.max(qx, 0f), oy = Math.max(qy, 0f);
        return (float) Math.sqrt(ox * ox + oy * oy) + Math.min(Math.max(qx, qy), 0f) - r;
    }

    private static int sample(int[] px, int w, int h, float x, float y) {
        int ix = Math.max(0, Math.min(w - 1, (int) x)), iy = Math.max(0, Math.min(h - 1, (int) y));
        return px[iy * w + ix];
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }

    @Override
    protected void onBoundsChange(Rect b) {
        rect.set(b);
        path.reset();
        if (circle) path.addOval(rect, Path.Direction.CW);
        else path.addRoundRect(rect, radius, radius, Path.Direction.CW);
        if (glass != null) {
            BitmapShader shader = new BitmapShader(glass, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            fit.setScale(b.width() / (float) glass.getWidth(), b.height() / (float) glass.getHeight());
            fit.postTranslate(b.left, b.top);
            shader.setLocalMatrix(fit);
            picture.setShader(shader);
        }
        // The rim: bright at the top left, nearly gone at the bottom right, and a little light again at the very bottom.
        rim.setShader(new LinearGradient(b.left, b.top, b.right, b.bottom,
                new int[]{0xE6FFFFFF, 0x40FFFFFF, 0x14FFFFFF, 0x59FFFFFF}, new float[]{0f, 0.35f, 0.7f, 1f},
                Shader.TileMode.CLAMP));
        // A wider, softer glow just inside the top edge.
        shine.setShader(new LinearGradient(0, b.top, 0, b.top + b.height() * 0.5f,
                new int[]{0x33FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas canvas) {
        if (glass != null) canvas.drawPath(path, picture);
        if ((tint >>> 24) != 0) canvas.drawPath(path, fill);
        int save = canvas.save();
        canvas.clipPath(path);
        canvas.drawPath(path, shine);
        canvas.restoreToCount(save);
        float in = rimWidth / 2f;
        if (circle) canvas.drawOval(rect.left + in, rect.top + in, rect.right - in, rect.bottom - in, rim);
        else canvas.drawRoundRect(rect.left + in, rect.top + in, rect.right - in, rect.bottom - in, radius - in, radius - in, rim);
    }

    @Override public void setAlpha(int alpha) { picture.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter cf) { picture.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
