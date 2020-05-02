package com.ctext;

import android.util.Log;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;

public class Translator {
    private String TAG = "Translator";
    final FirebaseTranslator translator;
    Callback callback = null;

    public Translator(int input, int output, Callback callback) {
        // Create an English-German translator:
        FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder()
            .setSourceLanguage(input)
            .setTargetLanguage(output)
            .build();
        translator = FirebaseNaturalLanguage.getInstance().getTranslator(options);
        this.callback = callback;
    }

    /* An interface to give the translated text as a response to Main Activity */
    public interface Callback {
        void translateTheText(String text);
    }

    public void translate(String text) {
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
            .requireWifi()
            .build();
        translator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void v) {
                // Model downloaded successfully. Okay to start translating.
                translator.translate(text).addOnSuccessListener(translatedText -> {
                        callback.translateTheText(translatedText);
                }).addOnFailureListener(error -> {
                    Log.d(TAG, "Could not translate the text. Error: " + error.toString());
                });
            }
        }).addOnFailureListener(error -> {
            // Model could not be downloaded or other internal error
            Log.d(TAG, "Error downloading the model. Error: " + error.toString());
        });
    }

}
