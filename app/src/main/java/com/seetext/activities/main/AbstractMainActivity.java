package com.seetext.activities.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.seetext.Mode;
import com.seetext.R;
import com.seetext.activities.AbstractActivity;
import com.seetext.profile.SharedPreferenceHelper;
import com.seetext.utils.GraphicOverlay;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import java.util.Objects;

import ai.fritz.core.Fritz;
import androidx.annotation.NonNull;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

/* BaseMainActivity.java
 * The abstract base class for MainActivity containing all the overriding implementation
 * and parameters.
 */

public abstract class AbstractMainActivity extends AbstractActivity {

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
    protected abstract void setInputLanguage(int number);
    protected abstract void setOutputLanguage(int number);
    protected abstract int getInputLanguage();
    protected abstract int getOutputLanguage();
    protected abstract boolean connectedToInternet();

    protected String TAG = "MainActivity:";
    protected Mode currentMode = Mode.SpeechRecognition;
    protected SharedPreferenceHelper sharedPreferenceHelper;
    protected static final int TTS_DATA_CHECK = 90;
    protected static final int MY_PERMISSIONS = 100; // Request code response for camera & microphone
    protected int inputLanguage = FirebaseTranslateLanguage.EN;
    protected int outputLanguage = FirebaseTranslateLanguage.EN; // Default is english
    protected ImageView userProfileImageView, cameraModeImageView, flashLightImageView, languagesImageView, speechDetectionImageView, objectDetectionImageView, faceCheckImageView, audioImageView, frontCameraOverlayImageView, swapLanguageImageView;
    protected Spinner languageSpinner;
    protected TextView languageTextView;
    protected TextView swapInputLanguage;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fritz.configure(this); // Initialize Fritz
        Objects.requireNonNull(getSupportActionBar()).hide(); // Hide the main app bar on top

        showAudioAndVideoPermissions();
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
    protected int getLayoutResourceId() {
        return R.layout.activity_main;
    }

    /**
     * To avoid pressing back button on intro
     */
    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        String firstTime = intent.getStringExtra("firstTime");
        if (firstTime != null) {
            if (firstTime.equals(("no"))) {
                super.onBackPressed();
            }
        }
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

    private void showAudioAndVideoPermissions() {
        /* Running UI Thread to get audio & video permission */
        this.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showPermissions();
            }
        });
    }
}