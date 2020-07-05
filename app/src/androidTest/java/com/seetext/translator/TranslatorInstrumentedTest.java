package com.seetext.translator;

import android.Manifest;
import android.content.Context;
import android.widget.Toast;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.seetext.activities.main.MainActivity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class TranslatorInstrumentedTest {

    Context context;
    Translator translator;
    Translator.TranslatorCallback cb;
    String word = "teste";
    String translatedWord = "";

    @Rule
    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            );

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
