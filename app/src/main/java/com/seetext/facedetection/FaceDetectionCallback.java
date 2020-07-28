package com.seetext.facedetection;

/*
 * An interface to update the position of the speech textView from Main Activity
 */

public interface FaceDetectionCallback {
    void updateSpeechTextViewPosition(float x, float y, boolean hasFace);
}
