package com.ctext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.Log;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import java.util.List;

import ai.fritz.vision.FritzVision;
import ai.fritz.vision.FritzVisionImage;
import ai.fritz.vision.FritzVisionModels;
import ai.fritz.vision.FritzVisionObject;
import ai.fritz.vision.objectdetection.BorderedText;
import ai.fritz.vision.objectdetection.FritzVisionObjectPredictor;
import ai.fritz.vision.objectdetection.FritzVisionObjectResult;
import ai.fritz.vision.objectdetection.ObjectDetectionOnDeviceModel;

public class ObjectDetection {
    private String TAG = "ObjectDetection";
    Translator translator;
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
    public void detectObjects(Context context, Bitmap image, int lensFacing, int outputLanguage) {
        FritzVisionImage visionImage = FritzVisionImage.fromBitmap(image);

        FritzVisionObjectResult objectResult = predictor.predict(visionImage);
        boolean sameAsOutput = true;
        if (outputLanguage != FirebaseTranslateLanguage.EN) { // If english by default
            sameAsOutput = false;
            translator = new Translator(context, FirebaseTranslateLanguage.EN, outputLanguage); // Input always english for Fritz AI
        }

        Canvas canvas = new Canvas(image);

        List<FritzVisionObject> objects = objectResult.getObjects();
        if (!sameAsOutput) { // Translator activated
            for (FritzVisionObject object : objects) {
                // Translate the appropriate text
                String text = object.getVisionLabel().getText();
                String translatedText = translator.translateObject(text, outputLanguage);
                BorderedText borderedText = BorderedText.createDefault(context);
                float x = object.getBoundingBox().centerX();
                float y = object.getBoundingBox().centerY();
                borderedText.drawText(canvas, x, y, translatedText);
                callback.draw(image);
            }
        } else { // No translator needed because same input and output languages (english)
            Bitmap boundingBoxesOnImage = visionImage.overlayBoundingBoxes(objects); // Draw all boxes on image
            callback.draw(boundingBoxesOnImage);
        }
    }
}
