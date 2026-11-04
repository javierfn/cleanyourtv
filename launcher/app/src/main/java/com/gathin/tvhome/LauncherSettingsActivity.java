package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Remote-friendly settings owned by the launcher; system settings stay one row away. */
public final class LauncherSettingsActivity extends Activity {
    @Override
    protected void attachBaseContext(android.content.Context base) {
        super.attachBaseContext(LauncherLocale.wrap(base));   // locale
    }

    static final String EXTRA_REORDER = "com.gathin.tvhome.extra.REORDER_FROM_SETTINGS";
    private SharedPreferences choices;
    private final TextView[] columnChoices = new TextView[4];
    private final TextView[] rowChoices = new TextView[4];
    private static final int[] DOCK_OPACITIES = {100, 70, 50, 30};
    private final TextView[] dockChoices = new TextView[DOCK_OPACITIES.length];
    private static final String[] CLOCK_FORMATS = {"system", "12", "24"};
    private final TextView[] clockChoices = new TextView[CLOCK_FORMATS.length];
    private TextView wallpaperChoice;
    private TextView animationsChoice;
    private TextView wallpaperMotionChoice;
    private TextView advancedChoice;
    private TextView followThemeChoice;
    private TextView openOnStartChoice;
    private int columns;
    private int rowsBelowDock;
    private int dockOpacity;
    private String clockFormat;
    private boolean animations;
    private boolean advancedEffects;
    /** tvos_settings: the tvOS-look screen, used instead of the layout's rows. */
    private TvosSettings tvos;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_launcher_settings);
        choices = getSharedPreferences(Config.SETTINGS_NAME, MODE_PRIVATE);
        Config config = Config.load(this);
        View reorderChoice = findViewById(R.id.reorderChoice);
        reorderChoice.setVisibility(config.launcherSettingsButton ? View.VISIBLE : View.GONE);
        reorderChoice.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
            finish();
        });
        columns = config.columns;
        rowsBelowDock = config.rowsBelowDock;
        dockOpacity = config.dockOpacity;
        clockFormat = config.clockFormat;
        animations = config.animations;
        advancedEffects = config.animations && config.advancedEffects;
        if (WeatherLine.configFlag(this, "tvos_settings")) {
            buildTvos(config);
            return;
        }

        int[] ids = {R.id.columns4, R.id.columns5, R.id.columns6, R.id.columns7};
        for (int i = 0; i < ids.length; i++) {
            final int value = i + 4;
            columnChoices[i] = findViewById(ids[i]);
            columnChoices[i].setOnClickListener(v -> {
                if (columns == value) return;
                columns = value;
                choices.edit().putInt("columns", value).apply();
                changed();
                refresh();
            });
        }
        int[] rowIds = {R.id.rows0, R.id.rows1, R.id.rows2, R.id.rows3};
        for (int i = 0; i < rowIds.length; i++) {
            final int value = i;
            rowChoices[i] = findViewById(rowIds[i]);
            rowChoices[i].setOnClickListener(v -> {
                if (rowsBelowDock == value) return;
                rowsBelowDock = value;
                choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] dockIds = {R.id.dock100, R.id.dock70, R.id.dock50, R.id.dock30};
        for (int i = 0; i < dockIds.length; i++) {
            final int value = DOCK_OPACITIES[i];
            dockChoices[i] = findViewById(dockIds[i]);
            dockChoices[i].setOnClickListener(v -> {
                if (dockOpacity == value) return;
                dockOpacity = value;
                choices.edit().putInt(Config.DOCK_OPACITY_KEY, value).apply();
                changed();
                refresh();
            });
        }
        int[] clockIds = {R.id.clockSystem, R.id.clock12, R.id.clock24};
        for (int i = 0; i < clockIds.length; i++) {
            final String value = CLOCK_FORMATS[i];
            clockChoices[i] = findViewById(clockIds[i]);
            clockChoices[i].setOnClickListener(v -> {
                if (value.equals(clockFormat)) return;
                clockFormat = value;
                choices.edit().putString(Config.CLOCK_FORMAT_KEY, value).apply();
                changed();
                refresh();
            });
        }
        // Mirror text alignment without globally opting the launcher into RTL. Numeric rows
        // still need explicit ordering in Android's no-supportsRtl compatibility mode.
        if (getResources().getBoolean(R.bool.rtl_menu_bar))
            findViewById(android.R.id.content).setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if (getResources().getBoolean(R.bool.rtl_app_grid)) {
            reverseRow(columnChoices);
            reverseRow(rowChoices);
            reverseRow(dockChoices);
            reverseRow(clockChoices);
        }
        wallpaperChoice = findViewById(R.id.wallpaperChoice);
        wallpaperChoice.setOnClickListener(v -> showWallpaperPicker());
        wallpaperMotionChoice = findViewById(R.id.wallpaperMotionChoice);
        wallpaperMotionChoice.setOnClickListener(v -> {
            WallpaperMotion.set(this, !WallpaperMotion.enabled(this));
            setResult(RESULT_OK);
            refresh();
        });
        animationsChoice = findViewById(R.id.animationsChoice);
        animationsChoice.setOnClickListener(v -> {
            animations = !animations;
            if (!animations) advancedEffects = false;
            choices.edit().putBoolean("animations", animations)
                    .putBoolean("advanced_effects", advancedEffects).apply();
            changed();
            refresh();
        });
        advancedChoice = findViewById(R.id.advancedChoice);
        advancedChoice.setOnClickListener(v -> {
            if (advancedEffects) {
                setAdvanced(false);
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_effects)
                        .setMessage(R.string.advanced_effects_warning)
                        .setPositiveButton(R.string.enable, (dialog, which) -> setAdvanced(true))
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });
        openOnStartChoice = findViewById(R.id.openOnStartChoice);
        if (config.openOnStartChoice) {
            openOnStartChoice.setVisibility(View.VISIBLE);
            openOnStartChoice.setOnClickListener(v -> showOpenOnStartPicker());
        }
        followThemeChoice = findViewById(R.id.followThemeChoice);
        followThemeChoice.setOnClickListener(v -> {
            if (!hasOnTvChoices()) return;
            choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                    .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                    .remove(Config.DOCK_OPACITY_KEY).remove(Config.CLOCK_FORMAT_KEY).remove(OpenOnStart.KEY).commit();
            Config current = Config.load(this);
            columns = current.columns;
            rowsBelowDock = current.rowsBelowDock;
            dockOpacity = current.dockOpacity;
            clockFormat = current.clockFormat;
            animations = current.animations;
            advancedEffects = current.animations && current.advancedEffects;
            changed();
            refresh();
        });
        findViewById(R.id.defaultHomeChoice).setOnClickListener(v ->
                open(new Intent(this, HomeSettingsActivity.class)));
        findViewById(R.id.tvSettingsChoice).setOnClickListener(v ->
                open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        refresh();
        if (config.launcherSettingsButton) reorderChoice.requestFocus();
        else columnChoices[Math.max(0, Math.min(columns - 4, 3))].requestFocus();
    }

    private void reverseRow(TextView[] views) {
        LinearLayout row = (LinearLayout) views[0].getParent();
        row.removeAllViews();
        for (int i = views.length - 1; i >= 0; i--) row.addView(views[i]);
    }

    private void showWallpaperPicker() {
        File dir = Config.dir(this);
        List<File> images = new java.util.ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        // A theme with a single wallpaper.jpg offers that picture too (Config maps the choice back to it).
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        String[] labels = new String[images.size() + 1];
        for (int i = 0; i < images.size(); i++) labels[i] = images.get(i).getName();
        labels[images.size()] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = "none".equals(current) ? images.size()
                : indexOf(images, current);
        new AlertDialog.Builder(this)
                .setTitle(R.string.wallpaper_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == images.size()) {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    } else {
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY,
                                images.get(which).getName()).apply();
                    }
                    changed();
                    Toast.makeText(this, R.string.wallpaper_applied, Toast.LENGTH_SHORT).show();
                    refresh();
                })
                .setNeutralButton(R.string.wallpaper_theme_default, (dialog, which) -> {
                    choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    changed();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Off, then every app with a TV entry point by name. Saved at once; it is read on the next boot. */
    private void showOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[]{r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.open_on_start_label)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** The picked app's name, its package when it is gone, or Off. */
    private String openOnStartLabel() {
        String pkg = choices.getString(OpenOnStart.KEY, "");
        if (pkg.isEmpty()) return getString(R.string.off);
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    private static int indexOf(List<File> images, String name) {
        if (name == null) return -1;
        for (int i = 0; i < images.size(); i++) {
            if (images.get(i).getName().equals(name)) return i;
        }
        return -1;
    }

    /**
     * A choice was saved. Home rebuilds when it next resumes because the change count moved, whether these
     * settings are left with Back or with the remote's Home button (which cancels the activity result).
     */
    private void changed() {
        setResult(RESULT_OK);
        choices.edit().putLong(Config.SETTINGS_REVISION_KEY,
                choices.getLong(Config.SETTINGS_REVISION_KEY, 0) + 1).apply();
    }

    private void setAdvanced(boolean enabled) {
        advancedEffects = enabled;
        choices.edit().putBoolean("advanced_effects", enabled).apply();
        changed();
        refresh();
    }

    private void refresh() {
        if (tvos != null) {
            tvos.refresh();
            return;
        }
        for (int i = 0; i < columnChoices.length; i++) {
            columnChoices[i].setSelected(columns == i + 4);
        }
        for (int i = 0; i < rowChoices.length; i++) {
            rowChoices[i].setSelected(rowsBelowDock == i);
        }
        for (int i = 0; i < dockChoices.length; i++) {
            dockChoices[i].setSelected(dockOpacity == DOCK_OPACITIES[i]);
        }
        for (int i = 0; i < clockChoices.length; i++) {
            clockChoices[i].setSelected(CLOCK_FORMATS[i].equals(clockFormat));
        }
        animationsChoice.setText(getString(R.string.animations_state,
                getString(animations ? R.string.on : R.string.off)));
        advancedChoice.setText(getString(R.string.advanced_effects_state,
                getString(advancedEffects ? R.string.on : R.string.off)));
        wallpaperMotionChoice.setText(getString(R.string.wallpaper_motion_state,
                getString(WallpaperMotion.enabled(this) ? R.string.on : R.string.off)));
        advancedChoice.setEnabled(animations);
        advancedChoice.setAlpha(animations ? 1f : 0.5f);
        String wallpaperCurrent = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        wallpaperChoice.setText(getString(R.string.wallpaper_state,
                "none".equals(wallpaperCurrent) ? getString(R.string.wallpaper_none)
                        : wallpaperCurrent != null ? wallpaperCurrent
                        : getString(R.string.wallpaper_theme_default)));
        openOnStartChoice.setText(getString(R.string.open_on_start_state, openOnStartLabel()));
        followThemeChoice.setSelected(!hasOnTvChoices());
    }

    private boolean hasOnTvChoices() {
        return choices.contains("columns") || choices.contains(Config.ROWS_BELOW_DOCK_KEY)
                || choices.contains("animations") || choices.contains("advanced_effects")
                || choices.contains(Config.WALLPAPER_CHOICE_KEY) || choices.contains(Config.DOCK_OPACITY_KEY)
                || choices.contains(Config.CLOCK_FORMAT_KEY) || choices.contains(OpenOnStart.KEY);
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.not_available, Toast.LENGTH_LONG).show();
        }
    }

    // ---- tvos_settings -----------------------------------------------------------------------------

    @Override
    public void onBackPressed() {
        if (tvos != null && tvos.back()) return;
        super.onBackPressed();
    }

    /** The same options as the layout, each a row of TvosSettings; what OK does matches the layout's buttons. */
    private void buildTvos(Config config) {
        tvos = new TvosSettings(this, getString(R.string.launcher_settings), getString(R.string.tvos_desc_main));
        tvos.section(getString(R.string.tvos_section_home));
        tvos.row(getString(R.string.columns_label), () -> String.valueOf(columns),
                getString(R.string.tvos_desc_columns), null, () -> tvos.picker(getString(R.string.columns_label),
                        getString(R.string.tvos_desc_columns), new String[] {"4", "5", "6", "7"}, columns - 4, which -> {
                            if (columns == which + 4) return;
                            columns = which + 4;
                            choices.edit().putInt("columns", columns).apply();
                            changed();
                            refresh();
                        }));
        tvos.row(getString(R.string.rows_below_dock_label), () -> String.valueOf(rowsBelowDock),
                getString(R.string.tvos_desc_rows), null, () -> tvos.picker(getString(R.string.rows_below_dock_label),
                        getString(R.string.tvos_desc_rows), new String[] {"0", "1", "2", "3"}, rowsBelowDock, which -> {
                            if (rowsBelowDock == which) return;
                            rowsBelowDock = which;
                            choices.edit().putInt(Config.ROWS_BELOW_DOCK_KEY, which).apply();
                            changed();
                            refresh();
                        }));
        String[] opacities = new String[DOCK_OPACITIES.length];
        for (int i = 0; i < opacities.length; i++) opacities[i] = DOCK_OPACITIES[i] + " %";
        tvos.row(getString(R.string.dock_opacity_label), () -> dockOpacity + " %",
                getString(R.string.tvos_desc_dock), null, () -> {
                    int checked = -1;
                    for (int i = 0; i < DOCK_OPACITIES.length; i++) if (DOCK_OPACITIES[i] == dockOpacity) checked = i;
                    tvos.picker(getString(R.string.dock_opacity_label), getString(R.string.tvos_desc_dock), opacities,
                            checked, which -> {
                                if (dockOpacity == DOCK_OPACITIES[which]) return;
                                dockOpacity = DOCK_OPACITIES[which];
                                choices.edit().putInt(Config.DOCK_OPACITY_KEY, dockOpacity).apply();
                                changed();
                                refresh();
                            });
                });
        if (config.launcherSettingsButton) {
            tvos.row(getString(R.string.reorder), null, getString(R.string.tvos_desc_reorder), null, () -> {
                setResult(RESULT_OK, new Intent().putExtra(EXTRA_REORDER, true));
                finish();
            });
        }

        tvos.section(getString(R.string.tvos_section_look));
        tvos.row(getString(R.string.wallpaper_label), this::tvosWallpaperLabel,
                getString(R.string.tvos_desc_wallpaper), null, this::tvosWallpaperPicker);
        String[] clocks = {getString(R.string.clock_format_system), getString(R.string.clock_format_12),
                getString(R.string.clock_format_24)};
        tvos.row(getString(R.string.dream_name), () -> clocks[Math.max(0, clockIndex())],
                getString(R.string.tvos_desc_clock), null, () -> tvos.picker(getString(R.string.dream_name),
                        getString(R.string.tvos_desc_clock), clocks, clockIndex(), which -> {
                            if (CLOCK_FORMATS[which].equals(clockFormat)) return;
                            clockFormat = CLOCK_FORMATS[which];
                            choices.edit().putString(Config.CLOCK_FORMAT_KEY, clockFormat).apply();
                            changed();
                            refresh();
                        }));
        tvos.row(getString(R.string.tvos_wallpaper_motion), () -> onOff(WallpaperMotion.enabled(this)),
                getString(R.string.tvos_desc_motion), null, () -> {
                    WallpaperMotion.set(this, !WallpaperMotion.enabled(this));
                    setResult(RESULT_OK);
                    refresh();
                });

        tvos.section(getString(R.string.tvos_section_effects));
        tvos.row(getString(R.string.tvos_animations), () -> onOff(animations),
                getString(R.string.tvos_desc_animations), null, () -> {
                    animations = !animations;
                    if (!animations) advancedEffects = false;
                    choices.edit().putBoolean("animations", animations)
                            .putBoolean("advanced_effects", advancedEffects).apply();
                    changed();
                    refresh();
                });
        tvos.row(getString(R.string.advanced_effects), () -> onOff(advancedEffects),
                getString(R.string.advanced_effects_warning), () -> animations, () -> {
                    if (advancedEffects) setAdvanced(false);
                    else tvos.picker(getString(R.string.advanced_effects), getString(R.string.advanced_effects_warning),
                            new String[] {getString(R.string.enable), getString(android.R.string.cancel)}, -1,
                            which -> {
                                if (which == 0) setAdvanced(true);
                            });
                });

        tvos.section(getString(R.string.tvos_section_general));
        if (config.openOnStartChoice) {
            tvos.row(getString(R.string.open_on_start_label), this::openOnStartLabel,
                    getString(R.string.tvos_desc_open_on_start), null, this::tvosOpenOnStartPicker);
        }
        tvos.row(getString(R.string.follow_theme), () -> onOff(!hasOnTvChoices()),
                getString(R.string.follow_theme_hint), null, () -> {
                    if (!hasOnTvChoices()) return;
                    choices.edit().remove("columns").remove(Config.ROWS_BELOW_DOCK_KEY).remove("animations")
                            .remove("advanced_effects").remove(Config.WALLPAPER_CHOICE_KEY)
                            .remove(Config.DOCK_OPACITY_KEY).remove(Config.CLOCK_FORMAT_KEY).remove(OpenOnStart.KEY).commit();
                    Config current = Config.load(this);
                    columns = current.columns;
                    rowsBelowDock = current.rowsBelowDock;
                    dockOpacity = current.dockOpacity;
                    clockFormat = current.clockFormat;
                    animations = current.animations;
                    advancedEffects = current.animations && current.advancedEffects;
                    changed();
                    refresh();
                });
        tvos.row(getString(R.string.home_settings), null, getString(R.string.home_settings_help), null,
                () -> open(new Intent(this, HomeSettingsActivity.class)));
        tvos.row(getString(R.string.tv_settings), null, getString(R.string.tvos_desc_tv_settings), null,
                () -> open(SystemSettings.intent(this, Settings.ACTION_SETTINGS)));
        refresh();
        tvos.focusFirst();
    }

    private int clockIndex() {
        for (int i = 0; i < CLOCK_FORMATS.length; i++) if (CLOCK_FORMATS[i].equals(clockFormat)) return i;
        return -1;
    }

    private String onOff(boolean on) {
        return getString(on ? R.string.on : R.string.off);
    }

    /** The wallpapers the theme has, as showWallpaperPicker() lists them. */
    private List<File> tvosWallpapers() {
        File dir = Config.dir(this);
        List<File> images = new ArrayList<>(WallpaperRotation.images(
                dir == null ? null : new File(dir, "wallpapers")));
        File single = dir == null ? null : new File(dir, Config.WALLPAPER_NAME);
        if (single != null && single.isFile()) images.add(0, single);
        return images;
    }

    /** "03-lago-chola.jpg" reads "Lago chola": no number in front, no extension, spaces for dashes. */
    private static String tvosName(String file) {
        String name = file.replaceFirst("\\.[A-Za-z0-9]+$", "").replaceFirst("^\\d+[-_ ]+", "")
                .replace('-', ' ').replace('_', ' ').trim();
        if (name.isEmpty()) return file;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private String tvosWallpaperLabel() {
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        if (current == null) return getString(R.string.tvos_wallpaper_theme);
        if ("none".equals(current)) return getString(R.string.wallpaper_none);
        return tvosName(current);
    }

    /** Theme default first, then each picture by name, then none. */
    private void tvosWallpaperPicker() {
        List<File> images = tvosWallpapers();
        String[] labels = new String[images.size() + 2];
        labels[0] = getString(R.string.tvos_wallpaper_theme);
        for (int i = 0; i < images.size(); i++) labels[i + 1] = tvosName(images.get(i).getName());
        labels[images.size() + 1] = getString(R.string.wallpaper_none);
        String current = choices.getString(Config.WALLPAPER_CHOICE_KEY, null);
        int checked = current == null ? 0 : "none".equals(current) ? images.size() + 1 : indexOf(images, current) + 1;
        tvos.picker(getString(R.string.wallpaper_label), getString(R.string.tvos_desc_wallpaper), labels,
                checked == 0 && current != null ? -1 : checked, which -> {
                    if (which == 0) choices.edit().remove(Config.WALLPAPER_CHOICE_KEY).apply();
                    else if (which == images.size() + 1)
                        choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, "none").apply();
                    else choices.edit().putString(Config.WALLPAPER_CHOICE_KEY, images.get(which - 1).getName()).apply();
                    changed();
                    refresh();
                });
    }

    /** As showOpenOnStartPicker(): off, then every app with a TV entry point by name. */
    private void tvosOpenOnStartPicker() {
        PackageManager pm = getPackageManager();
        List<String[]> apps = new ArrayList<>();
        Intent tv = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(tv, 0)) {
            String pkg = r.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            boolean seen = false;
            for (String[] a : apps) seen |= a[1].equals(pkg);
            if (!seen) apps.add(new String[] {r.loadLabel(pm).toString(), pkg});
        }
        Collections.sort(apps, (a, b) -> a[0].compareToIgnoreCase(b[0]));
        String[] labels = new String[apps.size() + 1];
        labels[0] = getString(R.string.off);
        String current = choices.getString(OpenOnStart.KEY, "");
        int checked = 0;
        for (int i = 0; i < apps.size(); i++) {
            labels[i + 1] = apps.get(i)[0];
            if (apps.get(i)[1].equals(current)) checked = i + 1;
        }
        tvos.picker(getString(R.string.open_on_start_label), getString(R.string.tvos_desc_open_on_start), labels,
                checked, which -> {
                    if (which == 0) choices.edit().remove(OpenOnStart.KEY).apply();
                    else {
                        OpenOnStart.skipThisBoot(this);
                        choices.edit().putString(OpenOnStart.KEY, apps.get(which - 1)[1]).apply();
                    }
                    refresh();
                });
    }
}
