package com.imdacode.bharatfreetv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class MainActivityUiTest {
    @Test
    public void directMedia3PlayerFillsTheRootFrameLayout() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                FrameLayout root = activity.findViewById(R.id.root);
                PlayerView player = activity.findViewById(R.id.exo_player_view);

                assertNotNull(root);
                assertNotNull(player);
                assertSame(root, player.getParent());
                assertMatchParent(root);
                assertMatchParent(player);
                assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FILL, player.getResizeMode());
            });
        }
    }

    @Test
    public void dthOverlaysHaveTheRequiredBoundsAndColors() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                View guide = activity.findViewById(R.id.navigation_overlay);
                View infoBar = activity.findViewById(R.id.info_bar);
                int expectedGuideWidth = Math.round(320f
                        * activity.getResources().getDisplayMetrics().density);

                assertEquals(expectedGuideWidth, guide.getLayoutParams().width);
                assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, guide.getLayoutParams().height);
                assertEquals(Color.parseColor("#CC111827"),
                        ((ColorDrawable) guide.getBackground()).getColor());
                assertEquals(Color.parseColor("#E6000000"),
                        ((ColorDrawable) infoBar.getBackground()).getColor());
                assertEquals(Gravity.BOTTOM,
                        ((FrameLayout.LayoutParams) infoBar.getLayoutParams()).gravity);
            });
        }
    }

    @Test
    public void dpadCenterOpensOverlayMenu() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                activity.onKeyUp(KeyEvent.KEYCODE_DPAD_CENTER,
                        new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER));
                assertEquals(View.VISIBLE,
                        activity.findViewById(R.id.navigation_overlay).getVisibility());
            });
        }
    }

    @Test
    public void splashUsesDedicatedLayoutAndExpectedDuration() {
        try (ActivityScenario<SplashActivity> scenario =
                     ActivityScenario.launch(SplashActivity.class)) {
            scenario.onActivity(activity -> {
                assertNotNull(activity.findViewById(R.id.splash_root));
                assertEquals(2_500L, SplashActivity.SPLASH_DURATION_MILLIS);
            });
        }
    }

    private static void assertMatchParent(View view) {
        ViewGroup.LayoutParams parameters = view.getLayoutParams();
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, parameters.width);
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, parameters.height);
    }
}
