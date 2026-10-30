package com.gathin.tvhome;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * control_center (config.json): a tvOS-style Control Center over Home, opened from one round button at the top right.
 * Frosted glass tiles: Power off (large), network, night mode, inputs and the sleep timer, then round buttons for the
 * VPN and settings. Entradas and Ajustes open in the panel's place, on the same glass (frost()). The TV has no window blur, so the glass is made here: the panel holds a blurred copy of
 * the wallpaper, lined up with the one behind it.
 */
final class ControlCenter {
    /** What the tiles do and show; MainActivity fills it in. */
    interface Actions {
        void powerOff();
        void network();
        String networkLabel();
        void nightToggle();
        /** True at night, false by day, null when there is no night-mode add-on. */
        Boolean night();
        void timer();
        boolean hasTimer();
        void inputs();
        void vpn();
        /** -1 no VPN app, 0 off, 1 up. */
        int vpnState();
        void settings();
    }

    private static final int GLASS = 0x1FFFFFFF;
    private static final int TILE = 0x1AFFFFFF;
    private static final int TILE_STROKE = 0x33FFFFFF;
    private static final int FOCUS = 0xFFF4F4F6;
    private static final int INK = 0xFF111113;

    private final MainActivity activity;
    private final FrameLayout root;
    private final Actions actions;
    private final float dp;
    private FrameLayout overlay;
    private View returnFocus;
    /** Where the panel was last open, in root coordinates: submenus (Entradas, Ajustes) open in its place. */
    private final android.graphics.Rect lastPanel = new android.graphics.Rect();
    private ImageView lastWallpaper;
    private Bitmap lastPicture;

    android.graphics.Rect lastPanel() {
        return lastPanel;
    }

    /** The panel's width, for menus that take its measure without it having been opened (the app tile menu). */
    int panelWidth() {
        return px(340);
    }

    /** The wallpaper that frost() blurs, for a menu opened without the panel. */
    void useWallpaper(ImageView wallpaper, Bitmap picture) {
        lastWallpaper = wallpaper;
        lastPicture = picture;
    }

    ControlCenter(MainActivity activity, FrameLayout root, Actions actions) {
        this.activity = activity;
        this.root = root;
        this.actions = actions;
        this.dp = activity.getResources().getDisplayMetrics().density;
    }

    boolean isShowing() {
        return overlay != null;
    }

    /** Opens over Home, the wallpaper (and its zoom, if it moves) blurred behind the tiles. */
    void show(View from, ImageView wallpaper, Bitmap picture) {
        if (overlay != null) return;
        returnFocus = from;
        Context c = activity;
        overlay = new FrameLayout(c);
        overlay.setClickable(true);
        overlay.setBackgroundColor(0x14000000);
        overlay.setOnClickListener(v -> hide());

        int width = px(340);
        // Just under the button and lined up with its right edge, so the button stays in sight.
        int[] at = new int[2];
        int[] rootAt = new int[2];
        from.getLocationInWindow(at);
        root.getLocationInWindow(rootAt);
        int top = at[1] - rootAt[1] + from.getHeight() + dpx(10);
        int right = Math.max(dpx(12), root.getWidth() - (at[0] - rootAt[0] + from.getWidth()));
        lastPanel.set(root.getWidth() - right - width, top, root.getWidth() - right, top);
        lastWallpaper = wallpaper;
        lastPicture = picture;
        FrameLayout panel = new FrameLayout(c);
        panel.setClickable(true);
        panel.setClipToOutline(true);
        float radius = px(26);
        panel.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        panel.setElevation(px(18));
        FrameLayout.LayoutParams plp = new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.END);
        plp.topMargin = top;
        plp.rightMargin = right;
        overlay.addView(panel, plp);

        // The glass: the same picture, at the same place and zoom as the wallpaper, blurred and darkened a little.
        if (picture != null && Build.VERSION.SDK_INT >= 31) {
            ImageView blur = new ImageView(c);
            blur.setScaleType(ImageView.ScaleType.CENTER_CROP);
            blur.setImageBitmap(picture);
            int sw = root.getWidth(), sh = root.getHeight();
            panel.addView(blur, new FrameLayout.LayoutParams(sw, sh));
            int left = sw - right - width;
            blur.setTranslationX(-left);
            blur.setTranslationY(-top);
            if (wallpaper != null) {
                blur.setPivotX(wallpaper.getPivotX());
                blur.setPivotY(wallpaper.getPivotY());
                blur.setScaleX(wallpaper.getScaleX());
                blur.setScaleY(wallpaper.getScaleY());
            }
            // tvOS-like frost: a wide blur with the colours lifted, so the glass looks lit rather than grey.
            android.graphics.ColorMatrix vivid = new android.graphics.ColorMatrix();
            vivid.setSaturation(1.6f);
            android.graphics.ColorMatrix lift = new android.graphics.ColorMatrix(new float[] {
                    1.08f, 0, 0, 0, 14, 0, 1.08f, 0, 0, 14, 0, 0, 1.08f, 0, 14, 0, 0, 0, 1, 0});
            vivid.postConcat(lift);
            blur.setRenderEffect(RenderEffect.createChainEffect(
                    RenderEffect.createColorFilterEffect(new android.graphics.ColorMatrixColorFilter(vivid)),
                    RenderEffect.createBlurEffect(dpx(22), dpx(22), Shader.TileMode.CLAMP)));
        }
        View tint = new View(c);
        GradientDrawable glass = new GradientDrawable();
        glass.setColor(picture != null && Build.VERSION.SDK_INT >= 31 ? GLASS : 0xE6202226);
        glass.setCornerRadius(radius);
        glass.setStroke(Math.max(1, Math.round(dp)), 0x40FFFFFF);
        tint.setBackground(glass);
        panel.addView(tint, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout content = new LinearLayout(c);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = px(14);
        content.setPadding(pad, pad, pad, pad);
        panel.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int gap = px(10);
        int inner = width - 2 * pad;
        int col = (inner - gap) / 2;
        int pill = px(62);
        GridLayout grid = new GridLayout(c);
        grid.setColumnCount(2);
        // A focused tile grows a little; let it, instead of cutting its edge off.
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        content.setClipChildren(false);
        content.setClipToPadding(false);
        content.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View power = bigTile(R.drawable.ic_atv_power, activity.getString(R.string.tv_power_off_short), actions::powerOff);
        grid.addView(power, cell(0, 0, 2, col, 2 * pill + gap, 0, gap));
        grid.addView(pillTile(R.drawable.ic_wifi, activity.getString(R.string.cc_network), actions.networkLabel(),
                actions::network), cell(0, 1, 1, col, pill, gap, gap));
        Boolean night = actions.night();
        int row = 1;
        if (night != null) {
            grid.addView(pillTile(night ? R.drawable.ic_night : R.drawable.ic_day, activity.getString(R.string.cc_night),
                    activity.getString(night ? R.string.cc_on : R.string.cc_off), () -> {
                        actions.nightToggle();
                        hide();
                    }), cell(row, 1, 1, col, pill, gap, gap));
            row++;
        }
        // Entradas, wide with its name; then the sleep timer when it is installed.
        grid.addView(pillTile(R.drawable.ic_input, activity.getString(R.string.inputs),
                activity.getString(R.string.cc_inputs_detail), actions::inputs), cell(2, 0, 1, col, pill, 0, gap));
        int vpn = actions.vpnState();
        int slot = 3;   // cells after Entradas (row 2, column 0), filled left to right
        if (actions.hasTimer()) {
            grid.addView(pillTile(R.drawable.ic_cc_timer, activity.getString(R.string.cc_timer),
                    activity.getString(R.string.cc_timer_detail), actions::timer),
                    cell(slot / 2 + 1, slot % 2, 1, col, pill, slot % 2 == 0 ? 0 : gap, gap));
        }

        LinearLayout rounds = new LinearLayout(c);
        rounds.setOrientation(LinearLayout.HORIZONTAL);
        rounds.setGravity(Gravity.CENTER_VERTICAL);
        rounds.setClipChildren(false);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = px(4);
        content.addView(rounds, rlp);
        // The VPN, wide like Entradas and under it, on the same line as Settings: its lock red while off, green while up.
        if (vpn >= 0) {
            View v = pillTile(R.drawable.ic_lock, activity.getString(R.string.cc_vpn),
                    activity.getString(vpn == 1 ? R.string.cc_on : R.string.cc_off), actions::vpn);
            int lock = vpn == 1 ? 0xFF30D158 : 0xFFFF6B5E;
            ((ImageView) ((ViewGroup) v).getChildAt(0)).setImageTintList(
                    new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}}, new int[]{lock, lock}));
            rounds.addView(v, new LinearLayout.LayoutParams(col, pill));
        }
        // Settings at the bottom right, on its own side.
        rounds.addView(new View(c), new LinearLayout.LayoutParams(0, 1, 1f));
        addRound(rounds, R.drawable.ic_settings, activity.getString(R.string.settings), 0, actions::settings);

        // The panel is as tall as its tiles; the full-screen blur inside it must not stretch it.
        content.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        plp.height = content.getMeasuredHeight();
        panel.setLayoutParams(plp);
        root.addView(overlay, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        // Keep the remote inside the panel: nothing under it takes focus while it is open.
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child != overlay && child instanceof ViewGroup) ((ViewGroup) child).setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        }
        panel.setAlpha(0f);
        panel.setTranslationY(-px(8));
        panel.animate().alpha(1f).translationY(0f).setDuration(200).start();
        power.requestFocus();
    }

    void hide() {
        if (overlay == null) return;
        for (int i = 0; i < root.getChildCount(); i++) {
            View child = root.getChildAt(i);
            if (child != overlay && child instanceof ViewGroup) ((ViewGroup) child).setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        }
        root.removeView(overlay);
        overlay = null;
        if (returnFocus != null) returnFocus.requestFocus();
    }

    /**
     * The same frosted glass as a background for a submenu panel shown at (left, top) in root coordinates, w by h:
     * the wallpaper under it, blurred and lifted, under a light frost and a thin edge. Null when there is no picture.
     */
    android.graphics.drawable.Drawable frost(int left, int top, int w, int h) {
        Bitmap picture = lastPicture;
        ImageView view = lastWallpaper;
        if (picture == null || view == null || w <= 0 || h <= 0) return null;
        try {
            Bitmap soft = picture.getConfig() == Bitmap.Config.HARDWARE ? picture.copy(Bitmap.Config.ARGB_8888, false) : picture;
            if (soft == null) return null;
            // Root point -> wallpaper view point (undo its zoom about the pivot) -> picture point (undo centre-crop).
            android.graphics.Matrix toPicture = new android.graphics.Matrix();
            view.getImageMatrix().invert(toPicture);
            float[] pts = {left, top, left + w, top + h};
            for (int i = 0; i < 4; i += 2) {
                pts[i] = view.getPivotX() + (pts[i] - view.getPivotX()) / view.getScaleX();
                pts[i + 1] = view.getPivotY() + (pts[i + 1] - view.getPivotY()) / view.getScaleY();
            }
            toPicture.mapPoints(pts);
            int x0 = Math.max(0, (int) pts[0]), y0 = Math.max(0, (int) pts[1]);
            int x1 = Math.min(soft.getWidth(), (int) pts[2]), y1 = Math.min(soft.getHeight(), (int) pts[3]);
            if (x1 - x0 < 4 || y1 - y0 < 4) return null;
            Bitmap crop = Bitmap.createBitmap(soft, x0, y0, x1 - x0, y1 - y0);
            // A wide, smooth blur: shrink to an eighth of the panel, box-blur that three times, and let the drawable's
            // filtering scale it back up (already soft, so no blocks show).
            Bitmap smooth = Bitmap.createScaledBitmap(crop, Math.max(4, w / 8), Math.max(4, h / 8), true);
            smooth = boxBlur(smooth.copy(Bitmap.Config.ARGB_8888, true), 3, 3);
            android.graphics.ColorMatrix vivid = new android.graphics.ColorMatrix();
            vivid.setSaturation(1.6f);
            android.graphics.ColorMatrix lift = new android.graphics.ColorMatrix(new float[] {
                    1.08f, 0, 0, 0, 14, 0, 1.08f, 0, 0, 14, 0, 0, 1.08f, 0, 14, 0, 0, 0, 1, 0});
            vivid.postConcat(lift);
            Bitmap out = Bitmap.createBitmap(smooth.getWidth(), smooth.getHeight(), Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(out);
            android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG);
            paint.setColorFilter(new android.graphics.ColorMatrixColorFilter(vivid));
            canvas.drawBitmap(smooth, 0, 0, paint);
            float r = px(26);
            android.graphics.drawable.BitmapDrawable bg = new android.graphics.drawable.BitmapDrawable(activity.getResources(), out);
            bg.setFilterBitmap(true);
            GradientDrawable glass = new GradientDrawable();
            glass.setColor(GLASS);
            glass.setCornerRadius(r);
            glass.setStroke(Math.max(1, Math.round(dp)), 0x40FFFFFF);
            android.graphics.drawable.LayerDrawable layers = new android.graphics.drawable.LayerDrawable(
                    new android.graphics.drawable.Drawable[]{bg, glass});
            return new RoundedDrawable(layers, r);
        } catch (RuntimeException | OutOfMemoryError e) {
            return null;
        }
    }

    /**
     * A submenu (Entradas, Ajustes) in the panel's own measure: every row becomes a glass tile like the panel's pills
     * (same height, corners, translucency and white focus), with the panel's type sizes and icon size.
     */
    void styleAsPanel(ViewGroup rows) {
        int gap = px(10);
        for (int i = 0; i < rows.getChildCount(); i++) {
            View row = rows.getChildAt(i);
            if (!row.isFocusable()) {
                // A separator line: a thin gap instead.
                ViewGroup.LayoutParams lp = row.getLayoutParams();
                if (lp instanceof ViewGroup.MarginLayoutParams) ((ViewGroup.MarginLayoutParams) lp).setMargins(px(18), px(2), px(18), px(8));
                continue;
            }
            GradientDrawable on = new GradientDrawable();
            on.setColor(FOCUS);
            on.setCornerRadius(px(31));
            GradientDrawable off = new GradientDrawable();
            off.setColor(TILE);
            off.setCornerRadius(px(31));
            off.setStroke(Math.max(1, Math.round(dp)), TILE_STROKE);
            StateListDrawable bg = new StateListDrawable();
            bg.addState(new int[]{android.R.attr.state_focused}, on);
            bg.addState(new int[]{}, off);
            row.setBackground(bg);
            row.setPadding(px(14), 0, px(14), 0);
            ViewGroup.LayoutParams lp = row.getLayoutParams();
            lp.height = px(62);
            if (lp instanceof ViewGroup.MarginLayoutParams) ((ViewGroup.MarginLayoutParams) lp).setMargins(0, 0, 0, gap);
            row.setLayoutParams(lp);
            row.setOnFocusChangeListener((v, focused) -> v.animate().scaleX(focused ? 1.04f : 1f).scaleY(focused ? 1.04f : 1f)
                    .setDuration(140).start());
            if (row instanceof ViewGroup) shrink((ViewGroup) row);
        }
        rows.setClipChildren(false);
        if (rows.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) rows.getParent();
            parent.setClipChildren(false);
            // A focused tile grows a little into the panel's padding: draw it there instead of cutting its edge.
            parent.setClipToPadding(false);
            rows.setClipToPadding(false);
            int pad = px(14);
            parent.setPadding(pad, pad, pad, Math.max(0, pad - gap));
        }
    }

    /** The panel's sizes inside one row: names 10 sp, second lines 8.5 sp, icons 22 (scaled). */
    private void shrink(ViewGroup g) {
        for (int i = 0; i < g.getChildCount(); i++) {
            View v = g.getChildAt(i);
            if (v.getId() == R.id.status) {
                // At the panel's width the green dot says "connected"; the word would cut the input's name.
                v.setVisibility(View.GONE);
            } else if (v instanceof TextView) {
                TextView t = (TextView) v;
                float sp = t.getTextSize() / activity.getResources().getDisplayMetrics().scaledDensity;
                t.setTextSize(TypedValue.COMPLEX_UNIT_SP, (sp >= 13 ? 10f : 9.5f) * TYPE);
                // Second lines in Outfit 400, not the variable font's thin default, which blurs at this size.
                if (sp < 13) {
                    try {
                        t.setTypeface(activity.getResources().getFont(R.font.outfit_regular));
                    } catch (RuntimeException ignored) {
                        // Keep the font it has.
                    }
                }
            } else if (v instanceof ImageView) {
                ViewGroup.LayoutParams lp = v.getLayoutParams();
                lp.width = lp.height = px(22);
                v.setLayoutParams(lp);
            } else if (v instanceof ViewGroup) {
                shrink((ViewGroup) v);
            }
        }
    }

    /** A separable box blur of the given radius, run this many times (three come close to a Gaussian). */
    private static Bitmap boxBlur(Bitmap b, int radius, int passes) {
        int w = b.getWidth(), h = b.getHeight();
        int[] px = new int[w * h], tmp = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        for (int p = 0; p < passes; p++) {
            blurLine(px, tmp, w, h, radius, true);
            blurLine(tmp, px, w, h, radius, false);
        }
        b.setPixels(px, 0, w, 0, 0, w, h);
        return b;
    }

    private static void blurLine(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int lines = horizontal ? h : w, len = horizontal ? w : h;
        for (int l = 0; l < lines; l++) {
            for (int i = 0; i < len; i++) {
                int a = 0, rr = 0, g = 0, bb = 0, n = 0;
                for (int k = Math.max(0, i - r); k <= Math.min(len - 1, i + r); k++) {
                    int c = in[horizontal ? l * w + k : k * w + l];
                    a += c >>> 24; rr += (c >> 16) & 0xFF; g += (c >> 8) & 0xFF; bb += c & 0xFF; n++;
                }
                out[horizontal ? l * w + i : i * w + l] = ((a / n) << 24) | ((rr / n) << 16) | ((g / n) << 8) | (bb / n);
            }
        }
    }

    /** Clips a drawable to rounded corners (the frosted picture is square). */
    private static final class RoundedDrawable extends android.graphics.drawable.Drawable {
        private final android.graphics.drawable.Drawable inner;
        private final float radius;
        private final android.graphics.Path path = new android.graphics.Path();

        RoundedDrawable(android.graphics.drawable.Drawable inner, float radius) {
            this.inner = inner;
            this.radius = radius;
        }

        @Override
        protected void onBoundsChange(android.graphics.Rect b) {
            inner.setBounds(b);
            path.reset();
            path.addRoundRect(new android.graphics.RectF(b), radius, radius, android.graphics.Path.Direction.CW);
        }

        @Override
        public void draw(android.graphics.Canvas canvas) {
            int save = canvas.save();
            canvas.clipPath(path);
            inner.draw(canvas);
            canvas.restoreToCount(save);
        }

        @Override public void setAlpha(int alpha) { inner.setAlpha(alpha); }
        @Override public void setColorFilter(android.graphics.ColorFilter cf) { inner.setColorFilter(cf); }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }

    /** Back closes the panel; other keys go on to the focused tile. */
    boolean onKey(KeyEvent event) {
        if (overlay == null) return false;
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
            if (event.getAction() == KeyEvent.ACTION_UP) hide();
            return true;
        }
        return false;
    }

    private GridLayout.LayoutParams cell(int row, int column, int rowSpan, int w, int h, int left, int bottom) {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(GridLayout.spec(row, rowSpan), GridLayout.spec(column));
        lp.width = w;
        lp.height = h;
        lp.leftMargin = left;
        lp.bottomMargin = bottom;
        return lp;
    }

    private View bigTile(int icon, String label, Runnable action) {
        LinearLayout tile = tile(px(20), action);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        ImageView i = icon(icon, 46);
        tile.addView(i);
        TextView t = text(label, 11 * TYPE, true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(8);
        tile.addView(t, lp);
        return tile;
    }

    private View pillTile(int icon, String label, String detail, Runnable action) {
        LinearLayout tile = tile(px(31), action);
        tile.setOrientation(LinearLayout.HORIZONTAL);
        tile.setGravity(Gravity.CENTER_VERTICAL);
        tile.setPadding(px(14), 0, px(10), 0);
        tile.addView(icon(icon, 22));
        LinearLayout words = new LinearLayout(activity);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setDuplicateParentStateEnabled(true);
        words.addView(text(label, 10 * TYPE, true));
        if (detail != null && !detail.isEmpty()) {
            TextView d = text(detail, 9.5f * TYPE, false);
            d.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}}, new int[]{0xFF44444A, 0xE6FFFFFF}));
            words.addView(d);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        lp.leftMargin = px(10);
        tile.addView(words, lp);
        return tile;
    }

    private void addRound(LinearLayout row, int icon, String label, int tint, Runnable action) {
        LinearLayout b = tile(-1, action);
        b.setGravity(Gravity.CENTER);
        b.setContentDescription(label);
        ImageView i = icon(icon, 22);
        if (tint != 0) i.setImageTintList(new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}}, new int[]{tint, tint}));
        b.addView(i);
        int size = px(56);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        if (row.getChildCount() > 0) lp.leftMargin = 0;
        row.addView(b, lp);
    }

    /** A focusable glass tile: white with dark ink while focused (lifted a little), translucent otherwise. */
    private LinearLayout tile(float corner, Runnable action) {
        LinearLayout tile = new LinearLayout(activity);
        tile.setFocusable(true);
        tile.setClickable(true);
        GradientDrawable on = new GradientDrawable();
        on.setColor(FOCUS);
        GradientDrawable off = new GradientDrawable();
        off.setColor(TILE);
        if (corner < 0) {   // a round button: a true circle at any size
            on.setShape(GradientDrawable.OVAL);
            off.setShape(GradientDrawable.OVAL);
        } else {
            on.setCornerRadius(corner);
            off.setCornerRadius(corner);
        }
        off.setStroke(Math.max(1, Math.round(dp)), TILE_STROKE);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, on);
        bg.addState(new int[]{}, off);
        tile.setBackground(bg);
        tile.setOnClickListener(v -> {
            hide();
            action.run();
        });
        tile.setOnFocusChangeListener((v, focused) -> v.animate().scaleX(focused ? 1.04f : 1f).scaleY(focused ? 1.04f : 1f)
                .setDuration(140).start());
        return tile;
    }

    private ImageView icon(int res, int sizeDp) {
        ImageView i = new ImageView(activity);
        i.setImageResource(res);
        i.setDuplicateParentStateEnabled(true);
        i.setImageTintList(new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}}, new int[]{INK, Color.WHITE}));
        i.setLayoutParams(new LinearLayout.LayoutParams(px(sizeDp), px(sizeDp)));
        return i;
    }

    private TextView text(String s, float sp, boolean bold) {
        TextView t = new TextView(activity);
        t.setText(s);
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        t.setDuplicateParentStateEnabled(true);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        try {
            t.setTypeface(activity.getResources().getFont(bold ? R.font.outfit_medium : R.font.outfit_regular));
        } catch (RuntimeException e) {
            t.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        }
        t.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_focused}, {}}, new int[]{INK, Color.WHITE}));
        return t;
    }

    /** The panel's layout scale: 0.6 of the first try covered too little, 20 % more than that reads well. */
    private static final float SCALE = 0.72f;
    /** Type grows with the panel. */
    private static final float TYPE = 1.2f;

    private int px(float dps) {
        return Math.round(dps * SCALE * dp);
    }

    /** Unscaled dp, for distances to things outside the panel. */
    private int dpx(float dps) {
        return Math.round(dps * dp);
    }
}
