package com.ctext.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.ctext.SplashScreenActivity;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SharedPreferenceHelperTest {
    private SharedPreferenceHelper sharedPreferenceHelper;

    @Mock
    SharedPreferences mockSharedPreferences;
    @Mock
    SharedPreferences.Editor mockEditor;

    @Before
    public void setupMock() {
        sharedPreferenceHelper = createMockSharedPreferenceHelper();
    }

    @Test
    public void testSaveProfile() {
        Profile profile = new Profile(0, 0, 0, 0);
        sharedPreferenceHelper.saveProfile(profile);
        Profile actualProfile = sharedPreferenceHelper.getProfile();
        assertEquals(0, actualProfile.getLanguage());
        assertEquals(0, actualProfile.getLanguageOutputId());
        assertEquals(0, actualProfile.getLensFacing());
        assertEquals(0, actualProfile.getMode());
    }

    /* Creates the necessary mocks */
    private SharedPreferenceHelper createMockSharedPreferenceHelper() {
        // TODO: mock the getProfile
        when(mockEditor.commit()).thenReturn(true);
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockSharedPreferences.edit().putInt(anyString(), anyInt())).thenReturn(mockEditor);
        doNothing().when(mockEditor).apply();
        return new SharedPreferenceHelper(mockSharedPreferences);
    }

}
