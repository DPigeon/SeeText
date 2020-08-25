package com.seetext.activities.main;

import android.content.Intent;
import android.view.View;
import android.widget.Toast;

import com.seetext.R;

import smartdevelop.ir.eram.showcaseviewlib.GuideView;
import smartdevelop.ir.eram.showcaseviewlib.config.DismissType;

public abstract class AbstractGuideTourMainActivity extends AbstractUIMainActivity {
    protected void guideTour() {
        Intent intent = getIntent();
        String tutorial = intent.getStringExtra("tutorial");

        if (tutorial != null) {
            if (tutorial.equals("start")) {
                guideViewBuilder = new GuideView.Builder(this)
                        .setTitle("Profile Button")
                        .setContentText("Used to you to edit your profile. \n" +
                                "Touch to continue...")
                        .setTargetView(userProfileImageView)
                        .setDismissType(DismissType.targetView)
                        .setGuideListener(view -> {
                            switch (view.getId()) {
                                case R.id.userProfileImageView:
                                    guideTourStep(speechDetectionImageView, "Speech Detection Mode", "Used to enter the speech-face \n" +
                                            "recognition mode. \n" +
                                            "Touch to continue...");
                                    break;
                                case R.id.speechDetectionImageView:
                                    guideTourStep(swapLanguageImageView, "Swap Languages", "Allows you to swap spoken \n" +
                                            "and translated languages for discussions \n" +
                                            "between you and someone else. \n" +
                                            "Touch to continue...");
                                    break;
                                case R.id.swapLanguageImageView:
                                    guideTourStep(swapInputLanguage, "Language Spoken", "The language set in your profile. \n" +
                                            "Touch to continue...");
                                    break;
                                case R.id.inputLanguageTextView:
                                    guideTourStep(languageTextView, "Language Translated", "The language you want to translate to. \n" +
                                            "Touch to continue...");
                                    break;
                                case R.id.languageTextView:
                                    guideTourStep(objectDetectionImageView, "Object Detection Mode", "Used to enter the object detection mode. \n" +
                                            "Tip: touch objects detected to get definitions of them. \n" +
                                            "Touch to continue...");
                                    break;
                                case R.id.objectDetectionImageView:
                                    guideTourStep(languagesImageView, "Translate Languages", "Used to change the translated language. \n" +
                                            "Touch to continue...");
                                    break;
                                case R.id.languagesImageView:
                                    guideTourStep(flashLightImageView, "Flash Light", "Used to see in the dark. \n" +
                                            "Touch to continue...");
                                    break;
                                case R.id.flashLightImageView:
                                    guideTourStep(cameraModeImageView, "Camera Mode", "Used to change the camera lens side. \n" +
                                            "Touch to continue...");
                                    break;
                                case R.id.cameraModeImageView:
                                    Toast.makeText(this, "You have completed the tutorial!", Toast.LENGTH_LONG).show();
                                    return;
                            }
                            guideView = guideViewBuilder.build();
                            guideView.show();
                        });
                guideView = guideViewBuilder.build();
                guideView.show();
            }
        }
    }

    private void guideTourStep(View view, String title, String description) {
        guideViewBuilder.setTargetView(view)
                .setTitle(title)
                .setContentText(description)
                .build();
    }
}
