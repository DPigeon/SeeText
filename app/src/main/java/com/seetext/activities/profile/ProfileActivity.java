package com.seetext.activities.profile;

import android.content.Intent;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;

import com.seetext.R;
import com.seetext.activities.main.MainActivity;
import com.seetext.profile.Profile;
import com.seetext.translator.Translator;
import com.seetext.utils.Utils;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateRemoteModel;

import java.util.ArrayList;

/*
 * The User Profile to change his language (input language)
 */

public class ProfileActivity extends AbstractProfileActivity {

    protected String TAG = "ProfileActivity:";

    protected void setupUI() {
        languagesScrollView = findViewById(R.id.languagesScrollView);
        languagesRadioGroup = findViewById(R.id.languagesRadioGroup);
        checkedButton = findViewById(languagesRadioGroup.getCheckedRadioButtonId());
        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(view -> saveProfile());
    }

    private void scrollToCheckedButton() {
        if (checkedButton != null) {
            languagesScrollView.post(() -> languagesScrollView.smoothScrollTo(0, checkedButton.getTop()));
        }
    }

    protected void instantiateRadioGroup() {
        ArrayList<String> languages = Utils.getLanguageList();

        /* Setup the radioGroup list of languages */
        for (int i = 0; i < languages.size(); i++) {
            addRadioButtons(languages.get(i), i);
        }
    }

    protected void addRadioButtons(String languages, int i) {
        RadioButton rButton = new RadioButton(this);
        rButton.setId(i);
        rButton.setText(languages);
        rButton.setTextSize(20F);
        rButton.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isChecked()) {
                languageChosen = compoundButton.getId(); // Set the current language chosen when touched
            }
        });
        languagesRadioGroup.addView(rButton);
    }

    protected void setActivityFields() {
        Intent intent = getIntent();
        String firstTime = intent.getStringExtra("firstTime");
        assert firstTime != null;
        if (firstTime.equals("yes")) { // If first time launching app
            switchMode(true, View.VISIBLE); // Switch to the edit mode
        } else {
            switchMode(false, View.INVISIBLE); // Switch to display mode

            // Getting the profile and displaying it
            Profile profile = sharedPreferenceHelper.getProfile();
            int currentLanguage = profile.getLanguage();
            languagesRadioGroup.check(currentLanguage);
        }
        scrollToCheckedButton();
    }

    protected void switchMode(boolean enabled, int view) { // Toggle between display mode and edit mode
        for (int i = 0; i < languagesRadioGroup.getChildCount(); i++) {
            languagesRadioGroup.getChildAt(i).setEnabled(enabled);
        }
        saveButton.setVisibility(view);
        saveButton.setEnabled(enabled);
    }

    protected void saveProfile() {
        if (languageChosen != -1) {
            Intent intent = getIntent();
            int lensFacing = -1, mode = -1;
            if (intent != null) {
                lensFacing = intent.getIntExtra("lensFacing", -1);
                mode = intent.getIntExtra("mode", -1);
            }
            int outputLanguage = sharedPreferenceHelper.getLanguageOutput();
            Profile profile = new Profile(languageChosen, outputLanguage, lensFacing, mode);
            sharedPreferenceHelper.saveProfile(profile);
            toastMessage("Your profile has been saved!");
            downloadModel(outputLanguage);
            goToActivity();
        } else {
            toastMessage("You must choose a language!");
        }
    }

    private void downloadModel(int outputLanguage) {
        /* Here we download the input model to be able to view translated sentences in definitions */
        if (outputLanguage >= 0) {
            Translator translator = new Translator(getApplicationContext(), languageChosen, outputLanguage);
            FirebaseTranslateRemoteModel model = new FirebaseTranslateRemoteModel.Builder(languageChosen).build();
            translator.checkAndDownloadModel(model);
        }
    }

    protected void toastMessage(String message) { // Shows a toast message
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG); // Current pointer to the add, the string and the length if stays on
        toast.show(); // We display it
    }

    private void goToActivity() { // Function that goes from the main activity to another one
        Intent intent = getIntent();
        String firstTime = intent.getStringExtra("firstTime");
        Intent mainIntent = new Intent(ProfileActivity.this, MainActivity.class);
        if (firstTime != null && firstTime.equals("yes")) {
            mainIntent.putExtra("tutorial", "start");
        }
        startActivity(mainIntent);
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
}