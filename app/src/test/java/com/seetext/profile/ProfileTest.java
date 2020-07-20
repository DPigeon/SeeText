package com.seetext.profile;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ProfileTest {

    protected int inputLanguage = FirebaseTranslateLanguage.EN;
    protected int outputLanguage = FirebaseTranslateLanguage.FR;
    protected int lensFacing = 0;
    protected int mode = 1;

    Profile profile;

    @Before
    public void setupMock() {
        profile = new Profile(inputLanguage, outputLanguage, lensFacing, mode);
    }

    @Test
    public void testSetLanguage() {
        profile.setLanguage(inputLanguage);
        assertEquals(11, profile.getLanguage());
    }

    @Test
    public void testGetLanguage() {
        profile.setLanguage(inputLanguage);
        int language = profile.getLanguage();
        assertEquals(11, language);
    }

    @Test
    public void testSetLanguageOutput() {
        profile.setLanguageOutput(outputLanguage);
        assertEquals(17, profile.getLanguageOutputId());
    }

    @Test
    public void testGetLanguageOutput() {
        profile.setLanguageOutput(outputLanguage);
        int language = profile.getLanguageOutputId();
        assertEquals(17, language);
    }

    @Test
    public void testSetLensFacing() {
        profile.setLensFacing(lensFacing);
        assertEquals(0, profile.getLensFacing());
    }

    @Test
    public void testGetLensFacing() {
        profile.setLensFacing(lensFacing);
        int lens = profile.getLensFacing();
        assertEquals(0, lens);
    }

    @Test
    public void testGetMode() {
        profile.setMode(mode);
        assertEquals(1, profile.getMode());
    }

    @Test
    public void testSetMode() {
        profile.setMode(mode);
        int m = profile.getMode();
        assertEquals(1, m);
    }
}
