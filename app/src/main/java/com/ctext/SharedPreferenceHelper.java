package com.ctext;

import android.content.Context;
import android.content.SharedPreferences;

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
        editor.apply(); // Using apply instead of commit now
    }

    public int getLanguageOutput() {
        int output = sharedPreferences.getInt("ProfileLanguageOutput", -1);
        return output;
    }

    public Profile getProfile() {
        int language = sharedPreferences.getInt("ProfileLanguage", -1);
        int output = sharedPreferences.getInt("ProfileLanguageOutput", -1);
        return new Profile(language, output);
    }
}
