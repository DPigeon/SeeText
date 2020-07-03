package com.seetext.activities.main;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.seetext.activities.definition.DefinitionActivity;
import com.seetext.facedetection.FaceDetection;
import com.seetext.objectdetection.ObjectOverlay;
import com.seetext.translator.Translator;

public abstract class AbstractInterfacesMainActivity extends AbstractSpeechMainActivity implements
        FaceDetection.Callback,
        Translator.Callback,
        ObjectOverlay.Callback {

    protected String TAG = "AbstractInterfacesMainActivity:";

    @Override
    public void updateSpeechTextViewPosition(float x, float y, boolean hasFace)  {
        // This function knows that it has detected a face
        if (!hasFace) { // Closing speech recognition
            stopListeningSpeech();
            speechTextView.setText(""); // Reset text
        } else { // Opening speech recognition with speech text
            faceCheckAnimation();
            if (mRecognizer == null) { // Not initialized
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                    initializeRecognition();
                startRecognition();
            }
            speechTextView.setX(x);
            speechTextView.setY(y);
        }
        faceProcessing = false;
    }

    @Override
    public void translateTheText(String text) {
        String sentenceToFitUI = " " + text + " ";
        speechTextView.setText(sentenceToFitUI);
        ttsSentence = text;
    }

    /* Callback for object detection touching words */
    @Override
    public void goToObjectDefinition(String word) {
        if (connectedToInternet()) {
            if (!word.equals("Loading...")) {
                if (getInputLanguage() >= 0) {
                    progressOverlay.setVisibility(View.VISIBLE);
                    Intent intent = new Intent(getApplicationContext(), DefinitionActivity.class);
                    intent.putExtra("word", word);
                    intent.putExtra("inputLanguage", getInputLanguage());
                    intent.putExtra("outputLanguage", getOutputLanguage());
                    startActivity(intent);
                } else {
                    goToProfileActivity("yes");
                    Toast.makeText(getApplicationContext(), "Set your language!", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            Toast.makeText(getApplicationContext(), "You must be connected to internet to see the definitions!", Toast.LENGTH_LONG).show();
        }
    }
}
