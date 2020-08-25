package com.seetext.activities;

import android.view.View;

import androidx.test.espresso.Root;

import org.hamcrest.Matcher;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.anything;

/**
 * Reusable UI actions for instrumented tests
 */

public class ActionActivityFactory {

    public static void performClick(int id) {
        onView(withId(id))
                .perform(click());
    }

    public static void performClickOnList(int position) {
        onData(anything())
                .atPosition(position)
                .perform(click());
    }

    public static void performClickInRoot(int id, Matcher<Root> matcher) {
        onView(withId(id))
                .perform(click())
                .inRoot(matcher);
    }

    public static void assertView(int id, boolean click, Matcher<View> matcher) {
        if (click) {
            onView(withId(id))
                    .perform(click())
                    .check(matches(matcher));
        } else {
            onView(withId(id))
                    .check(matches(matcher));
        }
    }

    public static void assertToast(String text, Matcher<Root> root, Matcher<View> matcher) {
        onView(withText(text))
                .inRoot(root)
                .check(matches(matcher));
    }
}
