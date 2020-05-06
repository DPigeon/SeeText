package com.ctext;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionModels;
import ai.fritz.vision.FritzVisionObject;
import ai.fritz.vision.FritzVisionOrientation;
import ai.fritz.vision.ImageOrientation;
import ai.fritz.vision.objectdetection.FritzVisionObjectPredictor;
import ai.fritz.vision.objectdetection.FritzVisionObjectResult;
import ai.fritz.vision.objectdetection.ObjectDetectionOnDeviceModel;
import androidx.camera.core.ImageProxy;

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
    public void detectObjects(ImageProxy image, int lensFacing) {
        Image mediaImage = image.getImage();
        int intRotation = Utils.degreesToFirebaseRotation(image.getImageInfo().getRotationDegrees());
        ImageOrientation rotation = FritzVisionOrientation.getOrientationByDeviceRotation(intRotation, lensFacing);
        FritzVisionImage visionImage = FritzVisionImage.fromMediaImage(mediaImage, rotation);

        FritzVisionObjectResult objectResult = predictor.predict(visionImage);

        List<FritzVisionObject> objects = objectResult.getObjects();

        // Will have to make a custom overlay bounding box here
        Bitmap boundingBoxesOnImage = visionImage.overlayBoundingBoxes(objects); // Draw all boxes on image
        callback.draw(boundingBoxesOnImage);
    }
}
