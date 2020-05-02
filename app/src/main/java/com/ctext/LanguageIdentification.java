package com.ctext;

import android.util.Log;

import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

public class LanguageIdentification {
    private String TAG = "LanguageIdentifier";
    FirebaseLanguageIdentification languageIdentifier;

    public LanguageIdentification() {
        languageIdentifier = FirebaseNaturalLanguage.getInstance().getLanguageIdentification();
    }

    public void identification(String text) {
        languageIdentifier.identifyLanguage(text).addOnSuccessListener(languageCode -> {
            if (languageCode != "und") {
                Log.d(TAG, "Language: " + languageCode);
                // callback.identify(FirebaseTranslateLanguage.languageForLanguageCode(languageCode)); // used for FirebaseTranslator
            } else {
                Log.d(TAG, "Can't identify the language.");
            }
        }).addOnFailureListener(exception -> {
            Log.d(TAG, exception.toString());
        });
    }
}
