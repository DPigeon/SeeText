package com.ctext.objectdetection.definition;

import android.content.Context;
import android.os.AsyncTask;

import com.ctext.translator.Translator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

/*
 * AsyncTask used to retranslate back the detected object to english
 */

public class TranslateBackObjectAsyncTask extends AsyncTask<String, Void, String> {
    private String TAG = "TranslateBackObjectAsyncTask";
    private Translator translator;
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
    protected String doInBackground(String... word) {
        translator = new Translator(context, outputLanguage, FirebaseTranslateLanguage.EN); // Output must be english
        String translatedWord = translator.translateObject(word[0], FirebaseTranslateLanguage.EN);
        return translatedWord;
    }

}

