package com.ctext;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.RootMatchers.isSystemAlertWindow;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityInstrumentedTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Test
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
}


