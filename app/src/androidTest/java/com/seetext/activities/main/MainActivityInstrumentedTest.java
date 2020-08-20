package com.seetext.activities.main;

import android.Manifest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.TorchState;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.seetext.Mode;
import com.seetext.R;
import com.seetext.utils.Utils;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isSystemAlertWindow;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;
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
        mainActivity = intentsTestRule.getActivity();
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
    public void testPressingSpeechDetectionButton() {
        onView(withId(R.id.speechDetectionImageView))
                .perform(click())
                .check(matches(isDisplayed()));

        assertEquals(Mode.SpeechRecognition, mainActivity.currentMode);
    }

    @Test
    public void testPressingObjectDetectionButton() {
        onView(withId(R.id.objectDetectionImageView))
                .perform(click())
                .check(matches(isDisplayed()));

        assertEquals(Mode.ObjectDetection, mainActivity.currentMode);
    }

    @Test
    public void testPressingCameraLens() {
        onView(withId(R.id.cameraModeImageView))
                .perform(click())
                .check(matches(isDisplayed()));

        // Initialized on back first
        assertEquals(CameraSelector.LENS_FACING_FRONT, mainActivity.lensFacing);
    }

    @Test
    public void testPressingLanguageDropdownButton() {
        int czechPosition = 6;
        onView(withId(R.id.languagesImageView))
                .perform(click())
                .inRoot(isSystemAlertWindow());

        onData(anything())
                .atPosition(czechPosition)
                .perform(click());
        onView(withId(R.id.languageTextView))
                .check(matches(withText(Utils.getLanguageByTag(czechPosition))));
    }

    @Test
    public void testBackFlashLight() {
        int lens = mainActivity.lensFacing;
        if (lens != CameraSelector.LENS_FACING_BACK) {
            onView(withId(R.id.cameraModeImageView))
                    .perform(click());
        }
        int off = mainActivity.camera.getCameraInfo().getTorchState().getValue();
        assertEquals(TorchState.OFF, off);

        onView(withId(R.id.flashLightImageView))
                .perform(click())
                .check(matches(isDisplayed()));

        int on = mainActivity.camera.getCameraInfo().getTorchState().getValue();
        assertEquals(TorchState.ON, on);
    }

    @Test
    public void testFrontFlashLight() {
        int lens = mainActivity.lensFacing;
        if (lens == CameraSelector.LENS_FACING_BACK) {
            onView(withId(R.id.cameraModeImageView))
                    .perform(click());
        }

        onView(withId(R.id.flashLightImageView))
                .perform(click())
                .check(matches(isDisplayed()));

        onView(withId(R.id.frontCameraOverlayImageView))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testSwapLanguageButton() {
        int input = 11; // English
        int output = 16; // Finnish
        mainActivity.setInputLanguage(input);
        mainActivity.setOutputLanguage(output);

        onView(withId(R.id.swapLanguageImageView))
                .perform(click()); // Swap

        onView(withId(R.id.inputLanguageTextView))
                .check(matches(withText(Utils.getLanguageByTag(output))));
        onView(withId(R.id.languageTextView))
                .check(matches(withText(Utils.getLanguageByTag(input))));
    }
}


