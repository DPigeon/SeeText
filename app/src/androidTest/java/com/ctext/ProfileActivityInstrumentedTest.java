package com.ctext;

import android.content.Context;
import android.content.Intent;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

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
public class ProfileActivityInstrumentedTest {

    @Rule
    public ActivityTestRule<ProfileActivity> activityRule =
            new ActivityTestRule<ProfileActivity>(ProfileActivity.class) {
                @Override
                protected Intent getActivityIntent() {
                    Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
                    Intent result = new Intent(targetContext, ProfileActivity.class);
                    result.putExtra("firstTime", "yes");
                    return result;
                }
            };

    @Test
    public void testPressingSaveButton() {
        /* Performs click on save button */
        onView(withId(R.id.saveButton))
                .perform(click())
                .check(matches(isDisplayed()));

        /* Makes sure toast matches */
        onView(withText("You must choose a language!"))
                .inRoot(withDecorView(not(activityRule.getActivity().getWindow().getDecorView())))
                .check(matches(isDisplayed()));
    }
}
