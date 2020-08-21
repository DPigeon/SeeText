package com.seetext.activities;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.camera.core.CameraSelector;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro;
import com.github.appintro.AppIntroFragment;
import com.seetext.R;
import com.seetext.activities.main.MainActivity;
import com.seetext.activities.profile.ProfileActivity;

import java.util.Objects;

public class IntroActivity extends AppIntro {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();

        setStyle();
        setSlides();
        setPermissions();
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        goToNextActivity();
        Toast.makeText(getApplicationContext(), "Create your profile!", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        goToNextActivity();
        Toast.makeText(getApplicationContext(), "Create your profile!", Toast.LENGTH_LONG).show();
        finish();
    }

    private void setStyle() {
        setIndicatorColor(
                getColor(R.color.colorPrimary),
                getColor(R.color.appintro_icon_tint)
        );
    }

    private void setSlides() {
        addSlide(AppIntroFragment.newInstance(
                "Welcome to SeeText!",
                "Understand languages the easy way.",
                R.drawable.objects_detection_enabled
        ));
        addSlide(AppIntroFragment.newInstance(
                "Camera Permission",
                "In order to access your camera, you must give permissions." +
                        " Note that SeeText does not store or record any video.",
                R.drawable.camera_mode
        ));
        addSlide(AppIntroFragment.newInstance(
                "Record Audio Permission",
                "In order to access your microphone, you must give permissions." +
                        " Note that SeeText does not store any audio.",
                R.drawable.tts_audio
        ));
        addSlide(AppIntroFragment.newInstance(
                "All Set!",
                "You are now ready to see text!" +
                        " No pun intended..." +
                        " Let's configure your profile.",
                R.drawable.face_check
        ));
    }

    private void setPermissions() {
        askForPermissions(
                new String[]{Manifest.permission.CAMERA},
                2,
                true
        );
        askForPermissions(
                new String[]{Manifest.permission.RECORD_AUDIO},
                3,
                true
        );
    }

    private void goToNextActivity() {
        Intent intent = new Intent(IntroActivity.this, ProfileActivity.class);
        intent.putExtra("lensFacing", CameraSelector.LENS_FACING_BACK);
        intent.putExtra("mode", 0);
        intent.putExtra("firstTime", "yes");
        startActivity(intent);
    }
}