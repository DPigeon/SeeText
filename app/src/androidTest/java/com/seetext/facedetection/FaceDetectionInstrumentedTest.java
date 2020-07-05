package com.seetext.facedetection;

import android.Manifest;
import android.content.Context;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageAnalysis;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import com.seetext.R;
import com.seetext.activities.main.MainActivity;
import com.seetext.utils.GraphicOverlay;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@RunWith(AndroidJUnit4.class)
public class FaceDetectionInstrumentedTest {

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
    FaceDetection faceDetection;
    GraphicOverlay graphicOverlay;
    FaceDetectionCallback cb;
    ImageAnalysis imageAnalysis;

    @Before
    public void initialize() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CameraX.initialize(context, Camera2Config.defaultConfig());
        imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        graphicOverlay = activityRule.getActivity().findViewById(R.id.graphicOverlay);
        faceDetection = new FaceDetection(graphicOverlay, cb);
    }

    @Test
    public void testSuccessfulDetection() {
        Executor executor = Executors.newSingleThreadExecutor();
        imageAnalysis.setAnalyzer(executor, image -> {
            faceDetection.analyzeImage(image);
        });
    }
}
