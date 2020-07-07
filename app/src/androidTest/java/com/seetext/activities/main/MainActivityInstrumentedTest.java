package com.seetext.activities.main;

import android.Manifest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfo;
import androidx.camera.core.TorchState;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.seetext.R;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isSystemAlertWindow;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.matcher.ViewMatchers.hasBackground;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityInstrumentedTest {

    MainActivity mainActivity;

    @Rule
    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            );

    @Rule
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<>(MainActivity.class, false, false);

    @Rule
    public IntentsTestRule<MainActivity> intentsTestRule =
            new IntentsTestRule<>(MainActivity.class);

    @Before
    public void initialize() {
        mainActivity = activityRule.getActivity();
    }

    @Test
    @Ignore("TODO: init called twice?")
    public void testPressingProfileButton() {
        Intents.init();
        onView(withId(R.id.userProfileImageView))
                .perform(click());
        Intents.release();
    }

    @Test
    public void testPressingLanguageDropdownButton() {
        onView(withId(R.id.languagesImageView))
                .perform(click())
                .inRoot(isSystemAlertWindow());
    }

    @Test
    public void testFlashLight() {
        onView(withId(R.id.flashLightImageView))
                .perform(click())
                .check(ViewAssertions.matches(isDisplayed()));
    }
}


