package io.github.tvextras.hdmiappletv;

import android.app.Activity;
import android.content.Intent;
import android.media.tv.TvContract;
import android.os.Bundle;

/** Switches the TV to HDMI 1, where the Apple TV is plugged in, and gets out of the way. */
public class MainActivity extends Activity {
    private static final String HDMI_1 = "com.mediatek.tvinput/.hdmi.HDMIInputService/HW5";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent view = new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(HDMI_1));
        view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(view);
        } catch (Exception ignored) {
            // No passthrough player on this firmware: nothing else to try.
        }
        finish();
    }
}
