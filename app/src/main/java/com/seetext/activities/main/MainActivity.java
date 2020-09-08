package com.seetext.activities.main;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.seetext.Mode;
import com.seetext.activities.IntroActivity;
import com.seetext.activities.profile.ProfileActivity;
import com.seetext.R;
import com.seetext.facedetection.FaceDetection;
import com.seetext.objectdetection.ObjectDetection;
import com.seetext.profile.Profile;
import com.seetext.profile.SharedPreferenceHelper;
import com.seetext.utils.Utils;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/* MainActivity.java
 * The MainActivity with CameraX library, Speech Recognition & a Face Detection callback to update the speech text.
 * The speech text should move every time the app recognizes a face near the mouth of the speaker.
 */

public class MainActivity extends AbstractInterfacesMainActivity {

    protected String TAG = "MainActivity:";

    protected void loadLanguageFirstTime() {
        int outputLang = sharedPreferenceHelper.getLanguageOutput();
        if (outputLang != -1) { // First time using the app
            String language = FirebaseTranslateLanguage.languageCodeForLanguage(outputLang);
            String stringLang = Locale.forLanguageTag(language).getDisplayName();
            if (languageTextView != null)
                languageTextView.setText(stringLang);
        }
    }

    /* Loads profile if needed */
    protected void loadProfile() {
        sharedPreferenceHelper = new SharedPreferenceHelper(this.getSharedPreferences("ProfilePreference", Context.MODE_PRIVATE));
        Profile profile = sharedPreferenceHelper.getProfile();
        int lang = profile.getLanguage();
        int outputLang = profile.getLanguageOutputId();
        int lens = profile.getLensFacing();
        int profileMode = profile.getMode();

        if (lens != -1)
            lensFacing = lens;
        if (profileMode != -1)
            currentMode = Mode.values()[profileMode];
        if (!sharedPreferenceHelper.checkFirstRun()) { // If first run of the app
            setInputLanguage(lang);
            if (outputLang != -1)
                setOutputLanguage(outputLang);
        } else {
            Intent intent = new Intent(MainActivity.this, IntroActivity.class);
            startActivity(intent);
        }
    }

    protected void goToProfileActivity(String firstTime) { // Function that goes from the main activity to profile one
        if (progressOverlay != null)
            progressOverlay.setVisibility(View.VISIBLE);
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        intent.putExtra("lensFacing", lensFacing);
        intent.putExtra("mode", currentMode.ordinal());
        intent.putExtra("firstTime", firstTime);
        stopListeningSpeech();
        startActivity(intent);
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    protected void bindPreview(@NonNull ProcessCameraProvider cameraProvider, int lensFacing) {
        preview = new Preview.Builder()
                .build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        /* Image Processing */
        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, image -> { // https://developer.android.com/training/camerax/analyze
            if (image.getImage() == null)
                return;

            if (currentMode == Mode.SpeechRecognition && connectedToInternet()) {
                graphicOverlay.clear(); // Always destroy the object graphic overlays
                FaceDetection faceDetection = new FaceDetection(graphicOverlay,this);
                if (!faceProcessing) { // Throttle the calls
                    faceProcessing = true;
                    faceDetection.analyzeImage(image);
                }
            } else if (currentMode == Mode.ObjectDetection && connectedToInternet()) {
                ObjectDetection objectDetection = new ObjectDetection(graphicOverlay, this);
                // We get the textureView to get the bitmap image every time for better orientation
                View surfaceOrTexture = previewView.getChildAt(0);
                if (surfaceOrTexture instanceof TextureView) {
                    Bitmap bitmap = ((TextureView) surfaceOrTexture).getBitmap();
                    if (bitmap != null) {
                        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, Utils.getScreenWidth(), Utils.getScreenHeight(), false);
                        objectDetection.detectObjects(getApplicationContext(), newBitmap, getOutputLanguage());
                    }
                }
            }
            image.close();
        });
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
        preview.setSurfaceProvider(previewView.createSurfaceProvider());
    }

    /* Rebinds the preview to keep the lenses working in real time */
    protected void rebindPreview() {
        try {
            cameraProviderFuture.get().unbindAll(); // Unbind all other cameras
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            bindPreview(cameraProviderFuture.get(), lensFacing); // Change lens facing
        } catch (Exception ignored) {}
    }

    /* Used to grant permission from the UI thread */
    protected void showPermissions() {
        AbstractMainActivity thisActivity = this;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) { // Videos
            ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS);
        }
    }

    /*
     * Text To Speech
     */
    protected void initializeTTS() {
        mTTS = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                audioImageView.setEnabled(true);
                mTTS.setOnUtteranceProgressListener(utteranceProgressListener());
            } else {
                Log.d(TAG, "Initialization failed");
                openDialog("TTS Installation", "You don't have the Text-to-Speech installed. " +
                        "Sending you to the installation...", false); // Install the text-to-speech app
            }
        });
    }

    private UtteranceProgressListener utteranceProgressListener() {
        return new UtteranceProgressListener() {
            @Override
            public void onStart(String s) {
                // Speaking started
                runOnUiThread(() -> {
                    audioImageView.setEnabled(false);
                });
            }

            @Override
            public void onDone(String s) {
                // Speaking stopped
                runOnUiThread(() -> {
                    audioImageView.setEnabled(true);
                });
            }

            @Override
            public void onError(String s) {
            }
        };
    }

    protected void startTTS(String sentence) {
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false); // Unmute sound
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "");
        mTTS.speak(sentence, TextToSpeech.QUEUE_FLUSH, params, "4b89afa7-8c1c-4e80-9312-e85e8160814a");
    }

    protected void stopTTS() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
            mTTS = null;
        }
    }

    /*
     * Utilities
     */
    protected void flashLight(boolean status) {
        camera.getCameraControl().enableTorch(status);
        flashLightStatus = status;
        if (status) {
            flashLightImageView.setImageResource(R.drawable.flash_light_enabled);
        } else {
            flashLightImageView.setImageResource(R.drawable.flash_light);
        }
    }

    protected void faceCheckAnimation() {
        if (!faceDetected && currentMode != Mode.ObjectDetection ) {
            faceDetected = true; // Run once when mode chosen
            faceCheckImageView.setVisibility(View.VISIBLE);
            // Movements to fit all screens
            double movementBefore = Utils.getScreenHeight() * 0.1; // 10% from bottom
            double movementAfter = Utils.getScreenHeight() * 0.2; // to 20% from bottom
            ObjectAnimator animatorY = ObjectAnimator.ofFloat(faceCheckImageView, "y", Utils.getScreenHeight() - (int)movementBefore, Utils.getScreenHeight() - (int)movementAfter);
            ObjectAnimator fadeInAnimation = ObjectAnimator.ofFloat(faceCheckImageView, View.ALPHA, 0.0F, 1.0F);
            ObjectAnimator fadeOutAnimation = ObjectAnimator.ofFloat(faceCheckImageView, View.ALPHA, 1.0F, 0.0F);
            animatorY.setDuration(animationDuration);
            fadeInAnimation.setDuration(animationDuration);
            fadeOutAnimation.setStartDelay(animationDuration * 4);
            fadeOutAnimation.setDuration(animationDuration);
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animatorY, fadeInAnimation, fadeOutAnimation);
            animatorSet.start();
        }
    }

    /* Checks if we have a wifi or LTE connection */
    protected boolean connectedToInternet() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo[] netInfo = cm.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    /*
     * Getters & Setters
     */
    protected void setInputLanguage(int number) {
        inputLanguage = number;
    }

    protected int getInputLanguage() {
        return inputLanguage;
    }

    protected void setOutputLanguage(int number) {
        outputLanguage = number;
    }

    protected int getOutputLanguage() {
        return outputLanguage;
    }
}