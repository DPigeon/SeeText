package com.ctext;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateRemoteModel;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;

import androidx.annotation.NonNull;

public class Translator {
    private String TAG = "Translator";
    final FirebaseModelManager modelManager;
    final FirebaseTranslator translator;
    Context context;
    Callback callback = null;

    public Translator(Context context, int input, int output, Callback callback) {
        // Create an English-German translator:
        FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder()
            .setSourceLanguage(input)
            .setTargetLanguage(output)
            .build();
        modelManager = FirebaseModelManager.getInstance();
        translator = FirebaseNaturalLanguage.getInstance().getTranslator(options);
        this.context = context;
        this.callback = callback;
    }

    /* An interface to give the translated text as a response to Main Activity */
    public interface Callback {
        void translateTheText(String text);
    }

    /* Translate text from input to output */
    public void translate(String text) {
        translator.translate(text).addOnSuccessListener(translatedText -> {
                callback.translateTheText(translatedText);
        }).addOnFailureListener(error -> {
            Log.d(TAG, "Could not translate the text. Error: " + error.toString());
            Toast.makeText(context,"There is an error with the translator...", Toast.LENGTH_LONG).show();
        });
    }

    /* Downloads a model requested if not downloaded & / or translates */
    public void downloadModelAndTranslate(int languageId, String text) {
        FirebaseTranslateRemoteModel model = new FirebaseTranslateRemoteModel.Builder(languageId).build();

        modelManager.isModelDownloaded(model).addOnSuccessListener(isDownloaded -> {
            if (isDownloaded)
                translate(text);
            else {
                Toast.makeText(context,"Downloading the language model...", Toast.LENGTH_LONG).show();
                downloadModel(model);
            }
        });
    }

    protected void downloadModel(FirebaseTranslateRemoteModel model) {
        FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder()
                .requireWifi()
                .build();
        modelManager.download(model, conditions).addOnSuccessListener(v -> {
            // Model downloaded
            Toast.makeText(context,"Language model has been downloaded!", Toast.LENGTH_LONG).show();
        }).addOnFailureListener(error -> {
            Log.d(TAG, "Error downloading the model. Error: " + error.toString());
        });
    }

}
