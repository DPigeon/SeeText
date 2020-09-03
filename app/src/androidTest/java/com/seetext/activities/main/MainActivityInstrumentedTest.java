package com.seetext.activities.main;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;

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
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.seetext.Mode;
import com.seetext.R;
import com.seetext.activities.profile.ProfileActivity;
import com.seetext.utils.Utils;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isSystemAlertWindow;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static com.seetext.activities.ActionActivityFactory.*;
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
            new ActivityTestRule<MainActivity>(MainActivity.class, false, false) {
                @Override
                protected Intent getActivityIntent() {
                    Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
                    Intent result = new Intent(targetContext, ProfileActivity.class);
                    result.putExtra("firstTime", "no");
                    return result;
                }
            };

    @Rule
    public IntentsTestRule<MainActivity> intentsTestRule =
            new IntentsTestRule<>(MainActivity.class);

    @Before
    public void initialize() {
        mainActivity = intentsTestRule.getActivity();
        mainActivity.sharedPreferenceHelper.resetFirstRun(false); // Not completely working
    }

    @Test
    @Ignore("TODO: init called twice?")
    public void testPressingProfileButton() {
        Intents.init();
        performClick(R.id.userProfileImageView);
        Intents.release();
    }

    @Test
    public void testPressingSpeechDetectionButton() {
        assertView(R.id.speechDetectionImageView, true, isDisplayed());

        assertEquals(Mode.SpeechRecognition, mainActivity.currentMode);
    }

    @Test
    public void testPressingObjectDetectionButton() {
        assertView(R.id.objectDetectionImageView, true, isDisplayed());

        assertEquals(Mode.ObjectDetection, mainActivity.currentMode);
    }

    @Test
    public void testPressingCameraLens() {
        assertView(R.id.cameraModeImageView, true, isDisplayed());

        // Initialized on back first
        assertEquals(CameraSelector.LENS_FACING_FRONT, mainActivity.lensFacing);
    }

    @Test
    public void testPressingLanguageDropdownButton() {
        int czechPosition = 6;
        performClickInRoot(R.id.languagesImageView, isSystemAlertWindow());

        performClickOnList(czechPosition);
        assertView(R.id.languageTextView, false, withText(Utils.getLanguageByTag(czechPosition)));
    }

    @Test
    public void testBackFlashLight() {
        int lens = mainActivity.lensFacing;
        if (lens != CameraSelector.LENS_FACING_BACK) {
            performClick(R.id.cameraModeImageView);
        }
        int off = mainActivity.camera.getCameraInfo().getTorchState().getValue();
        assertEquals(TorchState.OFF, off);

        assertView(R.id.flashLightImageView, true, isDisplayed());

        int on = mainActivity.camera.getCameraInfo().getTorchState().getValue();
        assertEquals(TorchState.ON, on);
    }

    @Test
    public void testFrontFlashLight() {
        int lens = mainActivity.lensFacing;
        if (lens == CameraSelector.LENS_FACING_BACK) {
            performClick(R.id.cameraModeImageView);
        }

        assertView(R.id.flashLightImageView, true, isDisplayed());

        assertView(R.id.frontCameraOverlayImageView, false, isDisplayed());
    }

    @Test
    public void testSwapLanguageButton() {
        int input = 11; // English
        int output = 16; // Finnish
        mainActivity.setInputLanguage(input);
        mainActivity.setOutputLanguage(output);

        performClick(R.id.speechDetectionImageView); // Enable speech mode

        performClick(R.id.swapLanguageImageView); // Swap

        assertView(R.id.inputLanguageTextView, false, withText(Utils.getLanguageByTag(output)));
        assertView(R.id.languageTextView, false, withText(Utils.getLanguageByTag(input)));
    }

    @Test
    @Ignore("TODO later")
    public void testAudioTTSButton() {
        int input = FirebaseTranslateLanguage.EN;
        int output = FirebaseTranslateLanguage.GA; // Irish
        mainActivity.ttsSentence = "duine"; // person in irish
        mainActivity.setInputLanguage(input);
        mainActivity.setOutputLanguage(output);
        mainActivity.audioImageView.setEnabled(true);

        assertView(R.id.audioImageView, true, isDisplayed());
    }
}