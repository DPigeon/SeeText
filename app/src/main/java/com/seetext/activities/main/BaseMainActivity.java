package com.seetext.activities.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.seetext.activities.definition.DefinitionActivity;
import com.seetext.R;
import com.seetext.facedetection.FaceDetection;
import com.seetext.objectdetection.ObjectOverlay;
import com.seetext.profile.SharedPreferenceHelper;
import com.seetext.translator.Translator;
import com.seetext.utils.GraphicOverlay;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import java.util.Objects;

import ai.fritz.core.Fritz;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

/* BaseMainActivity.java
 * The abstract base class for MainActivity containing all the overriding implementation
 */

public abstract class BaseMainActivity extends AppCompatActivity implements RecognitionListener, FaceDetection.Callback, Translator.Callback, ObjectOverlay.Callback {

    protected abstract void setupUI();
    protected abstract void persistentSpeech();
    protected abstract void showPermissions();
    protected abstract void loadProfile();
    protected abstract void loadLanguageFirstTime();
    protected abstract void faceCheckAnimation();
    protected abstract void initializeRecognition();
    protected abstract void startRecognition();
    protected abstract void stopListeningSpeech();
    protected abstract void goToProfileActivity(String firstTime);
    protected abstract int getInputLanguage();
    protected abstract int getOutputLanguage();
    protected abstract boolean connectedToInternet();

    protected String TAG = "MainActivity:";
    protected SharedPreferenceHelper sharedPreferenceHelper;
    protected static final int TTS_DATA_CHECK = 90;
    protected static final int MY_PERMISSIONS = 100; // Request code response for camera & microphone
    protected int inputLanguage = FirebaseTranslateLanguage.EN;
    protected int outputLanguage = FirebaseTranslateLanguage.EN; // Default is english
    protected ImageView userProfileImageView, cameraModeImageView, flashLightImageView, languagesImageView, speechDetectionImageView, objectDetectionImageView, faceCheckImageView, audioImageView;
    protected Spinner languageSpinner;
    protected TextView languageTextView;
    protected boolean faceProcessing = false; // For throttling the calls
    protected long animationDuration = 1000; // milliseconds
    protected boolean faceDetected = false; // For face check imageView anim to run once
    protected FrameLayout progressOverlay; // Loading overlay wheel

    /* Video Variables */
    protected ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    protected PreviewView previewView;
    protected Camera camera;
    protected Preview preview;
    protected int lensFacing = CameraSelector.LENS_FACING_BACK;
    protected GraphicOverlay graphicOverlay;
    protected boolean flashLightStatus = false;

    /* Audio Variables */
    protected static SpeechRecognizer mRecognizer = null;
    protected TextView speechTextView;
    protected AudioManager mAudioManager;
    protected TextToSpeech mTTS;
    protected String ttsSentence; // The last translated sentence to send to TTS

    /* Modes of the app */
    protected enum Mode {
        SpeechRecognition,
        ObjectDetection
    }
    protected Mode currentMode = Mode.SpeechRecognition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fritz.configure(this); // Initialize Fritz
        setContentView(R.layout.activity_main);
        Objects.requireNonNull(getSupportActionBar()).hide(); // Hide the main app bar on top

        /* Running UI Thread to get audio & video permission */
        this.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showPermissions();
            }
        });

        loadProfile();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            setupUI();
        }

        /* Stops any other noise from recognizer when switching activities with microphone */
        if (mRecognizer != null)
            stopListeningSpeech();
    }

    @Override
    protected void onStart() {
        super.onStart();
        loadLanguageFirstTime(); // Check when first time opening the app
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            if (requestCode == MY_PERMISSIONS) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupUI();
                } else {
                    Toast.makeText(this, "You cannot run the app without allowing the camera or microphone!", Toast.LENGTH_LONG).show();
                    this.finish();
                }
            }
        }
    }

    @Override
    public void updateSpeechTextViewPosition(float x, float y, boolean hasFace)  {
        // This function knows that it has detected a face
        if (!hasFace) { // Closing speech recognition
            stopListeningSpeech();
            speechTextView.setText(""); // Reset text
        } else { // Opening speech recognition with speech text
            faceCheckAnimation();
            if (mRecognizer == null) { // Not initialized
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                    initializeRecognition();
                startRecognition();
            }
            speechTextView.setX(x);
            speechTextView.setY(y);
        }
        faceProcessing = false;
    }

    @Override
    public void translateTheText(String text) {
        String sentenceToFitUI = " " + text + " ";
        speechTextView.setText(sentenceToFitUI);
        ttsSentence = text;
    }

    /* Callback for object detection touching words */
    @Override
    public void goToObjectDefinition(String word) {
        if (connectedToInternet()) {
            if (!word.equals("Loading...")) {
                if (getInputLanguage() >= 0) {
                    progressOverlay.setVisibility(View.VISIBLE);
                    Intent intent = new Intent(BaseMainActivity.this, DefinitionActivity.class);
                    intent.putExtra("word", word);
                    intent.putExtra("inputLanguage", getInputLanguage());
                    intent.putExtra("outputLanguage", getOutputLanguage());
                    startActivity(intent);
                } else {
                    // Infinity Loop here BUG
                    goToProfileActivity("yes");
                    Toast.makeText(getApplicationContext(), "Set your language!", Toast.LENGTH_LONG).show();
                }
            }
        } else
            Toast.makeText(getApplicationContext(),"You must be connected to internet to see the definitions!", Toast.LENGTH_LONG).show();
    }

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
        /* Sentences may be null sometimes so we avoid that */
        String sentenceToFitUI = " " + sentence + " ";
        if (currentMode != Mode.ObjectDetection) { // Current mode must be speech detection
            if (speechTextView.getVisibility() == View.INVISIBLE)
                speechTextView.setVisibility(View.VISIBLE);
                audioImageView.setVisibility(View.VISIBLE);
            try {
                if (inputLanguage != outputLanguage) { // Checks if input and output are the same
                    Translator translator = new Translator(getApplicationContext(), getInputLanguage(), getOutputLanguage(), this);
                        translator.downloadModelAndTranslate(outputLanguage, sentence);
                } else
                    speechTextView.setText(sentenceToFitUI); // We show the text like it is
            } catch (Exception ignored) {}

            /* Text Animation */
            speechTextView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            Animation fadeOutAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            fadeOutAnim.setStartTime(5000);
            speechTextView.startAnimation(fadeOutAnim);

            persistentSpeech();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {}

    @Override
    public void onEvent(int eventType, Bundle params) {}

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == TTS_DATA_CHECK) {
            if (resultCode != TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                if (intent != null) {
                    intent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                    startActivity(intent);
                }
            }
        }
    }
}