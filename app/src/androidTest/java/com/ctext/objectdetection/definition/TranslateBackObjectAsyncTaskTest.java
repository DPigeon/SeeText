package com.ctext.objectdetection.definition;

import android.content.Context;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TranslateBackObjectAsyncTaskTest {

    Context context;
    int inputLanguage = FirebaseTranslateLanguage.FR; // French
    final String type = "noun";
    final String definition = "A unit testing is a software testing method by which individual units of source code are tested to determine whether they are fit for use.";
    final String example = "I wrote some unit tests yesterday to test parts of my software.";

    @Test
    public void testTranslateBackObjectToFrench() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        TranslateBackObjectAsyncTask testTask = new TranslateBackObjectAsyncTask(context, inputLanguage) {
            @Override
            protected void onPostExecute(String[] result) {
                assertNotNull(result);
                assertTrue(result.length > 0);
                // For a new cellphone instance on the CI pipeline, the expected will be "Loading..."
                // TODO: Make it download the right model before test
                String expectedDefinition = "Un test de l'unité est une méthode d'essai de logiciel par laquelle des unités individuelles du code source sont testées pour déterminer s'ils sont adaptés à l'utilisation.";
                assertEquals(expectedDefinition, result[1]);
                String expectedExample = "J'ai écrit quelques tests unitaires hier pour tester des parties de mon logiciel.";
                assertEquals(expectedExample, result[2]);
                latch.countDown();
            }
        };
        testTask.execute(type, definition, example);
        latch.await();
    }
}
