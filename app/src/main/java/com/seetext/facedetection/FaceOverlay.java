package com.seetext.facedetection;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.view.MotionEvent;

import com.seetext.utils.GraphicOverlay;

/*
 * Will be used later to create a very nice face detection animation for about 2-3 secs
 */

public class FaceOverlay extends GraphicOverlay.Graphic {
    private String TAG = "FaceOverlay";
    private GraphicOverlay graphicOverlay;
    private Rect bounds;
    private Paint paint;

    public FaceOverlay(GraphicOverlay graphicOverlay, Rect bounds) {
        super(graphicOverlay);
        this.graphicOverlay = graphicOverlay;
        this.bounds = bounds;
        this.paint = new Paint();
        postInvalidate(); // Redraw
    }

    @Override
    public void draw(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE); // Set unfilled rectangle
        paint.setColor(Color.BLUE); // Set blue
        canvas.drawRect(bounds, paint);
    }

    @Override
    public void touchEvent(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        if (action == MotionEvent.ACTION_UP) {
            float posX = event.getX();
            float posY = event.getY();
            float leftBox = bounds.left;
            float rightBox = bounds.right;
            float bottomBox = bounds.bottom;
            float topBox = bounds.top;
            if (posX > leftBox && posX < rightBox && posY > topBox && posY < bottomBox) {
                Log.d(TAG, "Touching a face...");
            }
        }
    }

}

