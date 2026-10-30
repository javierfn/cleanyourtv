package com.gathin.tvhome;

import android.content.Context;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Keys of ours in the theme's config.json that Config does not know (night_button). The weather under the clock
 * itself is upstream's weather_now now, read from the Weather add-on, so the launcher stays offline.
 */
final class WeatherLine {
    private WeatherLine() { }

    /** A true/false key of the theme's config.json, read directly (for keys of ours that Config does not know). */
    static boolean configFlag(Context context, String key) {
        JSONObject json = readConfig(context);
        return json != null && json.optBoolean(key, false);
    }

    private static JSONObject readConfig(Context context) {
        File ext = context.getExternalFilesDir(null);
        File file = ext != null ? new File(ext, Config.FILE_NAME) : null;
        if (file == null || !file.isFile()) file = new File(context.getFilesDir(), Config.FILE_NAME);
        if (!file.isFile() || file.length() > 64 * 1024) return null;
        try (InputStream in = new FileInputStream(file)) {
            String text = readAll(in);
            if (text.startsWith("﻿")) text = text.substring(1);
            return new JSONObject(text);
        } catch (Exception e) {
            return null;
        }
    }

    private static String readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }
}
