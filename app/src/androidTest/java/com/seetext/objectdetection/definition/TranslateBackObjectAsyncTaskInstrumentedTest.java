package com.seetext.objectdetection.definition;

import android.content.Context;

import com.seetext.translator.Translator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TranslateBackObjectAsyncTaskInstrumentedTest {

    Context context;
    Translator translator;
    int inputLanguage = FirebaseTranslateLanguage.EN;
    int outputLanguage = FirebaseTranslateLanguage.FR; // French
    final String type = "noun";
    final String definition = "A unit testing is a software testing method by which individual units of source code are tested to determine whether they are fit for use.";
    final String example = "I wrote some unit tests yesterday to test parts of my software.";

    @Before
    public void initialize() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        translator = new Translator(context, inputLanguage, outputLanguage);
    }

    @Test
    public void testTranslateWithModelNotDownloaded() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        TranslateBackObjectAsyncTask testTask = new TranslateBackObjectAsyncTask(context, outputLanguage) {
            @Override
            protected void onPostExecute(String[] result) {
                assertNotNull(result);
                assertTrue(result.length > 0);
                String expectedDefinition = "Loading...";
                assertEquals(expectedDefinition, result[1]);
                String expectedExample = "Loading...";
                assertEquals(expectedExample, result[2]);
                latch.countDown();
            }
        };
        testTask.execute(type, definition, example);
        latch.await();
    }

    @Test
    @Ignore("Needs Rework")
    public void testSuccessfulRequest() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        TranslateBackObjectAsyncTask testTask = new TranslateBackObjectAsyncTask(context, outputLanguage) {
            @Override
            protected void onPostExecute(String[] result) {
                assertNotNull(result);
                assertTrue(result.length > 0);
                // TODO: We have to make the phone download a model here to test properly
                latch.countDown();
            }
        };
        testTask.execute(type, definition, example);
        latch.await();
    }
}
