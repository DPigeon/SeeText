package com.ctext.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.ctext.profile.Profile;

/*
 * The SharedPreferenceHelper helps storing value in the app as a profile
 */

public class SharedPreferenceHelper {
    private SharedPreferences sharedPreferences;

    public SharedPreferenceHelper(Context context) {
        sharedPreferences = context.getSharedPreferences("ProfilePreference", Context.MODE_PRIVATE);
    }

    public void saveProfile(Profile profile)  {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("ProfileLanguage", profile.getLanguage());
        editor.putInt("ProfileLanguageOutput", profile.getLanguageOutputId());
        editor.putInt("ProfileLensFacing", profile.getLensFacing());
        editor.putInt("ProfileMode", profile.getMode());
        editor.apply(); // Using apply instead of commit now
    }

    public int getLanguageInput() {
        int input = sharedPreferences.getInt("ProfileLanguage", -1);
        return input;
    }

    public int getLanguageOutput() {
        int output = sharedPreferences.getInt("ProfileLanguageOutput", -1);
        return output;
    }

    public Profile getProfile() {
        int language = sharedPreferences.getInt("ProfileLanguage", -1);
        int output = sharedPreferences.getInt("ProfileLanguageOutput", -1);
        int lensFacing = sharedPreferences.getInt("ProfileLensFacing", -1);
        int mode = sharedPreferences.getInt("ProfileMode", -1);
        return new Profile(language, output, lensFacing, mode);
    }

    // If first app installed, then it will make it pass this if statement and assign firstRun = true.
    // Then it will assign firstRun to false and if statement wil never rerun again
    public boolean checkFirstRun() {
        if (sharedPreferences.getBoolean("firstRun", true)) {
            sharedPreferences.edit().putBoolean("firstRun", false).apply();
            return true;
        }
        return false;
    }

}
