package com.seetext.activities.main;

import android.graphics.Canvas;
import android.view.MotionEvent;

import com.seetext.utils.GraphicOverlay;

public class FlashLightOverlay extends GraphicOverlay.Graphic {

    public FlashLightOverlay(GraphicOverlay graphicOverlay) {
        super(graphicOverlay);
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawARGB(1, 0, 0, 0);
    }

    @Override
    public void touchEvent(MotionEvent event) {
        // No touch needed
    }
}
