package com.seetext.profile;

import android.content.SharedPreferences;

/*
 * The SharedPreferenceHelper helps storing value in the app as a profile
 */

public class SharedPreferenceHelper {

    private SharedPreferences sharedPreferences;
    public final String KEY_LANGUAGE = "ProfileLanguage";
    public final String KEY_LANGUAGE_OUTPUT = "ProfileLanguageOutput";
    public final String KEY_LENS_FACING = "ProfileLensFacing";
    public final String KEY_MODE = "ProfileMode";
    public final String KEY_FIRST_RUN = "firstRun";

    public SharedPreferenceHelper(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void saveProfile(Profile profile)  {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(KEY_LANGUAGE, profile.getLanguage());
        editor.putInt(KEY_LANGUAGE_OUTPUT, profile.getLanguageOutputId());
        editor.putInt(KEY_LENS_FACING, profile.getLensFacing());
        editor.putInt(KEY_MODE, profile.getMode());
        editor.apply(); // Using apply instead of commit now
    }

    public int getLanguageInput() {
        return sharedPreferences.getInt(KEY_LANGUAGE, -1);
    }

    public int getLanguageOutput() {
        return sharedPreferences.getInt(KEY_LANGUAGE_OUTPUT, -1);
    }

    public Profile getProfile() {
        int language = sharedPreferences.getInt(KEY_LANGUAGE, -1);
        int output = sharedPreferences.getInt(KEY_LANGUAGE_OUTPUT, -1);
        int lensFacing = sharedPreferences.getInt(KEY_LENS_FACING, -1);
        int mode = sharedPreferences.getInt(KEY_MODE, -1);
        return new Profile(language, output, lensFacing, mode);
    }

    // If first app installed, then it will make it pass this if statement and assign firstRun = true.
    // Then it will assign firstRun to false and if statement wil never rerun again
    public boolean checkFirstRun() {
        if (sharedPreferences.getBoolean(KEY_FIRST_RUN, true)) {
            sharedPreferences.edit().putBoolean(KEY_FIRST_RUN, false).apply();
            return true;
        }
        return false;
    }
}
