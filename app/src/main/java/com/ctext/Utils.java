package com.ctext;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

/*
 * The utility class containing some useful reusable functions
 */

public class Utils {
    public static int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException("Rotation must be 0, 90, 180, or 270.");
        }
    }

    protected static ArrayList<String> getLanguageList() {
        ArrayList<String> list = new ArrayList<>();
        Set<Integer> tagSet = FirebaseTranslateLanguage.getAllLanguages();
        for (Integer tag : tagSet) {
            String language = FirebaseTranslateLanguage.languageCodeForLanguage(tag);
            String stringLang = Locale.forLanguageTag(language).getDisplayName();
            list.add(stringLang);
        }
        return list;
    }

}
