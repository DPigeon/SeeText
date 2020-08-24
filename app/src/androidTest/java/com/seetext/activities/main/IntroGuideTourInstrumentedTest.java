package com.seetext.activities.main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.seetext.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class IntroGuideTourInstrumentedTest {

    MainActivity mainActivity;

    @Rule
    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            );

    @Rule
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<MainActivity>(MainActivity.class) {
                @Override
                protected Intent getActivityIntent() {
                    Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
                    Intent result = new Intent(targetContext, MainActivity.class);
                    result.putExtra("firstTime", "yes");
                    return result;
                }
            };

    @Before
    public void initialize() {
        mainActivity = activityRule.getActivity();
        mainActivity.sharedPreferenceHelper.resetFirstRun(true);
    }

    @Test
    public void testSkipButton() {
        onView(withId(R.id.skip))
                .perform(click());

        onView(withText("You have to give permissions first!"))
                .inRoot(withDecorView(not(activityRule.getActivity().getWindow().getDecorView())))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testCompleteIntro() {
        int numSteps = 3;
        for (int i = 0; i < numSteps; i++) {
            onView(withId(R.id.next))
                    .perform(click());
        }

        onView(withId(R.id.done))
                .perform(click());

        onView(withText("Create your profile!"))
                .inRoot(withDecorView(not(activityRule.getActivity().getWindow().getDecorView())))
                .check(matches(isDisplayed()));

        /*onData(withId(R.id.languagesScrollView))
                .onChildView(withId(R.id.languagesRadioGroup))

        onView(withId(R.id.saveButton))
                .perform(click());

        onView(withText("Your profile has been saved!"))
                .inRoot(withDecorView(not(activityRule.getActivity().getWindow().getDecorView())))
                .check(matches(isDisplayed()));

        onView(withId(R.id.userProfileImageView))
                .perform(click());
        onView(withId(R.id.speechDetectionImageView))
                .perform(click());
        onView(withId(R.id.swapLanguageImageView))
                .perform(click());
        onView(withId(R.id.inputLanguageTextView))
                .perform(click());
        onView(withId(R.id.languageTextView))
                .perform(click());
        onView(withId(R.id.objectDetectionImageView))
                .perform(click());
        onView(withId(R.id.languageTextView))
                .perform(click());
        onView(withId(R.id.flashLightImageView))
                .perform(click());
        onView(withId(R.id.cameraModeImageView))
                .perform(click());
        onView(withText("You have completed the tutorial!"))
                .inRoot(withDecorView(not(activityRule.getActivity().getWindow().getDecorView())))
                .check(matches(isDisplayed()));*/
    }
}