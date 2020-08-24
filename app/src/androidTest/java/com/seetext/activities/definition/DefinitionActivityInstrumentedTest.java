package com.seetext.activities.definition;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.seetext.R;
import com.seetext.activities.ActionActivityFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DefinitionActivityInstrumentedTest {

    String word = "Test";
    int inputLanguage = FirebaseTranslateLanguage.EN;
    int outputLanguage = FirebaseTranslateLanguage.IT; // English <--> Italian

    @Rule
    public ActivityTestRule<DefinitionActivity> activityRule =
            new ActivityTestRule<DefinitionActivity>(DefinitionActivity.class) {
                @Override
                protected Intent getActivityIntent() {
                    Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
                    Intent intent = new Intent(targetContext, DefinitionActivity.class);
                    intent.putExtra("word", word);
                    intent.putExtra("inputLanguage", inputLanguage);
                    intent.putExtra("outputLanguage", outputLanguage);
                    return intent;
                }
            };

    @Test
    public void testPressingTranslateSwitch() {
        /* Performs click on language switch */
        ActionActivityFactory.assertView(R.id.languageSwitch, true, matches(isDisplayed()));

        /* Checks if language on switch is the same text */
        ActionActivityFactory.assertView(R.id.languageSwitch, false, matches(withText("Italian")));
    }
}

