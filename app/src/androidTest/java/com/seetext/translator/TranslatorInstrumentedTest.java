package com.seetext.translator;

import android.content.Context;
import android.widget.Toast;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.seetext.activities.main.MainActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class TranslatorInstrumentedTest {

    Context context;
    Translator translator;
    Translator.Callback cb;
    String word = "teste";
    String translatedWord = "";

    @Rule
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<>(MainActivity.class);

    @Before
    public void initialize() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        cb = text -> Toast.makeText(context, text, Toast.LENGTH_LONG).show();
    }

    @Test
    public void testSuccessfulRequest() {
        int inputLanguage = FirebaseTranslateLanguage.FR;
        int outputLanguage = FirebaseTranslateLanguage.ES;
        translator = new Translator(context, inputLanguage, outputLanguage, cb);
        translator.translate(word);
    }
}
