package com.gathin.tvhome;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;

/**
 * cast_pill (config.json): the Control Center's Chromecast tile. Google TV's built-in Cast receiver is
 * com.google.android.apps.mediashell. The launcher has no INTERNET permission, so it does not ask the receiver's
 * local web page (eureka_info) for its name: phones see the TV's device name (Settings.Global device_name), which is
 * the name that page reports. The receiver counts as answering while its CastReceiverService stays bound; the bind
 * also keeps Android 14 from freezing Cast while Home is in the background, as ambient-tv's launcher does.
 */
final class CastStatus {
    static final String PACKAGE = "com.google.android.apps.mediashell";
    /** mediashell's own Cast screen (exported, "dejar que otros controlen lo que se envía" and the rest). */
    private static final String SETTINGS_ACTION = "com.google.android.settings.CAST_RECEIVER_SETTINGS";

    /** No Cast receiver on this TV (or it is switched off). */
    static final int MISSING = -1;
    /** Installed, but its service does not answer. */
    static final int DOWN = 0;
    static final int READY = 1;

    private final Context context;
    private boolean bound;
    private volatile boolean connected;
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) { connected = true; }
        @Override public void onServiceDisconnected(ComponentName name) { connected = false; }
        @Override public void onBindingDied(ComponentName name) {
            connected = false;
            // The receiver was updated or restarted: bind again so it comes back and stays awake.
            unbind();
            bind();
        }
        @Override public void onNullBinding(ComponentName name) { connected = true; }
    };

    CastStatus(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Binds the receiver's service, which starts Cast if it was not running and keeps it from being frozen. */
    void bind() {
        if (bound || !installed()) return;
        Intent i = new Intent("com.google.cast.action.BIND").setClassName(PACKAGE, PACKAGE + ".CastReceiverService");
        try {
            bound = context.bindService(i, connection, Context.BIND_AUTO_CREATE);
        } catch (RuntimeException e) {
            bound = false;
        }
        if (!bound) {
            try {
                context.unbindService(connection);
            } catch (RuntimeException ignored) {
                // Nothing was bound.
            }
        }
    }

    void unbind() {
        if (!bound) return;
        bound = false;
        connected = false;
        try {
            context.unbindService(connection);
        } catch (RuntimeException ignored) {
            // Already gone.
        }
    }

    int state() {
        if (!installed()) return MISSING;
        return connected ? READY : DOWN;
    }

    boolean installed() {
        try {
            ApplicationInfo info = context.getPackageManager().getApplicationInfo(PACKAGE, 0);
            return info.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /** The name phones list when they cast: the TV's device name. */
    String name() {
        String n = Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME);
        return TextUtils.isEmpty(n) ? Build.MODEL : n;
    }

    /** mediashell's Cast settings screen, else its App info page in Settings, else null. */
    Intent settingsIntent() {
        Intent own = new Intent(SETTINGS_ACTION).setPackage(PACKAGE);
        if (context.getPackageManager().resolveActivity(own, 0) != null) return own;
        return SystemSettings.appInfo(context, PACKAGE);
    }
}
