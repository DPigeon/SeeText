package com.seetext.objectdetection.definition;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;

import com.seetext.translator.Translator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

/*
 * AsyncTask used to retranslate back the detected object to english
 */

public class TranslateBackObjectAsyncTask extends AsyncTask<String, Void, String[]> {
    private String TAG = "TranslateBackObjectAsyncTask";
    @SuppressLint("StaticFieldLeak")
    private Context context;
    private int outputLanguage;

    public TranslateBackObjectAsyncTask(Context context, int outputLanguage) {
        this.context = context;
        this.outputLanguage = outputLanguage;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected String[] doInBackground(String... word) {
        String[] translatedWord = new String[3];
        Translator translator;
        if (word.length == 1) { // If params only has 1 word to translate (normally the title)
            // Translating from output language to english for us to be able to find definition
            translator = new Translator(context, outputLanguage, FirebaseTranslateLanguage.EN); // Output must be english
                translatedWord[0] = translator.translateObject(word[0], FirebaseTranslateLanguage.EN);
        } else { // This is used when more params in (used to translate the whole page with type, def and example) from english to whatever output language
            translator = new Translator(context, FirebaseTranslateLanguage.EN, outputLanguage); // Output must be english
            for (int i = 0; i < word.length; i++)
                translatedWord[i] = translator.translateObject(word[i], outputLanguage);
        }
        return translatedWord;
    }

}

