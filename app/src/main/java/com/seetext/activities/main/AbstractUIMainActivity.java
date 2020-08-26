package com.seetext.activities.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.net.Uri;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.seetext.Mode;
import com.seetext.R;
import com.seetext.profile.Profile;
import com.seetext.utils.Utils;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;

/* UIMainActivity.java
 * The abstract base class for MainActivity containing all the UI elements
 */

public abstract class AbstractUIMainActivity extends AbstractMainActivity {

    protected abstract void bindPreview(ProcessCameraProvider cameraProvider, int lensFacing);
    protected abstract void rebindPreview();
    protected abstract void flashLight(boolean flashLightStatus);
    protected abstract void startTTS(String ttsSentence);
    protected abstract void guideTour();

    protected String TAG = "AbstractUIMainActivity:";
    GuideView guideView;
    GuideView.Builder guideViewBuilder;

    @Override
    @SuppressLint({"ClickableViewAccessibility"})
    public void setupUI() {
        previewView = findViewById(R.id.previewView);
        previewView.setPreferredImplementationMode(PreviewView.ImplementationMode.TEXTURE_VIEW);
        userProfileImageView = findViewById(R.id.userProfileImageView);
        cameraModeImageView = findViewById(R.id.cameraModeImageView);
        flashLightImageView = findViewById(R.id.flashLightImageView);
        languageTextView = findViewById(R.id.languageTextView);
        languagesImageView = findViewById(R.id.languagesImageView);
        speechDetectionImageView = findViewById(R.id.speechDetectionImageView);
        objectDetectionImageView = findViewById(R.id.objectDetectionImageView);
        audioImageView = findViewById(R.id.audioImageView);
        speechTextView = findViewById(R.id.speechTextView);
        faceCheckImageView = findViewById(R.id.faceCheckImageView);
        frontCameraOverlayImageView = findViewById(R.id.frontCameraOverlayImageView);
        swapLanguageImageView = findViewById(R.id.swapLanguageImageView);
        swapInputLanguage = findViewById(R.id.inputLanguageTextView);

        graphicOverlay = findViewById(R.id.graphicOverlay);
        progressOverlay = findViewById(R.id.progress_overlay);
        languageSpinner = findViewById(R.id.languageSpinner);
        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        /* Touch imageViews setup */
        setOnTouchListener(cameraModeImageView, R.drawable.camera_mode);
        setOnTouchListener(userProfileImageView, R.drawable.user_profile);
        setOnTouchListener(flashLightImageView, R.drawable.flash_light);
        setOnTouchListener(languagesImageView, R.drawable.languages);
        setOnTouchListener(speechDetectionImageView, R.drawable.speech_detection);
        setOnTouchListener(objectDetectionImageView, R.drawable.objects_detection);
        setOnTouchListener(audioImageView, R.drawable.tts_audio);
        setOnTouchListener(swapLanguageImageView, R.drawable.swap_language);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
        setOnItemForLanguageSpinner();

        frontCameraOverlayImageView.setVisibility(View.INVISIBLE);

        setSwapLanguageTextViews();

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, lensFacing); // Default facing back
            } catch (ExecutionException | InterruptedException e) {
                // Should never be reached
            }
        }, ContextCompat.getMainExecutor(this));

        if (currentMode == Mode.SpeechRecognition) {
            speechDetectionImageView.setImageResource(R.drawable.speech_detection_enabled);
        } else {
            objectDetectionImageView.setImageResource(R.drawable.objects_detection_enabled);
            toggleFastSwapLanguages(View.INVISIBLE);
        }

        guideTour();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setOnTouchListener(View imageView, int drawable) {
        imageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            touchActions(action, drawable, view);
            return true;
        });
    }

    private void setOnItemForLanguageSpinner() {
        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // An item was selected. You can retrieve the selected item using
                if (adapterView != null) {
                    int langId = adapterView.getPositionForView(view);
                    String item = adapterView.getItemAtPosition(i).toString();
                    if (langId != 0) { // Will have to increase all ids by 1 since 0 is default called at beginning
                        setOutputLanguage(langId);
                        // Save the output language in profile
                        Profile profile = new Profile(getInputLanguage(), getOutputLanguage(), lensFacing, currentMode.ordinal());
                        sharedPreferenceHelper.saveProfile(profile);
                        languageTextView.setText(item);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void touchActions(int action, int drawable, View view) {
        if (action == MotionEvent.ACTION_DOWN) {
            Objects.requireNonNull(view.getContext().getDrawable(drawable)).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
            view.invalidate();
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            Objects.requireNonNull(view.getContext().getDrawable(drawable)).clearColorFilter();
            view.invalidate();
            if (drawable == R.drawable.user_profile) {
                goToProfileActivity("no"); // Go to the profile activity with a nice slide animation
            } else if (drawable == R.drawable.camera_mode) {
                cameraAction();
            } else if (drawable == R.drawable.flash_light) {
                flashLightAction();
            } else if (drawable == R.drawable.languages) {
                languageSpinner.performClick();
            } else if (drawable == R.drawable.speech_detection) {
                modeAction(Mode.SpeechRecognition, R.drawable.speech_detection_enabled, R.drawable.objects_detection);
            } else if (drawable == R.drawable.objects_detection) {
                modeAction(Mode.ObjectDetection, R.drawable.speech_detection, R.drawable.objects_detection_enabled);
            } else if (drawable == R.drawable.tts_audio) {
                ttsAction();
            } else if (drawable == R.drawable.swap_language) {
                swapLanguage();
            }
        }
    }

    private void cameraAction() {
        if (lensFacing == CameraSelector.LENS_FACING_BACK) {
            lensFacing = CameraSelector.LENS_FACING_FRONT;
        } else {
            lensFacing = CameraSelector.LENS_FACING_BACK;
        }
        rebindPreview();
        flashLight(false);
        frontCameraOverlayImageView.setVisibility(View.INVISIBLE);
        speechTextView.setVisibility(View.INVISIBLE); // Reset textView
        sharedPreferenceHelper.saveProfile(new Profile(getInputLanguage(), getOutputLanguage(), lensFacing, currentMode.ordinal()));
        audioImageView.setVisibility(View.INVISIBLE);
    }

    private void flashLightAction() {
            flashLightStatus = !flashLightStatus;
            flashLight(flashLightStatus);
            if (lensFacing == CameraSelector.LENS_FACING_FRONT && flashLightStatus) {
                frontCameraOverlayImageView.setVisibility(View.VISIBLE);
            } else {
                frontCameraOverlayImageView.setVisibility(View.INVISIBLE);
            }
    }

    /* Actions to do when switching modes with speech and object detection */
    private void modeAction(Mode mode, int speechDrawable, int objectDetectionDrawable) {
        if (currentMode != mode) {
            currentMode = mode;
            if (mode == Mode.SpeechRecognition) {
                faceDetected = false; // Reset and ready to fire the face check anim
                toggleFastSwapLanguages(View.VISIBLE);
            } else {
                speechTextView.setVisibility(View.INVISIBLE);
                audioImageView.setVisibility(View.INVISIBLE);
                toggleFastSwapLanguages(View.INVISIBLE);
            }
            speechDetectionImageView.setImageResource(speechDrawable);
            objectDetectionImageView.setImageResource(objectDetectionDrawable);
            rebindPreview();
            sharedPreferenceHelper.saveProfile(new Profile(getInputLanguage(), getOutputLanguage(), lensFacing, currentMode.ordinal()));
            if (connectedToInternet()) {
                Toast.makeText(this, "Switched to " + mode.toString() + " Mode!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "You must be connected to internet to use the " + mode.toString() + " Mode!",
                        Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "You are already in this mode!", Toast.LENGTH_LONG).show();
        }
    }

    private void ttsAction() {
        if (mTTS != null) { // Start TTS
            int result = getLocaleResult();
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.d(TAG, "Language not supported");
                String language = Utils.getLanguageByTag(outputLanguage);
                openDialog("Install " + language + " Voice",
                        "You are missing the " + language + " voice! Download and install it by " +
                                "touching the settings button, 'Install' button and choose the voice!",
                                true
                        );
            } else {
                if (!mTTS.isSpeaking()) {
                    startTTS(ttsSentence);
                }
            }
        }
    }

    private int getLocaleResult() {
        Locale locale = Locale.ROOT;
        int data = 0;
        if (mTTS.getAvailableLanguages() != null) {
            for (Locale availableLocale : mTTS.getAvailableLanguages()) {
                String languageAudioWanted = Utils.getLanguageByTag(getOutputLanguage());
                if (languageAudioWanted.equals(availableLocale.getDisplayLanguage())) { // If the locale voice is installed on the phone then set it
                    locale = new Locale(availableLocale.toString());
                }
            }
            data = mTTS.setLanguage(locale);
        }
        return data;
    }

    protected void openDialog(String title, String message, boolean unsupportedLanguage) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    sendToInstallation(unsupportedLanguage);
                })
                .setNegativeButton(android.R.string.no, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void sendToInstallation(boolean unsupportedLanguage) {
        if (unsupportedLanguage) {
            openTtsSettingsToInstallUnsupportedLanguage();
        } else {
            installGoogleTTS();
        }
    }

    private void openTtsSettingsToInstallUnsupportedLanguage() {
        // TODO: Find a wait to go to com.android.settings.TTS_SETTINGS.settings.install.voiceId directly
        String TTS_SETTINGS = "com.android.settings.TTS_SETTINGS";
        Intent intent = new Intent();
        intent.setAction(TTS_SETTINGS);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void installGoogleTTS() {
        String MARKET_GOOGLE_TTS = "market://details?id=com.google.android.tts";
        Intent installIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_GOOGLE_TTS));
        installIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET
                | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(installIntent);
    }

    private void setSwapLanguageTextViews() {
        if (getInputLanguage() > -1 || getOutputLanguage() > -1) {
            swapInputLanguage.setText(Utils.getLanguageByTag(getInputLanguage()));
        }
    }

    private void swapLanguage() {
        int input = getInputLanguage();
        setInputLanguage(getOutputLanguage());
        setOutputLanguage(input);
        setSwapLanguageTextViews(); // Update both swap in and out language
        languageTextView.setText(Utils.getLanguageByTag(getOutputLanguage())); // Update language output

        Profile profile = new Profile(getInputLanguage(), getOutputLanguage(), lensFacing, currentMode.ordinal());
        sharedPreferenceHelper.saveProfile(profile);
    }

    private void toggleFastSwapLanguages(int state) {
        swapInputLanguage.setVisibility(state);
        swapLanguageImageView.setVisibility(state);
    }
}