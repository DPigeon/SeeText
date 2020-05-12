package com.ctext;

import android.Manifest;
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

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

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

public class MainActivity extends AppCompatActivity implements RecognitionListener, FaceDetection.Callback, Translator.Callback {
    private String TAG = "MainActivity:";
    SharedPreferenceHelper sharedPreferenceHelper;
    private static final int MY_PERMISSIONS = 100; // Request code response for camera & microphone
    private int inputLanguage = FirebaseTranslateLanguage.EN, outputLanguage = FirebaseTranslateLanguage.EN; // Default is english
    Spinner languageSpinner;
    TextView languageTextView;

    /* Video Variables */
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private Camera camera;
    private Preview preview;
    private ImageView userProfileImageView, cameraModeImageView, languagesImageView, speechDetectionImageView, objectDetectionImageView;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private ImageView previewImageView; // Used for object detection
    private GraphicOverlay graphicOverlay;

    /* Audio Variables */
    LanguageIdentification languageIdentification;
    Translator translator;
    private SpeechRecognizer mRecognizer;
    private TextView speechTextView;
    private AudioManager mAudioManager;

    /* Modes of the app */
    enum Mode {
        SpeechRecognition,
        ObjectDetection
    }
    Mode currentMode = Mode.SpeechRecognition;

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

        checkProfile();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            setupUI();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
            initializeRecognition();

        languageIdentification = new LanguageIdentification();
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
    public void update(float x, float y, boolean hasFace)  {
        // This function knows that it has detected a face
        if (!hasFace) { // Closing speech recognition
            stopListeningSpeech();
            speechTextView.setText(""); // Reset text
        } else { // Opening speech recognition with speech text
            if (mRecognizer != null) {
                initializeRecognition();
            } else {
                initializeRecognition();
                listenForSpeech();
            }
            speechTextView.setX(x);
            speechTextView.setY(y);
        }
    }

    @Override
    public void translateTheText(String text) {
        String sentenceToFitUI = " " + text + " ";
        speechTextView.setText(sentenceToFitUI);
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
            try {
                if (inputLanguage != outputLanguage) { // Checks if input and output are the same
                    translator = new Translator(getApplicationContext(), getInputLanguage(), getOutputLanguage(), this);
                    translator.downloadModelAndTranslate(outputLanguage, sentence);
                } else
                    speechTextView.setText(sentenceToFitUI); // We show the text like it is
                //languageIdentification.identification(sentence);
            } catch (Exception exception) {}

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
                setOutputLanguage(langId);
                languageTextView.setText(item);
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
                    previewImageView.setImageDrawable(null);
                    rebindPreview();
                    previewImageView.setVisibility(View.VISIBLE);
                    speechTextView.setVisibility(View.INVISIBLE);
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
                    previewImageView.setVisibility(View.INVISIBLE);
                    rebindPreview();
                    speechTextView.setVisibility(View.VISIBLE);
                    Toast.makeText(this, "Switched to Speech Translator Mode!", Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(this, "You are already in this mode!", Toast.LENGTH_LONG).show();
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
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    protected void bindPreview(@NonNull ProcessCameraProvider cameraProvider, int lensFacing) {
        preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        Size size = new Size(480, 360); // For better latency
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        /* Image Processing Face Detection */
        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, image -> { // https://developer.android.com/training/camerax/analyze
            if (image.getImage() == null) {
                return;
            }
            if (currentMode == Mode.SpeechRecognition) {
                // Currently only looks at the first image
                FaceDetection faceDetection = new FaceDetection(this);
                faceDetection.analyzeImage(image);

                //image.close(); // Closes the images to have multi-frames analysis for real time preview (CAUSES MEMORY LEAK WILL HAVE TO FIX)
            } else if (currentMode == Mode.ObjectDetection) {
                ObjectDetection objectDetection = new ObjectDetection(graphicOverlay);
                // We get the textureView to get the bitmap image every time for better orientation
                View surfaceOrTexture = previewView.getChildAt(0);
                if (surfaceOrTexture instanceof TextureView) {
                    Bitmap bitmap = ((TextureView) surfaceOrTexture).getBitmap();
                    Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, getScreenWidth(), getScreenHeight(), false);
                    objectDetection.detectObjects(getApplicationContext(), newBitmap, lensFacing, getOutputLanguage());
                }
                image.close();
            }
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
    }

    /* Starts the speech */
    public void listenForSpeech() {
        // Uses our SharedPreferences to perform recognition in different languages
        String lang = FirebaseTranslateLanguage.languageCodeForLanguage(getInputLanguage());
        Log.d(TAG, lang);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
        intent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{lang});

        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true); // Mutes any sound of beep for listening
        mRecognizer.startListening(intent);
    }

    protected void stopListeningSpeech() {
        if (mRecognizer != null) {
            mRecognizer.destroy();
            mRecognizer = null;
        }
    }

    /* Makes the app always listen to inputs */
    protected void persistentSpeech() {
        mRecognizer.destroy();
        mRecognizer = null;
        initializeRecognition();
        listenForSpeech();
    }

    /* Checks if profile is filled in before using the app */
    protected void checkProfile() {
        sharedPreferenceHelper = new SharedPreferenceHelper(this);
        Profile profile = sharedPreferenceHelper.getProfile();
        int lang = profile.getLanguage();
        if (lang != -1) {
            setInputLanguage(lang);
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
