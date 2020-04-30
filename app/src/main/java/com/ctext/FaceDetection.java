package com.ctext;

import android.annotation.SuppressLint;
import android.media.Image;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.util.List;

import androidx.camera.core.ImageProxy;

public class FaceDetection {
    private String TAG = "FaceDetectionActivity";
    FirebaseVisionFaceDetector detector;
    Callback callback = null;

    // High-accuracy landmark detection
    FirebaseVisionFaceDetectorOptions highAccuracyOpts = new FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .build();

    public FaceDetection(Callback cb) {
        this.callback = cb;
        detector = FirebaseVision.getInstance().getVisionFaceDetector(highAccuracyOpts);
    }

    /* An interface to update the position of the speech textView from Main Activity */
    public interface Callback {
        void updateSpeechTextView(float x, float y);
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    public void detect(ImageProxy image) {
        Image mediaImage = image.getImage();
        int rotation = degreesToFirebaseRotation(image.getImageInfo().getRotationDegrees());
        FirebaseVisionImage imageVision = FirebaseVisionImage.fromMediaImage(mediaImage, rotation);

        Task<List<FirebaseVisionFace>> result = detector.detectInImage(imageVision).addOnSuccessListener(faces -> {
            // Task completed successfully --> Should start speech recognition HERE
            for (FirebaseVisionFace face : faces) {
                // Check if face has mouth, ears, eyes, etc
                FirebaseVisionFaceLandmark leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR);
                FirebaseVisionFaceLandmark rightEar = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR);
                FirebaseVisionFaceLandmark leftEye = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EYE);
                FirebaseVisionFaceLandmark rightEye = face.getLandmark(FirebaseVisionFaceLandmark.RIGHT_EAR);
                FirebaseVisionFaceLandmark mouthBottom = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_BOTTOM);
                FirebaseVisionFaceLandmark mouthLeft = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_LEFT);
                FirebaseVisionFaceLandmark mouthRight = face.getLandmark(FirebaseVisionFaceLandmark.MOUTH_RIGHT);
                FirebaseVisionFaceLandmark nose = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE);
                if (leftEar != null && rightEar != null && leftEye != null && rightEye != null && mouthBottom != null && mouthLeft != null && mouthRight != null && nose != null) {
                    // Start Speech and show words near mouth
                    FirebaseVisionPoint mouthBottomPos = mouthBottom.getPosition();
                    // Has to match the screens preview for words to be near mouth
                    float x = mouthBottomPos.getX();
                    float y = mouthBottomPos.getX();
                    callback.updateSpeechTextView(x, y);
                }
            }
        }).addOnFailureListener(e -> {
            Log.d(TAG, "error");
            // Task failed with an exception --> No speech
        });
        //image.close(); // Closes the images to have multi-frames analysis for real time preview (CAUSES MEMORY LEAK WILL HAVE TO FIX)
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
