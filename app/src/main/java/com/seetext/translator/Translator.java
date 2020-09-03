package com.seetext.translator;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateRemoteModel;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;
import com.seetext.activities.main.MainActivity;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

/*
 * Translator using the Firebase API. It checks if the language model is downloaded. If it is, translate.
 * It it isn't downloaded, download it and translate afterwards
 */

public class Translator {

    private String TAG = "Translator";
    final FirebaseModelManager modelManager;
    final FirebaseTranslator translator;
    FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().build();
    Context context;
    TranslatorCallback callback = null;

    /* Used for translating in speech recognition using a callback */
    public Translator(Context context, int input, int output, TranslatorCallback callback) {
        FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder()
                .setSourceLanguage(input)
                .setTargetLanguage(output)
                .build();
        modelManager = FirebaseModelManager.getInstance();
        translator = FirebaseNaturalLanguage.getInstance().getTranslator(options);
        this.context = context;
        this.callback = callback;
    }

    /* Used for translating in object detection without callbacks of translator */
    public Translator(Context context, int input, int output) {
        FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder()
                .setSourceLanguage(input)
                .setTargetLanguage(output)
                .build();
        modelManager = FirebaseModelManager.getInstance();
        translator = FirebaseNaturalLanguage.getInstance().getTranslator(options);
        this.context = context;
    }

    /* Translate text from input to output */
    public void translate(String text) {
        translator.downloadModelIfNeeded(conditions).addOnSuccessListener(v -> {
            translator.translate(text).addOnSuccessListener(translatedText -> {
                callback.translateTheText(translatedText);
            }).addOnFailureListener(error -> {
                Log.d(TAG, "Could not translate the text. Error: " + error.toString());
                Toast.makeText(context,"There is an error with the translator...", Toast.LENGTH_LONG).show();
            });
        }).addOnFailureListener(error -> Log.d(TAG, "Downloading model if needed has an error: " + error.toString()));
    }

    public String translateObject(String text, int languageId) {
        FirebaseTranslateRemoteModel model = new FirebaseTranslateRemoteModel.Builder(languageId).build();
        boolean isDownloaded = false;
        String tText = "";

        Task downloadTask = modelManager.isModelDownloaded(model);
        try {
            Tasks.await(downloadTask);
            isDownloaded = (boolean)downloadTask.getResult();
        } catch (InterruptedException | ExecutionException error) {
            error.printStackTrace();
        }
        if (isDownloaded) {
            Task textTask = translator.translate(text);
            // Asynchronous call awaiting for the promise
            try {
                Tasks.await(textTask);
                tText = Objects.requireNonNull(textTask.getResult()).toString();
            } catch (InterruptedException | ExecutionException error) {
                error.printStackTrace();
            }
        } else {
            downloadModel(model);
            return "Loading...";
        }
        return tText;
    }

    /* Downloads a model requested if not downloaded & / or translates */
    public void downloadModelAndTranslate(Context context, int languageId, String text) {
        FirebaseTranslateRemoteModel model = new FirebaseTranslateRemoteModel.Builder(languageId).build();

        modelManager.isModelDownloaded(model).addOnSuccessListener(isDownloaded -> {
            if (isDownloaded) {
                translate(text);
            } else {
                openDownloadDialog(context, "Language Model Download", "To be able to translate, " +
                        "you must download the language model selected (around 30MB).", model);
            }
        });
    }

    protected void openDownloadDialog(Context context, String title, String message, FirebaseTranslateRemoteModel model) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Download", (dialog, which) -> {
                    downloadModel(model);
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public void checkAndDownloadModel(FirebaseTranslateRemoteModel model) {
        modelManager.isModelDownloaded(model).addOnSuccessListener(isDownloaded -> {
           if (!isDownloaded) {
               modelManager.download(model, conditions).addOnSuccessListener(v -> {
                   // Model downloaded
                   Toast.makeText(context,"Language model has been downloaded!", Toast.LENGTH_LONG).show();
               }).addOnFailureListener(error -> Log.d(TAG, "Error downloading the model. Error: " + error.toString()));
           }
        });
    }

    private void downloadModel(FirebaseTranslateRemoteModel model) {
        modelManager.download(model, conditions).addOnSuccessListener(v -> {
            // Model downloaded
            Toast.makeText(context,"Language model has been downloaded!", Toast.LENGTH_LONG).show();
        }).addOnFailureListener(error -> Log.d(TAG, "Error downloading the model. Error: " + error.toString()));
    }
}
