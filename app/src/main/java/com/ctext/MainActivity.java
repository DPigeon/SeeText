package com.ctext;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ctext.facedetection.FaceDetection;
import com.ctext.objectdetection.ObjectDetection;
import com.ctext.profile.Profile;
import com.ctext.profile.SharedPreferenceHelper;
import com.ctext.translator.Translator;
import com.ctext.utils.GraphicOverlay;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import ai.fritz.core.Fritz;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Lifecycle;

/* MainActivity.java
* The MainActivity with CameraX library, Speech Recognition & a Face Detection callback to update the speech text.
* The speech text should move every time the app recognizes a face near the mouth of the speaker.
 */

public class MainActivity extends AppCompatActivity implements RecognitionListener, FaceDetection.Callback, Translator.Callback {
    private String TAG = "MainActivity:";
    private SharedPreferenceHelper sharedPreferenceHelper;
    private static final int MY_PERMISSIONS = 100; // Request code response for camera & microphone
    private int inputLanguage, outputLanguage = FirebaseTranslateLanguage.EN; // Default is english
    private ImageView userProfileImageView, cameraModeImageView, languagesImageView, speechDetectionImageView, objectDetectionImageView, faceCheckImageView, audioImageView;
    private Spinner languageSpinner;
    private TextView languageTextView;
    private boolean faceProcessing = false; // For throttling the calls
    private long animationDuration = 1000; // milliseconds
    private boolean faceDetected = false; // For face check imageView anim to run once

    /* Video Variables */
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private Camera camera;
    private Preview preview;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private ImageView previewImageView; // Used for object detection
    private GraphicOverlay graphicOverlay;

    /* Audio Variables */
    private Translator translator;
    private SpeechRecognizer mRecognizer;
    private TextView speechTextView;
    private AudioManager mAudioManager;
    private TextToSpeech mTTS;
    private String ttsSentence; // The last translated sentence to send to TTS

    /* Modes of the app */
    private enum Mode {
        SpeechRecognition,
        ObjectDetection
    }
    private Mode currentMode = Mode.SpeechRecognition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fritz.configure(this); // Initialize Fritz
        setContentView(R.layout.activity_main);
        getSupportActionBar().hide(); // Hide the main app bar on top

        /* Running UI Thread to get audio & video permission */
        this.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showPermissions();
            }
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            setupUI();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            initializeRecognition();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkProfile();
        loadLanguageFirstTime(); // Check when first time opening the app
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListeningSpeech(); // Stopping the recognizer when changing activity
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            if (requestCode == MY_PERMISSIONS) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setupUI();
                    initializeRecognition();
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
            if (mRecognizer != null) {
                initializeRecognition();
            } else {
                initializeRecognition();
                if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED))
                    listenForSpeech();
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
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
            persistentSpeech();
    }

    @Override
    public void onResults(Bundle results) {
        String sentence = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
        /* Sentences may be null sometimes so we avoid that */
        String sentenceToFitUI = " " + sentence + " ";
        if (currentMode != Mode.ObjectDetection) { // Current mode must be speech detection
            if (speechTextView.getVisibility() == View.INVISIBLE)
                speechTextView.setVisibility(View.VISIBLE);
                audioImageView.setVisibility(View.VISIBLE);
            try {
                if (inputLanguage != outputLanguage) { // Checks if input and output are the same
                    translator = new Translator(getApplicationContext(), getInputLanguage(), getOutputLanguage(), this);
                    translator.downloadModelAndTranslate(outputLanguage, sentence);
                } else
                    speechTextView.setText(sentenceToFitUI); // We show the text like it is
            } catch (Exception exception) {}

            /* Text Animation */
            speechTextView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            Animation fadeOutAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            fadeOutAnim.setStartTime(5000);
            speechTextView.startAnimation(fadeOutAnim);

            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
                persistentSpeech();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {}

    @Override
    public void onEvent(int eventType, Bundle params) {}

    @SuppressLint("ClickableViewAccessibility")
    protected void setupUI() {
        previewView = findViewById(R.id.previewView);
        previewView.setPreferredImplementationMode(PreviewView.ImplementationMode.TEXTURE_VIEW); // TextureView
        graphicOverlay = findViewById(R.id.graphicOverlay);

        userProfileImageView = findViewById(R.id.userProfileImageView);
        userProfileImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                view.getContext().getDrawable(R.drawable.user_profile).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                view.invalidate();
            }
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.getContext().getDrawable(R.drawable.user_profile).clearColorFilter();
                view.invalidate();
                // Go to the profile activity with a nice slide animation
                goToActivity(ProfileActivity.class);
            }

            return true;
        });

        cameraModeImageView = findViewById(R.id.cameraModeImageView);
        cameraModeImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                view.getContext().getDrawable(R.drawable.camera_mode).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                view.invalidate();
            }
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.getContext().getDrawable(R.drawable.camera_mode).clearColorFilter();
                view.invalidate();
                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                }
                rebindPreview();
                speechTextView.setVisibility(View.INVISIBLE); // Reset textView
                audioImageView.setVisibility(View.INVISIBLE);
            }

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
                    Profile profile = new Profile(getInputLanguage(), getOutputLanguage());
                    sharedPreferenceHelper.saveProfile(profile);
                    languageTextView.setText(item);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        languageTextView = findViewById(R.id.languageTextView);
        languagesImageView = findViewById(R.id.languagesImageView);
        languagesImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                view.getContext().getDrawable(R.drawable.languages).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                view.invalidate();
            }
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.getContext().getDrawable(R.drawable.languages).clearColorFilter();
                view.invalidate();
                languageSpinner.performClick();
            }

            return true;
        });

        objectDetectionImageView = findViewById(R.id.objectDetectionImageView);
        objectDetectionImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                view.getContext().getDrawable(R.drawable.objects_detection).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                view.invalidate();
            }
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.getContext().getDrawable(R.drawable.objects_detection).clearColorFilter();
                view.invalidate();
                if (currentMode != Mode.ObjectDetection) {
                    currentMode = Mode.ObjectDetection;
                    speechDetectionImageView.setImageResource(R.drawable.speech_detection);
                    objectDetectionImageView.setImageResource(R.drawable.objects_detection_enabled);
                    previewImageView.setImageDrawable(null);
                    rebindPreview();
                    previewImageView.setVisibility(View.VISIBLE);
                    speechTextView.setVisibility(View.INVISIBLE);
                    audioImageView.setVisibility(View.INVISIBLE);
                    Toast.makeText(this, "Switched to Object Detector Mode!", Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(this, "You are already in this mode!", Toast.LENGTH_LONG).show();
            }

            return true;
        });

        speechDetectionImageView = findViewById(R.id.speechDetectionImageView);
        speechDetectionImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                view.getContext().getDrawable(R.drawable.speech_detection).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                view.invalidate();
            }
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.getContext().getDrawable(R.drawable.speech_detection).clearColorFilter();
                view.invalidate();
                if (currentMode != Mode.SpeechRecognition) {
                    currentMode = Mode.SpeechRecognition;
                    speechDetectionImageView.setImageResource(R.drawable.speech_detection_enabled);
                    objectDetectionImageView.setImageResource(R.drawable.objects_detection);
                    previewImageView.setVisibility(View.INVISIBLE);
                    rebindPreview();
                    //speechTextView.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Switched to Speech Translator Mode!", Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(this, "You are already in this mode!", Toast.LENGTH_LONG).show();
            }

            return true;
        });

        audioImageView = findViewById(R.id.audioImageView);
        audioImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                view.getContext().getDrawable(R.drawable.tts_audio).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                view.invalidate();
            }
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                view.getContext().getDrawable(R.drawable.tts_audio).clearColorFilter();
                view.invalidate();
                // Start TTS
                if (!mTTS.isSpeaking())
                    startTTS(ttsSentence);
            }

            return true;
        });

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, lensFacing); // Default facing back
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        speechTextView = findViewById(R.id.speechTextView);
        previewImageView = findViewById(R.id.previewImageView);
        faceCheckImageView = findViewById(R.id.faceCheckImageView);
        speechDetectionImageView.setImageResource(R.drawable.speech_detection_enabled);
    }

    protected void faceCheckAnimation() {
        if (!faceDetected && currentMode != Mode.ObjectDetection) {
            faceDetected = true; // Run once when mode chosen
            faceCheckImageView.setVisibility(View.VISIBLE);
            ObjectAnimator animatorY = ObjectAnimator.ofFloat(faceCheckImageView, "y", getScreenHeight() - 150, getScreenHeight() - 250);
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

    protected void loadLanguageFirstTime() {
        int outputLang = sharedPreferenceHelper.getLanguageOutput();
        if (outputLang != -1) { // First time using the app
            String language = FirebaseTranslateLanguage.languageCodeForLanguage(outputLang);
            String stringLang = Locale.forLanguageTag(language).getDisplayName();
            languageTextView.setText(stringLang);
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    protected void bindPreview(@NonNull ProcessCameraProvider cameraProvider, int lensFacing) {
        preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        Size size = new Size(480, 360); // For better latency
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        /* Image Processing */
        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, image -> { // https://developer.android.com/training/camerax/analyze
            if (image.getImage() == null) {
                return;
            }
            if (currentMode == Mode.SpeechRecognition) {
                // Currently only looks at the first image
                graphicOverlay.clear(); // Always destroy the object graphic overlays
                FaceDetection faceDetection = new FaceDetection(graphicOverlay,this);
                if (!faceProcessing) { // Throttle the calls
                    faceProcessing = true;
                    faceDetection.analyzeImage(image);
                }
            } else if (currentMode == Mode.ObjectDetection) {
                ObjectDetection objectDetection = new ObjectDetection(graphicOverlay);
                // We get the textureView to get the bitmap image every time for better orientation
                View surfaceOrTexture = previewView.getChildAt(0);
                if (surfaceOrTexture instanceof TextureView) {
                    Bitmap bitmap = ((TextureView) surfaceOrTexture).getBitmap();
                    assert bitmap != null;
                    Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, getScreenWidth(), getScreenHeight(), false);
                    objectDetection.detectObjects(getApplicationContext(), newBitmap, lensFacing, getOutputLanguage());
                }
            }
            image.close();
        });
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));
    }

    /* Rebinds the preview to keep the lenses working in real time */
    protected void rebindPreview() {
        try {
            cameraProviderFuture.get().unbindAll(); // Unbind all other cameras
            cameraProviderFuture = ProcessCameraProvider.getInstance(this);
            faceDetected = false; // Reseted and ready to fire the face check anim
            bindPreview(cameraProviderFuture.get(), lensFacing); // Change lens facing
        } catch (Exception exception) {}
    }

    /* Used to grant permission from the UI thread */
    protected void showPermissions() {
        MainActivity thisActivity = this;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) // Videos
            ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS);
    }

    protected void initializeRecognition() {
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        mRecognizer.setRecognitionListener(this);
        initializeTTS();
    }

    /* Starts the speech */
    protected void listenForSpeech() {
        // Uses our SharedPreferences to perform recognition in different languages
        String lang = FirebaseTranslateLanguage.languageCodeForLanguage(getInputLanguage());
        Log.d(TAG, lang);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getApplicationContext().getPackageName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{lang});

        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true); // Mutes any sound of beep for listening
        mRecognizer.startListening(intent);
    }

    protected void stopListeningSpeech() {
        if (mRecognizer != null) {
            mRecognizer.stopListening();
            mRecognizer.cancel();
            mRecognizer.destroy();
            mRecognizer = null;
        }
        if (mTTS != null) {
            stopTTS();
        }
    }

    /* Makes the app always listen to inputs */
    protected void persistentSpeech() {
        if (mRecognizer != null) {
            mRecognizer.destroy();
            mRecognizer = null;
            initializeRecognition();
            listenForSpeech();
        }
    }

    protected void initializeTTS() {
        mTTS = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                // We get the output language translated
                String language = FirebaseTranslateLanguage.languageCodeForLanguage(getOutputLanguage());
                Locale locale = new Locale(language);
                mTTS.setLanguage(Locale.UK);
            }
        });
    }

    protected void startTTS(String sentence) {
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false); // Unmute sound
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        mTTS.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID);
    }

    protected void stopTTS() {
        mTTS.stop();
        mTTS.shutdown();
        mTTS = null;
    }

    /* Checks if profile is filled in before using the app */
    protected void checkProfile() {
        sharedPreferenceHelper = new SharedPreferenceHelper(this);
        Profile profile = sharedPreferenceHelper.getProfile();
        int lang = profile.getLanguage();
        int outputLang = profile.getLanguageOutputId();
        if (lang != -1) {
            setInputLanguage(lang);
            if (outputLang != -1)
                setOutputLanguage(outputLang);
        } else {
            goToActivity(ProfileActivity.class);
            Toast.makeText(getApplicationContext(), "Create your profile!", Toast.LENGTH_LONG).show();
        }
    }

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

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    void goToActivity(Class activity) { // Function that goes from the main activity to another one
        Intent intent = new Intent(MainActivity.this, activity);
        startActivity(intent);
    }

}
