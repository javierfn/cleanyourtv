package io.github.maxhayim.brightness;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * Switches night mode in one press, for a remote button ("hold:190:io.github.maxhayim.brightness/.NightToggleActivity"
 * in the launcher's app_keys) or another app. Without extras it toggles: on (dimming with it) when night mode was off
 * or on a schedule, and on the next press back to what it replaced, the schedule or off. An extra "night" of "on" or
 * "off" sets it instead (off also puts a replaced schedule back), and "bar" false skips the quick bar that shows the
 * result. No window of its own.
 */
public class NightToggleActivity extends Activity {
    static final String EXTRA_NIGHT = "night";
    static final String EXTRA_BAR = "bar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DimService.canDraw(this)) {
            Dimming.Settings s = Prefs.load(this);
            String wanted = getIntent().getStringExtra(EXTRA_NIGHT);
            int delta = getIntent().getIntExtra("night_brightness_delta", 0);
            if ("auto".equals(wanted) || delta != 0) {
                // From the home screen's menu: back to the schedule, or the night brightness a step up or down.
                if ("auto".equals(wanted)) {
                    s.night = Dimming.Night.SCHEDULED;
                    s.override = -1;
                }
                if (delta != 0) s.nightBrightness = Math.max(10, Math.min(100, s.nightBrightness + delta));
                s.on = true;
                Prefs.save(this, s);
                DimService.start(this);
                finish();
                return;
            }
            if ((wanted == null || "flip".equals(wanted)) && s.night == Dimming.Night.SCHEDULED) {
                // On a schedule: the other side until the schedule's next start or end, keeping the schedule.
                Dimming.flip(s, DimService.minuteNow(), System.currentTimeMillis());
                s.on = true;
                Prefs.save(this, s);
                DimService.start(this);
                if (getIntent().getBooleanExtra(EXTRA_BAR, true)) startActivity(new Intent(this, QuickActivity.class));
                finish();
                return;
            }
            if ("flip".equals(wanted)) wanted = null;
            boolean turnOn = wanted == null ? s.night != Dimming.Night.ON : "on".equals(wanted);
            Dimming.Night replaced = Prefs.nightBeforeToggle(this);
            if (!turnOn) Prefs.setNightBeforeToggle(this, null);
            else if (s.night != Dimming.Night.ON) Prefs.setNightBeforeToggle(this, s.night);
            s.night = Dimming.nightAfterToggle(turnOn, replaced);
            if (turnOn) s.on = true;
            Prefs.save(this, s);
            DimService.start(this);
        }
        if (getIntent().getBooleanExtra(EXTRA_BAR, true)) startActivity(new Intent(this, QuickActivity.class));
        finish();
    }
}
