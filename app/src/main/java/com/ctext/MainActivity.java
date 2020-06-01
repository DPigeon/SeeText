package com.ctext;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ctext.facedetection.FaceDetection;
import com.ctext.objectdetection.ObjectDetection;
import com.ctext.objectdetection.ObjectOverlay;
import com.ctext.profile.Profile;
import com.ctext.profile.SharedPreferenceHelper;
import com.ctext.translator.Translator;
import com.ctext.utils.GraphicOverlay;
import com.ctext.utils.Utils;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
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

/* MainActivity.java
* The MainActivity with CameraX library, Speech Recognition & a Face Detection callback to update the speech text.
* The speech text should move every time the app recognizes a face near the mouth of the speaker.
 */

public class MainActivity extends AppCompatActivity implements RecognitionListener, FaceDetection.Callback, Translator.Callback, ObjectOverlay.Callback {
    private String TAG = "MainActivity:";
    private SharedPreferenceHelper sharedPreferenceHelper;
    private static final int MY_TTS_DATA_CHECK_CODE = 90;
    private static final int MY_PERMISSIONS = 100; // Request code response for camera & microphone
    private int inputLanguage = FirebaseTranslateLanguage.EN, outputLanguage = FirebaseTranslateLanguage.EN; // Default is english
    private ImageView userProfileImageView, cameraModeImageView, languagesImageView, speechDetectionImageView, objectDetectionImageView, faceCheckImageView, audioImageView;
    private Spinner languageSpinner;
    private TextView languageTextView;
    private boolean faceProcessing = false; // For throttling the calls
    private long animationDuration = 1000; // milliseconds
    private boolean faceDetected = false; // For face check imageView anim to run once
    private boolean outOfMainActivity = false; // Flag to stop speech recognition on other activities
    private FrameLayout progressOverlay; // Loading overlay wheel

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
    private static SpeechRecognizer mRecognizer = null;
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
        Objects.requireNonNull(getSupportActionBar()).hide(); // Hide the main app bar on top

        /* Running UI Thread to get audio & video permission */
        this.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showPermissions();
            }
        });

        loadProfile();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            setupUI();

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
                    Intent intent = new Intent(MainActivity.this, DefinitionActivity.class);
                    intent.putExtra("word", word);
                    intent.putExtra("inputLanguage", getInputLanguage());
                    intent.putExtra("outputLanguage", getOutputLanguage());
                    outOfMainActivity = true;
                    startActivity(intent);
                } else {
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
        //if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
        if (!outOfMainActivity)
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
                //audioImageView.setVisibility(View.VISIBLE);
            try {
                if (inputLanguage != outputLanguage) { // Checks if input and output are the same
                        translator = new Translator(getApplicationContext(), getInputLanguage(), getOutputLanguage(), this);
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

    @SuppressLint({"ClickableViewAccessibility", "WrongViewCast"})
    protected void setupUI() {
        previewView = findViewById(R.id.previewView);
        previewView.setPreferredImplementationMode(PreviewView.ImplementationMode.TEXTURE_VIEW); // TextureView
        graphicOverlay = findViewById(R.id.graphicOverlay);

        userProfileImageView = findViewById(R.id.userProfileImageView);
        userProfileImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                Objects.requireNonNull(view.getContext().getDrawable(R.drawable.user_profile)).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                view.invalidate();
            }
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                Objects.requireNonNull(view.getContext().getDrawable(R.drawable.user_profile)).clearColorFilter();
                view.invalidate();
                // Go to the profile activity with a nice slide animation
                goToProfileActivity("no");
            }

            return true;
        });

        cameraModeImageView = findViewById(R.id.cameraModeImageView);
        cameraModeImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                Objects.requireNonNull(view.getContext().getDrawable(R.drawable.camera_mode)).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                view.invalidate();
            }
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                Objects.requireNonNull(view.getContext().getDrawable(R.drawable.camera_mode)).clearColorFilter();
                view.invalidate();
                if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                } else {
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                }
                rebindPreview();
                speechTextView.setVisibility(View.INVISIBLE); // Reset textView
                sharedPreferenceHelper.saveProfile(new Profile(getInputLanguage(), getOutputLanguage(), lensFacing, currentMode.ordinal()));
                //audioImageView.setVisibility(View.INVISIBLE);
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
                    Profile profile = new Profile(getInputLanguage(), getOutputLanguage(), lensFacing, currentMode.ordinal());
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
                Objects.requireNonNull(view.getContext().getDrawable(R.drawable.languages)).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                view.invalidate();
            }
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                Objects.requireNonNull(view.getContext().getDrawable(R.drawable.languages)).clearColorFilter();
                view.invalidate();
                languageSpinner.performClick();
            }

            return true;
        });

        objectDetectionImageView = findViewById(R.id.objectDetectionImageView);
        objectDetectionImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                Objects.requireNonNull(view.getContext().getDrawable(R.drawable.objects_detection)).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                view.invalidate();
            }
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                Objects.requireNonNull(view.getContext().getDrawable(R.drawable.objects_detection)).clearColorFilter();
                view.invalidate();
                if (currentMode != Mode.ObjectDetection) {
                        currentMode = Mode.ObjectDetection;
                        speechDetectionImageView.setImageResource(R.drawable.speech_detection);
                        objectDetectionImageView.setImageResource(R.drawable.objects_detection_enabled);
                        previewImageView.setImageDrawable(null);
                        rebindPreview();
                        previewImageView.setVisibility(View.VISIBLE);
                        speechTextView.setVisibility(View.INVISIBLE);
                        //audioImageView.setVisibility(View.INVISIBLE);
                        sharedPreferenceHelper.saveProfile(new Profile(getInputLanguage(), getOutputLanguage(), lensFacing, currentMode.ordinal()));
                        if (connectedToInternet())
                            Toast.makeText(this, "Switched to Object Detector Mode!", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(this, "You must be connected to internet to use the Object Detector Mode!", Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(this, "You are already in this mode!", Toast.LENGTH_LONG).show();
            }

            return true;
        });

        speechDetectionImageView = findViewById(R.id.speechDetectionImageView);
        speechDetectionImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                Objects.requireNonNull(view.getContext().getDrawable(R.drawable.speech_detection)).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                view.invalidate();
            }
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                Objects.requireNonNull(view.getContext().getDrawable(R.drawable.speech_detection)).clearColorFilter();
                view.invalidate();
                if (currentMode != Mode.SpeechRecognition) {
                        currentMode = Mode.SpeechRecognition;
                        faceDetected = false; // Reseted and ready to fire the face check anim
                        speechDetectionImageView.setImageResource(R.drawable.speech_detection_enabled);
                        objectDetectionImageView.setImageResource(R.drawable.objects_detection);
                        previewImageView.setVisibility(View.INVISIBLE);
                        rebindPreview();
                        //speechTextView.setVisibility(View.VISIBLE);
                        sharedPreferenceHelper.saveProfile(new Profile(getInputLanguage(), getOutputLanguage(), lensFacing, currentMode.ordinal()));
                        if (connectedToInternet())
                            Toast.makeText(this, "Switched to Speech Translator Mode!", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(this, "You must be connected to internet to use the Speech Detection Mode!", Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(this, "You are already in this mode!", Toast.LENGTH_LONG).show();
            }

            return true;
        });

        audioImageView = findViewById(R.id.audioImageView);
        audioImageView.setOnTouchListener((view, motionEvent) -> {
            int action = motionEvent.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                Objects.requireNonNull(view.getContext().getDrawable(R.drawable.tts_audio)).setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP);
                view.invalidate();
            }
            else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                Objects.requireNonNull(view.getContext().getDrawable(R.drawable.tts_audio)).clearColorFilter();
                view.invalidate();
                // Start TTS
                if (mTTS != null) {
                    if (!mTTS.isSpeaking())
                        startTTS(ttsSentence);
                }
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
        progressOverlay = findViewById(R.id.progress_overlay);
        if (currentMode == Mode.SpeechRecognition)
            speechDetectionImageView.setImageResource(R.drawable.speech_detection_enabled);
        else
            objectDetectionImageView.setImageResource(R.drawable.objects_detection_enabled);
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

    protected void loadLanguageFirstTime() {
        int outputLang = sharedPreferenceHelper.getLanguageOutput();
        if (outputLang != -1) { // First time using the app
            String language = FirebaseTranslateLanguage.languageCodeForLanguage(outputLang);
            String stringLang = Locale.forLanguageTag(language).getDisplayName();
            if (languageTextView != null)
                languageTextView.setText(stringLang);
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    protected void bindPreview(@NonNull ProcessCameraProvider cameraProvider, int lensFacing) {
        preview = new Preview.Builder().build();
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
        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));
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
        MainActivity thisActivity = this;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) // Videos
            ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS);
    }

    protected void initializeRecognition() {
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        mRecognizer.setRecognitionListener(this);
    }

    /* Starts the speech */
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
            goToProfileActivity("yes");
            Toast.makeText(getApplicationContext(), "Set your language!", Toast.LENGTH_LONG).show();
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

    /* Makes the app always listen to inputs */
    protected void persistentSpeech() {
        if (mRecognizer != null) {
            stopListeningSpeech();
            initializeRecognition();
            startRecognition();
        }
    }

    protected void checkTtsResources() {
        Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_TTS_DATA_CHECK_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MY_TTS_DATA_CHECK_CODE) {
            ArrayList<String> availableLanguages = data.getStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES);
            assert availableLanguages != null;
            if (availableLanguages.isEmpty()) {
                Intent installIntent = new Intent();
                installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            } else
                initializeTTS();
        }
    }

    protected void initializeTTS() {
        mTTS = new TextToSpeech(getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                // We get the output language translated
                String language = FirebaseTranslateLanguage.languageCodeForLanguage(getOutputLanguage());
                Locale locale = new Locale(language);
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

    void goToProfileActivity(String firstTime) { // Function that goes from the main activity to profile one
        progressOverlay.setVisibility(View.VISIBLE);
        Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
        intent.putExtra("lensFacing", lensFacing);
        intent.putExtra("mode", currentMode.ordinal());
        intent.putExtra("firstTime", firstTime);
        outOfMainActivity = true;
        stopListeningSpeech();
        startActivity(intent);
    }

    /* Checks if we have a wifi or LTE connection */
    private boolean connectedToInternet() {
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

}
