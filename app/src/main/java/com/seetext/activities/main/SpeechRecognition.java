package com.seetext.activities.main;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.speech.RecognitionListener;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.seetext.translator.Translator;

import java.util.Objects;

public abstract class SpeechRecognition extends UIMainActivity implements RecognitionListener {

    protected abstract void initializeTTS();
    protected abstract void stopTTS();

    @Override
    public void onReadyForSpeech(Bundle params) {}

    @Override
    public void onBeginningOfSpeech() {}

    @Override
    public void onRmsChanged(float rmsDb) {}

    @Override
    public void onBufferReceived(byte[] buffer) {}

    @Override
    public void onEndOfSpeech() {}

    @Override
    public void onError(int error) {
        if (error == SpeechRecognizer.ERROR_AUDIO) {
            Log.d(TAG, "Audio recording error");
        } else if (error == SpeechRecognizer.ERROR_CLIENT) {
            Log.d(TAG, "Client side error");
        } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            Log.d(TAG, "Insufficient permissions");
        } else if (error == SpeechRecognizer.ERROR_NETWORK) {
            Log.d(TAG, "Network error");
        } else if (error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
            Log.d(TAG, "Network timeout");
        } else if (error == SpeechRecognizer.ERROR_NO_MATCH) {
            Log.d(TAG, "No speech match");
        } else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            Log.d(TAG, "Recognizer busy");
        } else if (error == SpeechRecognizer.ERROR_SERVER) {
            Log.d(TAG, "Server error");
        } else if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            Log.d(TAG, "No speech input");
        } else {
            Log.d(TAG, "Unknown error");
        }
        persistentSpeech();
    }

    @Override
    public void onResults(Bundle results) {
        String sentence = Objects.requireNonNull(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)).get(0);
        String sentenceToFitUI = " " + sentence + " ";
        if (currentMode != Mode.ObjectDetection) { // Current mode must be speech detection
            if (speechTextView.getVisibility() == View.INVISIBLE) {
                speechTextView.setVisibility(View.VISIBLE);
            }
            audioImageView.setVisibility(View.VISIBLE);
            try {
                if (inputLanguage != outputLanguage) { // Checks if input and output are the same
                    Translator translator = new Translator(getApplicationContext(), getInputLanguage(), getOutputLanguage(), this);
                    translator.downloadModelAndTranslate(outputLanguage, sentence);
                } else
                    speechTextView.setText(sentenceToFitUI); // We show the text like it is
            } catch (Exception ignored) {}
            textAnimation();
            persistentSpeech();
        }
    }

    private void textAnimation() {
        speechTextView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
        Animation fadeOutAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        fadeOutAnim.setStartTime(5000);
        speechTextView.startAnimation(fadeOutAnim);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {}

    @Override
    public void onEvent(int eventType, Bundle params) {}

    /*
     * SpeechRecognizer
     */
    protected void initializeRecognition() {
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mRecognizer.setRecognitionListener(this);
        initializeTTS();
    }

    protected void startRecognition() {
        // Uses our SharedPreferences to perform recognition in different languages
        if (getInputLanguage() >= 0) {
            String lang = FirebaseTranslateLanguage.languageCodeForLanguage(getInputLanguage());
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);

            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true); // Mutes any sound of beep for listening
            mRecognizer.startListening(intent);
        } else { // If language not set then send back to profile activity
            Toast.makeText(getApplicationContext(), "Your language is not set!", Toast.LENGTH_LONG).show();
        }
    }

    protected void stopListeningSpeech() {
        if (mRecognizer != null) {
            mRecognizer.stopListening();
            mRecognizer.destroy();
            mRecognizer = null;
        }
        if (mTTS != null) {
            stopTTS();
        }
    }

    protected void persistentSpeech() {
        /* Makes the app always listen to inputs */
        if (mRecognizer != null) {
            stopListeningSpeech();
            initializeRecognition();
            startRecognition();
        }
    }
}