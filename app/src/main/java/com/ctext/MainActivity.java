package com.ctext;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.util.Size;
import android.view.animation.AnimationUtils;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

public class MainActivity extends AppCompatActivity implements RecognitionListener, FaceDetection.Callback {
    private String TAG = "MainActivity:";

    /* Video Variables */
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private Switch cameraSwitch;

    /* Audio Variables */
    private SpeechRecognizer mRecognizer;
    private TextView speechTextView;
    private AudioManager mAudioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Running UI Thread to get audio permission */
        this.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                showPermissions();
            }
        });

        setupUI();
        initializeRecognition();

        listenForSpeech();
    }

    @Override
    public void updateSpeechTextView(float x, float y)  {
        speechTextView.setX(x);
        speechTextView.setY(y);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {
        //showToastMessage("Say something!");
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
        try {
            this.runOnUiThread(() -> speechTextView.setText(sentence));
            speechTextView.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));

        } catch (Exception exception) {

        }

        persistentSpeech();
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }

    protected void setupUI() {
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider, CameraSelector.LENS_FACING_BACK); // Default facing back
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
        cameraSwitch = findViewById(R.id.cameraSwitch);
        cameraSwitch.setOnCheckedChangeListener((compoundButton, b) -> {
            int lensFacing;
            if (compoundButton.isChecked()) {
                cameraSwitch.setText("Front");
                lensFacing = 0;
            } else {
                cameraSwitch.setText("Back");
                lensFacing = 1;
            }
            try {
                cameraProviderFuture.get().unbindAll(); // Unbind all other cameras
                cameraProviderFuture = ProcessCameraProvider.getInstance(this);
                bindPreview(cameraProviderFuture.get(), lensFacing); // Change lens facing
            } catch (Exception exception) {

            }
        });

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        speechTextView = findViewById(R.id.speechTextView);
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    protected void bindPreview(@NonNull ProcessCameraProvider cameraProvider, int lensFacing) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        Size size = new Size(480, 360); // For better latency
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder().setTargetResolution(size).setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();

        /* Image Processing Face Detection */
        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, image -> {
            if (image == null || image.getImage() == null) {
                return;
            }
            FaceDetection faceDetection = new FaceDetection(this::updateSpeechTextView);
            faceDetection.detect(image);
        });
        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);
        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));
    }

    /* Used to grant permission from the UI thread */
    protected void showPermissions() {
        int MY_PERMISSIONS = 1;
        MainActivity thisActivity = this;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) // Videos
            ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) // Audio
            ActivityCompat.requestPermissions(thisActivity, new String[]{Manifest.permission.RECORD_AUDIO}, MY_PERMISSIONS);
    }

    protected void initializeRecognition() {
        mRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        mRecognizer.setRecognitionListener(this);
    }

    /* Starts the speech */
    public void listenForSpeech() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getClass().getPackage().getName());
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true); // Mutes any sound of beep for listening
        mRecognizer.startListening(intent);
    }

    /* Makes the app always listen to inputs */
    protected void persistentSpeech() {
        mRecognizer.destroy();
        mRecognizer = null;
        initializeRecognition();
        listenForSpeech();
    }

    void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private int degreesToFirebaseRotation(int degrees) {
        switch (degrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                throw new IllegalArgumentException("Rotation must be 0, 90, 180, or 270.");
        }
    }

}
