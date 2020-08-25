package com.seetext.utils;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {

    @Test
    public void testDegreesToFirebaseRotation() {
        int rotation = 90;
        int expected = FirebaseVisionImageMetadata.ROTATION_90;
        int firebaseRotation = Utils.degreesToFirebaseRotation(rotation);
        assertEquals(expected, firebaseRotation);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIllegalDegreesToFirebaseRotation() {
        int rotation = 120;
        Utils.degreesToFirebaseRotation(rotation);
    }

    @Test
    public void testGetLanguageList() {
        int expectedSize = 59;
        String expectedLanguage = "English";
        ArrayList<String> list = Utils.getLanguageList();
        assertEquals(expectedSize, list.size());
        assertEquals(expectedLanguage, list.get(FirebaseTranslateLanguage.EN));
    }

    @Test
    public void testFilterBadWord() {
        String text = "f*cking hell f*cker f***";
        String filteredText = Utils.filterBadWord(text);
        assertEquals("fcking hell fcker f", filteredText);
    }
}
