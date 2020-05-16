package com.ctext.facedetection;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.Image;
import android.util.Log;

import com.ctext.facedetection.FaceOverlay;
import com.ctext.utils.GraphicOverlay;
import com.ctext.utils.Utils;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
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
    private String TAG = "FaceDetection";
    private FirebaseVisionFaceDetector detector;
    private Callback callback = null;
    private GraphicOverlay graphicOverlay;

    // Factors from Rect to Screen coordinates for speech textView
    private float xFactor = 0.5F;
    private float yFactor = 1.75F;

    // High-accuracy landmark detection
    private FirebaseVisionFaceDetectorOptions highAccuracyOpts = new FirebaseVisionFaceDetectorOptions.Builder()
        .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
        .build();

    public FaceDetection(GraphicOverlay graphicOverlay, Callback cb) {
        this.graphicOverlay = graphicOverlay;
        this.callback = cb;
        detector = FirebaseVision.getInstance().getVisionFaceDetector(highAccuracyOpts);
    }

    /* An interface to update the position of the speech textView from Main Activity */
    public interface Callback {
        void update(float x, float y, boolean hasFace);
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    public void analyzeImage(ImageProxy image) {
        Image mediaImage = image.getImage();
        int rotation = Utils.degreesToFirebaseRotation(image.getImageInfo().getRotationDegrees());
        assert mediaImage != null;
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
    }
}
