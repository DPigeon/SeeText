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
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
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

public class MainActivity extends UIMainActivity {

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
            goToProfileActivity("yes");
            Toast.makeText(getApplicationContext(), "Create your profile!", Toast.LENGTH_LONG).show();
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
                    assert bitmap != null;
                    Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, Utils.getScreenWidth(), Utils.getScreenHeight(), false);
                    objectDetection.detectObjects(getApplicationContext(), newBitmap, getOutputLanguage());
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
        BaseMainActivity thisActivity = this;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) // Videos
            ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS);
    }

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

    /*
     * Text To Speech
     */
    protected void checkTtsResources() {
        Intent intent = new Intent();
        intent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(intent, TTS_DATA_CHECK);
    }

    protected void initializeTTS() {
        mTTS = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // We get the output language translated
                Locale locale = null;
                for (Locale availableLocale : mTTS.getAvailableLanguages()) {
                    String languageAudioWanted = Utils.getLanguageByTag(getOutputLanguage());
                    if (languageAudioWanted.equals(availableLocale.getDisplayLanguage())) { // If the locale voice is installed on the phone then set it
                        locale = new Locale(availableLocale.toString());
                    } else { // If the locale voice not installed then install it
                        checkTtsResources();
                    }
                }
                int result = mTTS.setLanguage(locale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.d(TAG, "Language not supported");
                    audioImageView.setEnabled(false);
                } else {
                    audioImageView.setEnabled(true); // Enable the audio imageView
                }
            } else {
                Log.d(TAG, "Initialization failed");
            }
        });
    }

    protected void startTTS(String sentence) {
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false); // Unmute sound
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mTTS.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
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