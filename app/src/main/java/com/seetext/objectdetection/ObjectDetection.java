package com.seetext.objectdetection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;

import com.seetext.utils.GraphicOverlay;
import com.seetext.translator.Translator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import java.util.List;

import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionModels;
import ai.fritz.vision.FritzVisionObject;
import ai.fritz.vision.objectdetection.FritzVisionObjectPredictor;
import ai.fritz.vision.objectdetection.FritzVisionObjectResult;
import ai.fritz.vision.objectdetection.ObjectDetectionOnDeviceModel;

/*
 * The Object Detection class using the Fritz API. It gets the device model and detects objects.
 * It then draws the imageViews with unfilled rectangle & bordered text.
 */

public class ObjectDetection {

    private String TAG = "ObjectDetection";
    private Translator translator;
    private FritzVisionObjectPredictor predictor;
    private GraphicOverlay graphicOverlay;
    private TouchObjectCallback callback;

    public ObjectDetection(GraphicOverlay graphicOverlay, TouchObjectCallback cb) {
        this.graphicOverlay = graphicOverlay;
        ObjectDetectionOnDeviceModel onDeviceModel = FritzVisionModels.getObjectDetectionOnDeviceModel();
        predictor = FritzVision.ObjectDetection.getPredictor(onDeviceModel);
        this.callback = cb;
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    public void detectObjects(Context context, Bitmap image, int outputLanguage) {
        FritzVisionImage visionImage = FritzVisionImage.fromBitmap(image);

        FritzVisionObjectResult objectResult = predictor.predict(visionImage);
        boolean sameAsOutput = true;
        if (outputLanguage != FirebaseTranslateLanguage.EN) { // If english by default
            sameAsOutput = false;
            translator = new Translator(context, FirebaseTranslateLanguage.EN, outputLanguage); // Input always english for Fritz AI
        }

        List<FritzVisionObject> objects = objectResult.getObjects();
        graphicOverlay.clear();
        drawObject(context, sameAsOutput, objects, outputLanguage, image);
    }

    private void drawObject(Context context, boolean sameAsOutput, List<FritzVisionObject> objects, int outputLanguage, Bitmap image) {
        for (FritzVisionObject object : objects) {
            String text = object.getVisionLabel().getText();
            ObjectOverlay objectOverlay;
            if (!sameAsOutput) {
                String translatedText = translator.translateObject(text, outputLanguage);
                objectOverlay = new ObjectOverlay(graphicOverlay, context, object, image, translatedText, callback);
            } else {
                objectOverlay = new ObjectOverlay(graphicOverlay, context, object, image, text, callback);
            }
            graphicOverlay.add(objectOverlay);
        }
    }
}
