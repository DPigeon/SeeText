package com.ctext.objectdetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Size;
import android.view.MotionEvent;

import com.ctext.utils.GraphicOverlay;

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
    private BorderedText borderedText;
    private Callback callback = null;

    public ObjectOverlay(GraphicOverlay graphicOverlay, Context context, FritzVisionObject object, Bitmap image, String translatedText, Callback cb) {
        super(graphicOverlay);
        this.graphicOverlay = graphicOverlay;
        borderedText = BorderedText.createDefault(context);
        size = new Size(image.getWidth(), image.getHeight());
        scaledObject = object.scaledTo(size);
        x = scaledObject.getBoundingBox().left;
        y = scaledObject.getBoundingBox().top;
        this.translatedText = translatedText;
        this.paint = new Paint();
        this.callback = cb;
        postInvalidate(); // Redraw
    }

    /* An interface to go to definition activity */
    public interface Callback {
        void goToObjectDefinition(String word);
    }

    @Override
    public void draw(Canvas canvas) {
        borderedText.drawText(canvas, translateX(x), translateY(y), translatedText);
        paint.setStyle(Paint.Style.STROKE); // Set unfilled rectangle
        paint.setColor(Color.RED); // Set red
        canvas.drawRect(scaledObject.getBoundingBox(), paint);
    }

    @Override
    public void touchEvent(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_UP: {
                float posX = event.getX();
                float posY = event.getY();
                float leftBox = scaledObject.getBoundingBox().left;
                float rightBox = scaledObject.getBoundingBox().right;
                float bottomBox = scaledObject.getBoundingBox().bottom;
                float topBox = scaledObject.getBoundingBox().top;
                if (posX > leftBox && posX < rightBox && posY > topBox && posY < bottomBox) {
                    callback.goToObjectDefinition(translatedText);
                }
                break;
            }
        }
    }

}
