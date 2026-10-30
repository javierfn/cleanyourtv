package io.github.maxhayim.brightness;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import java.util.Calendar;

/**
 * Read-only: whether it is night for Brightness right now (schedule, manual switch and master switch together), so
 * a home screen can follow it, e.g. with a night wallpaper collection. content://io.github.maxhayim.brightness.night
 * answers one row with a "night" column, 1 or 0. It reveals nothing else and changes nothing.
 */
public class NightStateProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Dimming.Settings s = Prefs.load(getContext());
        Calendar now = Calendar.getInstance();
        int minute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        boolean night = s.on && Dimming.nightNow(s, minute);
        MatrixCursor c = new MatrixCursor(new String[]{"night"});
        c.addRow(new Object[]{night ? 1 : 0});
        return c;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
