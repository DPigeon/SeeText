package com.ctext;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.util.Size;

import ai.fritz.vision.FritzVisionObject;
import ai.fritz.vision.objectdetection.BorderedText;

public class ObjectOverlay extends GraphicOverlay.Graphic {
    private String TAG = "ObjectOverlay";
    private GraphicOverlay graphicOverlay;
    private float x, y;
    private Size size;
    private String translatedText;
    private FritzVisionObject scaledObject;
    private Paint paint;
    BorderedText borderedText;

    public ObjectOverlay(GraphicOverlay graphicOverlay, Context context, FritzVisionObject object, Bitmap image, String translatedText) {
        super(graphicOverlay);
        this.graphicOverlay = graphicOverlay;
        borderedText = BorderedText.createDefault(context);
        size = new Size(image.getWidth(), image.getHeight());
        scaledObject = object.scaledTo(size);
        x = scaledObject.getBoundingBox().left;
        y = scaledObject.getBoundingBox().top;
        this.translatedText = translatedText;
        this.paint = new Paint();
        postInvalidate(); // Redraw
    }

    @Override
    public void draw(Canvas canvas) {
        borderedText.drawText(canvas, translateX(x), translateY(y), translatedText);
        paint.setStyle(Paint.Style.STROKE); // Set unfilled rectangle
        paint.setColor(Color.RED); // Set red
        canvas.drawRect(scaledObject.getBoundingBox(), paint);
    }
}
