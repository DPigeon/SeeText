package com.seetext.facedetection;

import android.annotation.SuppressLint;
import android.media.Image;
import android.util.Log;

import com.seetext.utils.GraphicOverlay;
import com.seetext.utils.Utils;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionPoint;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark;

import java.util.ArrayList;
import java.util.List;

import androidx.camera.core.ImageProxy;

/* FaceDetection.java
 * The FaceDetection uses Firebase to recognize facial contours, landmarks and can be fast or accurate.
 * It starts the speech recognition once a face has been detected.
 */

public class FaceDetection {
    private String TAG = "FaceDetection";
    private FirebaseVisionFaceDetector detector;
    private FaceDetectionCallback callback = null;
    private GraphicOverlay graphicOverlay;

    public FaceDetection(GraphicOverlay graphicOverlay, FaceDetectionCallback cb) {
        this.graphicOverlay = graphicOverlay;
        this.callback = cb;
        // High-accuracy landmark detection
        FirebaseVisionFaceDetectorOptions highAccuracyOpts = new FirebaseVisionFaceDetectorOptions.Builder()
                .setPerformanceMode(FirebaseVisionFaceDetectorOptions.FAST) // Slow calls
                .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
                .build();
        detector = FirebaseVision.getInstance().getVisionFaceDetector(highAccuracyOpts);
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    public void analyzeImage(ImageProxy image) {
        Image mediaImage = image.getImage();
        assert mediaImage != null;
        int width = mediaImage.getWidth();
        int height = mediaImage.getHeight();
        int rotation = Utils.degreesToFirebaseRotation(image.getImageInfo().getRotationDegrees());
        FirebaseVisionImage imageVision = FirebaseVisionImage.fromMediaImage(mediaImage, rotation);

        detector.detectInImage(imageVision).addOnSuccessListener(faces -> {
            // Task completed successfully --> Should start speech recognition HERE
            if (faces.isEmpty())  // If no face detected
                callback.updateSpeechTextViewPosition(0, 0, false);
            else  // Face(s) detected
                processFace(faces, width, height);
        }).addOnFailureListener(e -> {
            Log.d(TAG, "Error");
            // Task failed with an exception --> No speech
        });
    }

    protected void processFace(List<FirebaseVisionFace> faces, int width, int height) {
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
                ArrayList<Integer> mappedXY = mapNewMessageCoordinates(width, height, (int)x, (int)y);
                callback.updateSpeechTextViewPosition((float)mappedXY.get(0), (float)mappedXY.get(1), true);// Rect to Screen Space from the picture
            }
        }
    }

    /* Maps new coordinates from dimensions of imageAnalysis mediaImages to real phone images */
    protected ArrayList<Integer> mapNewMessageCoordinates(int oldWidth, int oldHeight, int x, int y) {
        ArrayList<Integer> coordinates = new ArrayList<>();
        int widthRatio = Utils.getScreenWidth() / oldWidth;
        int heightRatio = Utils.getScreenHeight() / oldHeight;

        double offsetX = 0.75;
        double offsetY = 0.05;

        int newX = (int) (x * widthRatio - x * widthRatio * offsetX);
        int newY = (int) (y * heightRatio + y * heightRatio * offsetY);
        coordinates.add(newX);
        coordinates.add(newY);

        return coordinates;
    }

}
