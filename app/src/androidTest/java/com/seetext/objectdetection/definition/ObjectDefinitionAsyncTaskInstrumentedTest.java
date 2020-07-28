package com.seetext.objectdetection.definition;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class ObjectDefinitionAsyncTaskInstrumentedTest {

    /* Integration Tests for the OwlBot API */
    @Test
    public void testSuccessfulRequest() throws InterruptedException {
        String word = "laptop";

        final CountDownLatch latch = new CountDownLatch(1);
        ObjectDefinitionAsyncTask testTask = new ObjectDefinitionAsyncTask() {
            @Override
            protected void onPostExecute(JSONObject result) {
                assertNotNull(result);
                try {
                    String actual = result.getString("word");
                    assertEquals(word, actual);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                latch.countDown();
            }
        };
        testTask.execute(word);
        latch.await();
    }

    @Test
    public void testGetValidDefinitions() throws InterruptedException {
        String word = "person";
        String expectedType = "noun";
        String expectedDefinition = "a human being regarded as an individual.";

        final CountDownLatch latch = new CountDownLatch(1);
        ObjectDefinitionAsyncTask testTask = new ObjectDefinitionAsyncTask() {
            @Override
            protected void onPostExecute(JSONObject result) {
                assertNotNull(result);
                try {
                    JSONArray definitionArray = result.getJSONArray("definitions");
                    assertNotNull(definitionArray);

                    JSONObject firstDefinition = (JSONObject) definitionArray.get(0);
                    String type = firstDefinition.getString("type");
                    String definition = firstDefinition.getString("definition");
                    assertEquals(expectedType, type);
                    assertEquals(expectedDefinition, definition);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                latch.countDown();
            }
        };
        testTask.execute(word);
        latch.await();
    }
}
