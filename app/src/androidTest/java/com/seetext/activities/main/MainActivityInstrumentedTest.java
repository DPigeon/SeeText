package com.seetext.activities.main;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.rule.IntentsTestRule;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import com.seetext.R;
import com.seetext.activities.main.AbstractMainActivity;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.RootMatchers.isSystemAlertWindow;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityInstrumentedTest {

    @Rule
    public ActivityTestRule<AbstractMainActivity> activityRule =
            new ActivityTestRule<>(AbstractMainActivity.class);

    @Rule
    public IntentsTestRule<AbstractMainActivity> intentsTestRule =
            new IntentsTestRule<>(AbstractMainActivity.class);

    @Test
    @Ignore("Needs rework")
    public void testPressingProfileButton() {
        Intents.init();
        onView(ViewMatchers.withId(R.id.userProfileImageView))
                .perform(click());
        Intents.release();
    }

    @Test
    @Ignore("Needs rework")
    public void testPressingLanguageDropdownButton() {
        onView(withId(R.id.languagesImageView))
                .perform(click())
                .inRoot(isSystemAlertWindow());
    }
}


