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

import com.seetext.R;
import com.seetext.profile.Profile;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

public abstract class UIMainActivity extends BaseMainActivity {

    protected abstract void bindPreview(ProcessCameraProvider cameraProvider, int lensFacing);
    protected abstract void rebindPreview();
    protected abstract void setOutputLanguage(int languageId);
    protected abstract void flashLight(boolean flashLightStatus);
    protected abstract void startTTS(String ttsSentence);

    @Override
    @SuppressLint({"ClickableViewAccessibility"})
    public void setupUI() {
        previewView = findViewById(R.id.previewView);
        previewView.setPreferredImplementationMode(PreviewView.ImplementationMode.TEXTURE_VIEW); // TextureView
        graphicOverlay = findViewById(R.id.graphicOverlay);

        /* Touch button setup */
        userProfileImageView = findViewById(R.id.userProfileImageView);
        userProfileImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            touchActions(action, R.drawable.user_profile, view);
            return true;
        });

        cameraModeImageView = findViewById(R.id.cameraModeImageView);
        cameraModeImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            touchActions(action, R.drawable.camera_mode, view);
            return true;
        });

        flashLightImageView = findViewById(R.id.flashLightImageView);
        flashLightImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            touchActions(action, R.drawable.flash_light, view);
            return true;
        });

        languageTextView = findViewById(R.id.languageTextView);
        languagesImageView = findViewById(R.id.languagesImageView);
        languagesImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            touchActions(action, R.drawable.languages, view);
            return true;
        });

        objectDetectionImageView = findViewById(R.id.objectDetectionImageView);
        objectDetectionImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            touchActions(action, R.drawable.objects_detection, view);
            return true;
        });

        speechDetectionImageView = findViewById(R.id.speechDetectionImageView);
        speechDetectionImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            touchActions(action, R.drawable.speech_detection, view);
            return true;
        });

        audioImageView = findViewById(R.id.audioImageView);
        audioImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            touchActions(action, R.drawable.tts_audio, view);
            return true;
        });

        languageSpinner = findViewById(R.id.languageSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);
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
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, lensFacing); // Default facing back
            } catch (ExecutionException | InterruptedException e) {
                // Should never be reached
            }
        }, ContextCompat.getMainExecutor(this));

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        speechTextView = findViewById(R.id.speechTextView);
        faceCheckImageView = findViewById(R.id.faceCheckImageView);
        progressOverlay = findViewById(R.id.progress_overlay);
        if (currentMode == Mode.SpeechRecognition) {
            speechDetectionImageView.setImageResource(R.drawable.speech_detection_enabled);
        } else {
            objectDetectionImageView.setImageResource(R.drawable.objects_detection_enabled);
        }
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
        speechTextView.setVisibility(View.INVISIBLE); // Reset textView
        sharedPreferenceHelper.saveProfile(new Profile(getInputLanguage(), getOutputLanguage(), lensFacing, currentMode.ordinal()));
        audioImageView.setVisibility(View.INVISIBLE);
    }

    private void flashLightAction() {
        if (camera.getCameraInfo().hasFlashUnit()) {
            flashLightStatus = !flashLightStatus;
            flashLight(flashLightStatus);
        } else {
            Toast.makeText(getApplicationContext(), "No flash available on your device!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /* Actions to do when switching modes with speech and object detection */
    private void modeAction(Mode mode, int speechDrawable, int objectDetectionDrawable) {
        if (currentMode != mode) {
            currentMode = mode;
            if (mode == Mode.SpeechRecognition) {
                faceDetected = false; // Reset and ready to fire the face check anim
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
            Log.d(TAG, ttsSentence);
        }
    }
}