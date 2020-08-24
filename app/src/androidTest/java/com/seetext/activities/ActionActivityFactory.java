package com.seetext.activities;

import androidx.test.espresso.Root;
import androidx.test.espresso.ViewAssertion;
import androidx.test.espresso.ViewInteraction;

import org.hamcrest.Matcher;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
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

    public static void assertView(int id, boolean click, ViewAssertion viewAssertion) {
        if (click) {
            onView(withId(id))
                    .perform(click())
                    .check(viewAssertion);
        } else {
            onView(withId(id))
                    .check(viewAssertion);
        }
    }

    public static void assertToast(int id, Matcher<Root> root, ViewAssertion viewAssertion) {
        onView(withId(id))
                .inRoot(root)
                .check(viewAssertion);
    }
}
