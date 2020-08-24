package com.seetext.activities;

import androidx.test.espresso.Espresso;
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

    public void performClick(int id, boolean check, ViewAssertion... viewAssertion) {
        ViewInteraction viewInteraction;
        if (!check) {
            onView(withId(id))
                    .perform(click());
        } else {
            onView(withId(id))
                    .perform(click())
                    .check(viewAssertion[0]);
        }
    }

    public void checkToast(int id, Matcher<Root> root, ViewAssertion viewAssertion) {
        onView(withId(id))
                .inRoot(root)
                .check(viewAssertion);
    }

    public void performClickOnList(int position) {
        onData(anything())
                .atPosition(position)
                .perform(click());
    }
}
