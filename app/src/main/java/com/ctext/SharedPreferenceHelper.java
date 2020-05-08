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
        editor.apply(); // Using apply instead of commit now
    }

    public Profile getProfile() { // Getter for profile with different keys
        int language = sharedPreferences.getInt("ProfileLanguage", 0);
        return new Profile(language);
    }
}
