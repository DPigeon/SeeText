package com.seetext.activities.main;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.seetext.Mode;
import com.seetext.R;
import com.seetext.profile.Profile;
import com.seetext.utils.Utils;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

/* UIMainActivity.java
 * The abstract base class for MainActivity containing all the UI elements
 */

public abstract class AbstractUIMainActivity extends AbstractMainActivity {

    protected abstract void bindPreview(ProcessCameraProvider cameraProvider, int lensFacing);
    protected abstract void rebindPreview();
    protected abstract void flashLight(boolean flashLightStatus);
    protected abstract void startTTS(String ttsSentence);

    protected String TAG = "AbstractUIMainActivity:";

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
        swapOutputLanguage = findViewById(R.id.outputLanguageTextView);

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
                int langId = adapterView.getPositionForView(view);
                String item = adapterView.getItemAtPosition(i).toString();
                if (langId != 0) { // Will have to increase all ids by 1 since 0 is default called at beginning
                    setOutputLanguage(langId);
                    // Save the output language in profile
                    Profile profile = new Profile(getInputLanguage(), getOutputLanguage(), lensFacing, currentMode.ordinal());
                    sharedPreferenceHelper.saveProfile(profile);
                    languageTextView.setText(item);
                    swapOutputLanguage.setText(item);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }

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
            if (!mTTS.isSpeaking()) {
                startTTS(ttsSentence);
            }
        }
    }

    private void setSwapLanguageTextViews() {
        if (getInputLanguage() > -1 || getOutputLanguage() > -1) {
            swapInputLanguage.setText(Utils.getLanguageByTag(getInputLanguage()));
            swapOutputLanguage.setText(Utils.getLanguageByTag(getOutputLanguage()));
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
        swapOutputLanguage.setVisibility(state);
    }
}