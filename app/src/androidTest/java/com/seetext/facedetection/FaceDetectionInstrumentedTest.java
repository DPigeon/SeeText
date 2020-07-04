package com.seetext.facedetection;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraX;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageProxy;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

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
    public ActivityTestRule<MainActivity> activityRule =
            new ActivityTestRule<>(MainActivity.class);

    Context context;
    FaceDetection faceDetection;
    GraphicOverlay graphicOverlay;
    FaceDetection.Callback cb;
    ImageAnalysis imageAnalysis;

    @Before
    public void initPermissions() throws Throwable {
        String[] permissions = {"permissions"};
        int[] grantResults = {0};
        activityRule.runOnUiThread(() -> {
            activityRule.getActivity().onRequestPermissionsResult(100, permissions, grantResults);
        });
    }

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
