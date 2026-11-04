package com.gathin.tvhome;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Outline;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

/**
 * tvos_settings (config.json): the launcher's settings in the look of tvOS Settings. Home shows through blurred
 * behind everything; the screen's title sits centred at the top, a large icon on the left with a line on the focused
 * option under it, and the options on the right in named groups, each with its current value in grey. The focused
 * row turns white and lifts a little. A row with several values opens a list in the same place, the current one
 * ticked; Back closes it. LauncherSettingsActivity keeps what each option does; this class only draws.
 */
final class TvosSettings {
    /**
     * A small copy of Home as it was when the settings were opened (MainActivity.openLauncherSettings()), shown
     * blurred behind them. Null when there was none to take: the screen is then plain dark.
     */
    static Bitmap backdrop;

    static final class Row {
        View view;
        TextView label;
        TextView value;
        Supplier<String> source;
        BooleanSupplier enabled;
        String description;
        Runnable action;
    }

    private final Activity activity;
    private final float dp;
    private final Typeface regular;
    private final Typeface medium;
    private final String mainTitle;
    private final String mainDescription;
    private final TextView title;
    private final TextView description;
    private final ScrollView scroll;
    private final LinearLayout list;
    private final LinearLayout picks;
    private final List<Row> rows = new ArrayList<>();
    private View returnFocus;

    TvosSettings(Activity activity, String title, String description) {
        this.activity = activity;
        this.dp = activity.getResources().getDisplayMetrics().density;
        this.regular = font(R.font.outfit_regular, Typeface.NORMAL);
        this.medium = font(R.font.outfit_medium, Typeface.BOLD);
        this.mainTitle = title;
        this.mainDescription = description;

        FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(0xFF0B0E12);
        if (backdrop != null) {
            ImageView home = new ImageView(activity);
            home.setScaleType(ImageView.ScaleType.CENTER_CROP);
            home.setImageBitmap(backdrop);
            if (Build.VERSION.SDK_INT >= 31) {
                ColorMatrix vivid = new ColorMatrix();
                vivid.setSaturation(1.3f);
                home.setRenderEffect(RenderEffect.createChainEffect(
                        RenderEffect.createColorFilterEffect(new ColorMatrixColorFilter(vivid)),
                        RenderEffect.createBlurEffect(px(34), px(34), Shader.TileMode.CLAMP)));
            }
            root.addView(home, match());
            // Darker than the Control Center's glass: these are long lists of text and must read at a distance.
            View dim = new View(activity);
            dim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {0x99070A0F, 0xB3070A0F, 0xD9070A0F}));
            root.addView(dim, match());
        }

        this.title = text(22, true, 0xFFFFFFFF);
        this.title.setText(title);
        this.title.setGravity(Gravity.CENTER);
        this.title.setSingleLine(true);
        this.title.setEllipsize(TextUtils.TruncateAt.END);
        FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP);
        tp.topMargin = px(32);
        tp.leftMargin = tp.rightMargin = px(60);
        root.addView(this.title, tp);

        // Left: the Settings icon (the one Home uses for the TV's settings) and the focused option's line.
        LinearLayout left = new LinearLayout(activity);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView icon = new ImageView(activity);
        icon.setImageResource(R.drawable.banner_ajustes);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        icon.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), px(16));
            }
        });
        icon.setClipToOutline(true);
        icon.setElevation(px(14));
        left.addView(icon, new LinearLayout.LayoutParams(px(296), px(166)));
        this.description = text(13, false, 0xA6FFFFFF);
        this.description.setGravity(Gravity.CENTER_HORIZONTAL);
        this.description.setLineSpacing(0, 1.3f);
        this.description.setText(description);
        LinearLayout.LayoutParams dp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        dp2.topMargin = px(24);
        left.addView(this.description, dp2);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(330), ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.START | Gravity.CENTER_VERTICAL);
        lp.leftMargin = px(62);
        lp.topMargin = px(20);
        root.addView(left, lp);

        // Right: the options, scrolling under a soft edge. Padding leaves room for the focused row to grow.
        scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setVerticalFadingEdgeEnabled(true);
        scroll.setFadingEdgeLength(px(48));
        scroll.setClipToPadding(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout holder = new LinearLayout(activity);
        holder.setOrientation(LinearLayout.VERTICAL);
        holder.setClipChildren(false);
        holder.setPadding(px(16), px(8), px(16), px(110));
        list = column();
        picks = column();
        picks.setVisibility(View.GONE);
        holder.addView(list);
        holder.addView(picks);
        scroll.addView(holder, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        FrameLayout.LayoutParams sp = new FrameLayout.LayoutParams(px(478), ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.END);
        sp.topMargin = px(84);
        sp.rightMargin = px(44);
        root.addView(scroll, sp);

        activity.setContentView(root);
    }

    /** A group's name over its rows, small and grey. */
    void section(String name) {
        TextView t = text(11, true, 0x80FFFFFF);
        t.setText(name);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = list.getChildCount() == 0 ? px(4) : px(16);
        p.bottomMargin = px(5);
        p.leftMargin = px(15);
        list.addView(t, p);
    }

    /**
     * One option: its name, its value on the right (null for none), the line shown under the icon while it is focused,
     * whether it can be used now (null for always) and what OK does.
     */
    Row row(String label, Supplier<String> value, String description, BooleanSupplier enabled, Runnable action) {
        Row row = new Row();
        row.source = value;
        row.enabled = enabled;
        row.description = description;
        row.action = action;
        LinearLayout v = line(px(38));
        row.label = text(15, false, 0xE8FFFFFF);
        row.label.setText(label);
        row.label.setSingleLine(true);
        row.label.setEllipsize(TextUtils.TruncateAt.END);
        v.addView(row.label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.value = text(15, false, 0x80FFFFFF);
        row.value.setSingleLine(true);
        row.value.setEllipsize(TextUtils.TruncateAt.END);
        row.value.setMaxWidth(px(190));
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        vp.leftMargin = px(16);
        v.addView(row.value, vp);
        row.view = v;
        focusLook(v, row.label, row.value, 0x80FFFFFF, () -> description(row.description));
        v.setOnClickListener(x -> {
            if (row.enabled != null && !row.enabled.getAsBoolean()) return;
            row.action.run();
        });
        list.addView(v, rowParams());
        rows.add(row);
        return row;
    }

    /**
     * A list in place of the options: one row per value, the current one ticked (-1 for none). OK picks one and
     * brings the options back, focused where they were; Back brings them back without a pick.
     */
    void picker(String title, String description, String[] labels, int checked, IntConsumer pick) {
        returnFocus = activity.getCurrentFocus();
        picks.removeAllViews();
        View focus = null;
        for (int i = 0; i < labels.length; i++) {
            final int which = i;
            LinearLayout v = line(px(42));
            TextView tick = text(15, true, 0xE8FFFFFF);
            tick.setText(i == checked ? "✓" : "");
            v.addView(tick, new LinearLayout.LayoutParams(px(28), ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView label = text(16, false, 0xE8FFFFFF);
            label.setText(labels[i]);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            v.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            focusLook(v, label, tick, 0xE8FFFFFF, null);
            v.setOnClickListener(x -> {
                close();
                pick.accept(which);
            });
            picks.addView(v, rowParams());
            if (i == Math.max(0, checked)) focus = v;
        }
        this.title.setText(title);
        description(description);
        list.setVisibility(View.GONE);
        picks.setVisibility(View.VISIBLE);
        scroll.scrollTo(0, 0);
        if (focus != null) focus.requestFocus();
    }

    /** Back: closes an open list (true), or leaves it to the activity (false). */
    boolean back() {
        if (picks.getVisibility() != View.VISIBLE) return false;
        close();
        return true;
    }

    /** Puts every value and enabled state back in line with the activity's. */
    void refresh() {
        for (Row row : rows) {
            String v = row.source == null ? null : row.source.get();
            row.value.setText(v == null ? "" : v);
            boolean on = row.enabled == null || row.enabled.getAsBoolean();
            row.view.setAlpha(on ? 1f : 0.4f);
        }
    }

    void focusFirst() {
        if (!rows.isEmpty()) rows.get(0).view.requestFocus();
    }

    private void close() {
        picks.setVisibility(View.GONE);
        picks.removeAllViews();
        list.setVisibility(View.VISIBLE);
        title.setText(mainTitle);
        if (returnFocus != null && returnFocus.getParent() == list) returnFocus.requestFocus();
        else focusFirst();
        returnFocus = null;
    }

    private void description(String text) {
        description.setText(TextUtils.isEmpty(text) ? mainDescription : text);
    }

    /** As tvOS: the focused row turns white with dark text and lifts a little over a soft shadow. */
    private void focusLook(View v, TextView main, TextView side, int sideRest, Runnable onFocus) {
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(px(9));
        bg.setColor(0x00FFFFFF);
        v.setBackground(bg);
        v.setOnFocusChangeListener((x, focused) -> {
            bg.setColor(focused ? 0xFFFFFFFF : 0x00FFFFFF);
            main.setTextColor(focused ? 0xFF111111 : 0xE8FFFFFF);
            main.setTypeface(focused ? medium : regular);
            side.setTextColor(focused ? 0x99111111 : sideRest);
            v.setElevation(focused ? px(10) : 0);
            v.animate().scaleX(focused ? 1.05f : 1f).scaleY(focused ? 1.05f : 1f).setDuration(150).start();
            if (focused && onFocus != null) onFocus.run();
        });
    }

    private LinearLayout line(int height) {
        LinearLayout v = new LinearLayout(activity);
        v.setOrientation(LinearLayout.HORIZONTAL);
        v.setGravity(Gravity.CENTER_VERTICAL);
        v.setPadding(px(15), 0, px(15), 0);
        v.setMinimumHeight(height);
        v.setFocusable(true);
        v.setClickable(true);
        return v;
    }

    private LinearLayout.LayoutParams rowParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = px(1);
        return p;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(activity);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setClipChildren(false);
        return l;
    }

    private TextView text(float sp, boolean bold, int color) {
        TextView t = new TextView(activity);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setTypeface(bold ? medium : regular);
        t.setIncludeFontPadding(false);
        return t;
    }

    private Typeface font(int id, int fallback) {
        try {
            return activity.getResources().getFont(id);
        } catch (RuntimeException e) {
            return Typeface.create(Typeface.DEFAULT, fallback);
        }
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private int px(float v) {
        return Math.round(v * dp);
    }
}
