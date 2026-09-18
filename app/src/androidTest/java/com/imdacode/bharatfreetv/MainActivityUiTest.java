package com.imdacode.bharatfreetv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class MainActivityUiTest {
    @Test
    public void playerViewportAndBothEnginesFillTheScreen() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                VideoViewport viewport = activity.findViewById(R.id.video_viewport);
                View exoPlayer = activity.findViewById(R.id.exo_player_view);
                View vlcPlayer = activity.findViewById(R.id.vlc_player_view);
                assertNotNull(viewport);
                assertMatchParent(viewport);
                assertMatchParent(exoPlayer);
                assertMatchParent(vlcPlayer);
            });
        }
    }

    @Test
    public void dpadCenterOpensOverlayMenu() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.onKeyUp(KeyEvent.KEYCODE_DPAD_CENTER,
                        new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
                assertEquals(View.VISIBLE, activity.findViewById(R.id.navigation_overlay).getVisibility());
            });
        }
    }

    private static void assertMatchParent(View view) {
        ViewGroup.LayoutParams parameters = view.getLayoutParams();
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, parameters.width);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, parameters.height);
    }
}
