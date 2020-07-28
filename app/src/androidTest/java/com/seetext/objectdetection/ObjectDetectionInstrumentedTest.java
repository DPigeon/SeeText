package com.seetext.objectdetection;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.seetext.R;
import com.seetext.activities.main.MainActivity;
import com.seetext.utils.GraphicOverlay;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ObjectDetectionInstrumentedTest {

    @Rule
    public GrantPermissionRule permissionRule =
            GrantPermissionRule.grant(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
            );

    @Rule
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<>(MainActivity.class);

    Context context;
    ObjectDetection objectDetection;
    GraphicOverlay graphicOverlay;
    TouchObjectCallback cb;

    @Before
    public void initialize() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        graphicOverlay = activityRule.getActivity().findViewById(R.id.graphicOverlay);
        objectDetection = new ObjectDetection(graphicOverlay, cb);
    }

    @Test
    public void testSuccessfulDetection() {
        int outputLanguage = FirebaseTranslateLanguage.ES;
        Bitmap image = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        objectDetection.detectObjects(context, image, outputLanguage);
    }
}
