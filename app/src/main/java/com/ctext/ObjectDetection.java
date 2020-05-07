package com.ctext;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import java.util.List;

import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionModels;
import ai.fritz.vision.FritzVisionObject;
import ai.fritz.vision.objectdetection.FritzVisionObjectPredictor;
import ai.fritz.vision.objectdetection.FritzVisionObjectResult;
import ai.fritz.vision.objectdetection.ObjectDetectionOnDeviceModel;

public class ObjectDetection {
    private String TAG = "FritzObjectDetection";
    ObjectDetectionOnDeviceModel onDeviceModel;
    FritzVisionObjectPredictor predictor;
    Callback callback = null;

    public ObjectDetection(Callback cb) {
        this.callback = cb;
        onDeviceModel = FritzVisionModels.getObjectDetectionOnDeviceModel();
        predictor = FritzVision.ObjectDetection.getPredictor(onDeviceModel);
    }

    /* An interface to update the image after detecting in Main Activity */
    public interface Callback {
        void draw(Bitmap bitmap);
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    public void detectObjects(Bitmap image, int lensFacing) {
        FritzVisionImage visionImage = FritzVisionImage.fromBitmap(image);

        FritzVisionObjectResult objectResult = predictor.predict(visionImage);

        List<FritzVisionObject> objects = objectResult.getObjects();

        // Will have to make a custom overlay bounding box here
        Bitmap boundingBoxesOnImage = visionImage.overlayBoundingBoxes(objects); // Draw all boxes on image
        callback.draw(boundingBoxesOnImage);
    }
}
