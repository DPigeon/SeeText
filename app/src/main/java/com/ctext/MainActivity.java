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
import android.view.OrientationEventListener;
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

public class MainActivity extends AppCompatActivity implements RecognitionListener, FaceDetection.Callback, FaceDetection.DetectingCallback, Translator.Callback, ObjectDetection.Callback {
    private String TAG = "MainActivity:";
    private static final int MY_PERMISSIONS = 100; // Request code response for camera & microphone
    private int inputLanguage = FirebaseTranslateLanguage.EN; // For now SpeechRecognizer library only initialized with english
    private int outputLanguage = FirebaseTranslateLanguage.EN; // Default is english
    volatile boolean detecting = false;
    Spinner languageSpinner;
    TextView languageTextView;

    /* Video Variables */
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private Camera camera;
    private Preview preview;
    private ImageView cameraModeImageView;
    private ImageView languagesImageView;
    private ImageView speechDetectionImageView;
    private ImageView objectDetectionImageView;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private ImageView previewImageView; // Used for object detection
    private OrientationEventListener mOrientationListener;

    /* Audio Variables */
    LanguageIdentification languageIdentification;
    Translator translator;
    private SpeechRecognizer mRecognizer;
    private TextView speechTextView;
    private AudioManager mAudioManager;

    /* Modes of the app */
    enum Mode {
        SPEECHDETECTION,
        OBJECTDETECTION
    }
    Mode currentMode = Mode.SPEECHDETECTION;

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
                    Toast.makeText(this, "You cannot run the app without allowing camera or microphone!", Toast.LENGTH_LONG).show();
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
    public void draw(Bitmap image) {
        this.runOnUiThread(() -> {
            previewImageView.setImageBitmap(image); // Update the imageView that's gonna replace the previewView
        });
    }

    @Override
    public void detect(boolean bool) {
        detecting = bool;
    }

    @Override
    public void translateTheText(String text) {
        String sentenceToFitUI = " " + text + " ";
        speechTextView.setText(sentenceToFitUI);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsDb) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int error) {
        /*if (error == SpeechRecognizer.ERROR_AUDIO) {
            showToastMessage("Audio recording error");
        } else if (error == SpeechRecognizer.ERROR_CLIENT) {
            showToastMessage("Client side error");
        } else if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            showToastMessage("Insufficient permissions");
        } else if (error == SpeechRecognizer.ERROR_NETWORK) {
            showToastMessage("Network error");
        } else if (error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT) {
            showToastMessage("Network timeout");
        } else if (error == SpeechRecognizer.ERROR_NO_MATCH) {
            showToastMessage("No speech match");
        } else if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
            showToastMessage("Recognizer busy");
        } else if (error == SpeechRecognizer.ERROR_SERVER) {
            showToastMessage("Server error");
        } else if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            showToastMessage("No speech input");
        } else {
            showToastMessage("Unknown error");
        }*/
        persistentSpeech();
    }

    @Override
    public void onResults(Bundle results) {
        String sentence = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).get(0);
        /* Sentences may be null sometimes so we avoid that */
        String sentenceToFitUI = " " + sentence + " ";
        if (currentMode != Mode.OBJECTDETECTION) { // Current mode must be speech detection
            if (speechTextView.getVisibility() == View.INVISIBLE)
                speechTextView.setVisibility(View.VISIBLE);
            try {
                if (inputLanguage != outputLanguage) { // Checks if input and output are the same
                    translator = new Translator(getApplicationContext(), inputLanguage, getOutputLanguage(), this::translateTheText);
                    translator.downloadModelAndTranslate(outputLanguage, sentence);
                } else
                    speechTextView.setText(sentenceToFitUI); // We show the text like it is
                //languageIdentification.identification(sentence);
            } catch (Exception exception) {

            }
            speechTextView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            Animation fadeOutAnim = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
            fadeOutAnim.setStartTime(5000);
            speechTextView.startAnimation(fadeOutAnim);

            persistentSpeech();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    @SuppressLint("ClickableViewAccessibility")
    protected void setupUI() {
        previewView = findViewById(R.id.previewView);
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
                try {
                    cameraProviderFuture.get().unbindAll(); // Unbind all other cameras
                    cameraProviderFuture = ProcessCameraProvider.getInstance(this);
                    bindPreview(cameraProviderFuture.get(), lensFacing); // Change lens facing
                } catch (Exception exception) {

                }
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
                String item = adapterView.getItemAtPosition(i).toString();
                if (item.equals("English")) {
                    setOutputLanguage(FirebaseTranslateLanguage.EN);
                }
                else if (item.equals("French")) {
                    setOutputLanguage(FirebaseTranslateLanguage.FR);
                }
                else if (item.equals("Spanish")) {
                    setOutputLanguage(FirebaseTranslateLanguage.ES);
                }
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
                if (currentMode != Mode.OBJECTDETECTION) {
                    currentMode = Mode.OBJECTDETECTION;
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
                if (currentMode != Mode.SPEECHDETECTION) {
                    currentMode = Mode.SPEECHDETECTION;
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
                .setTargetResolution(size)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        /* Image Processing Face Detection */
        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, image -> { // https://developer.android.com/training/camerax/analyze
            if (image == null || image.getImage() == null) {
                return;
            }
            Log.d(TAG, image.getImage().getWidth() + " " + image.getImage().getHeight());
            if (currentMode == Mode.SPEECHDETECTION) {
                FaceDetection faceDetection = new FaceDetection(this::update, this::detect);
                faceDetection.analyzeImage(image);

                //image.close(); // Closes the images to have multi-frames analysis for real time preview (CAUSES MEMORY LEAK WILL HAVE TO FIX)
            } else if (currentMode == Mode.OBJECTDETECTION) {
                ObjectDetection objectDetection = new ObjectDetection(this::draw);
                objectDetection.detectObjects(image, lensFacing);

                image.close();
            }
        });
        camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));
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
        // To be able to use any languages, we must set it first on preferences.
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

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

}
