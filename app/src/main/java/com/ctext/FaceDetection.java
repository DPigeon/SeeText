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

/* FaceDetection.java
 * The FaceDetection uses Firebase to recognize facial contours, landmarks and can be fast or accurate.
 * It starts the speech recognition once a face has been detected.
 */

public class FaceDetection {
    private String TAG = "FaceDetectionActivity";
    FirebaseVisionFaceDetector detector;
    Callback callback = null;
    DetectingCallback detectingCallback = null;

    // High-accuracy landmark detection
    FirebaseVisionFaceDetectorOptions highAccuracyOpts = new FirebaseVisionFaceDetectorOptions.Builder()
            .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
            .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
            .build();

    // Factors from Rect to Screen coordinates for speech textView
    float xFactor = 0.5F;
    float yFactor = 1.5F;

    public FaceDetection(Callback cb, DetectingCallback dCb) {
        this.callback = cb;
        this.detectingCallback = dCb;
        detector = FirebaseVision.getInstance().getVisionFaceDetector(highAccuracyOpts);
    }

    /* An interface to update the position of the speech textView from Main Activity */
    public interface Callback {
        void update(float x, float y, boolean hasFace);
    }

    public interface DetectingCallback {
        void detect(boolean bool);
    }

    /* An interface to update the flag for detection to Main Activity */
    public interface DetectedCallback {
        void updateDetectionFlag();
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    public void analyzeImage(ImageProxy image) {
        Image mediaImage = image.getImage();
        int rotation = degreesToFirebaseRotation(image.getImageInfo().getRotationDegrees());
        FirebaseVisionImage imageVision = FirebaseVisionImage.fromMediaImage(mediaImage, rotation);

        Task<List<FirebaseVisionFace>> result = detector.detectInImage(imageVision).addOnSuccessListener(faces -> {
            // Task completed successfully --> Should start speech recognition HERE
            if (faces.isEmpty())  // If no face detected
                callback.update(0, 0, false);
            else { // Face(s) detected
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
                        // Show words near mouth
                        FirebaseVisionPoint mouthBottomPos = mouthBottom.getPosition();
                        // Has to match the screens preview for words to be near mouth
                        float x = mouthBottomPos.getX();
                        float y = mouthBottomPos.getY();
                        callback.update(x * xFactor, y * yFactor, true); // Rect to Screen Space from the picture
                    }
                }
            }
        }).addOnFailureListener(e -> {
            Log.d(TAG, "Error");
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
