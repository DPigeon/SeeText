package com.ctext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Size;

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

/*
 * The Object Detection class using the Fritz API. It gets the device model and detects objects.
 * It then draws the imageViews with unfilled rectangle & bordered text.
 */

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
                String text = object.getVisionLabel().getText();
                String translatedText = translator.translateObject(text, outputLanguage);
                drawBoxesAndLabels(context, object, image, canvas, translatedText);
            }
        } else { // No translator needed because same input and output languages (english)
            for (FritzVisionObject object : objects) {
                String text = object.getVisionLabel().getText();
                drawBoxesAndLabels(context, object, image, canvas, text);
            }
        }
    }

    /* Custom bounding boxes & labels with no score value */
    public void drawBoxesAndLabels(Context context, FritzVisionObject object, Bitmap image, Canvas canvas, String translatedText) {
        // Translate the appropriate text
        BorderedText borderedText = BorderedText.createDefault(context);
        Size size = new Size(image.getWidth(), image.getHeight());
        FritzVisionObject scaledObject = object.scaledTo(size);
        float x = scaledObject.getBoundingBox().left;
        float y = scaledObject.getBoundingBox().top;
        borderedText.drawText(canvas, x, y, translatedText);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE); // Set unfilled rectangle
        paint.setColor(Color.RED); // Set red
        canvas.drawRect(scaledObject.getBoundingBox(), paint);
        callback.draw(image);
    }

}
