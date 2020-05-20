package com.ctext;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import com.ctext.profile.Profile;
import com.ctext.profile.SharedPreferenceHelper;
import com.ctext.utils.Utils;

import java.util.ArrayList;

/*
 * The User Profile to change his language (input language)
 */

public class ProfileActivity extends AppCompatActivity {
    private String TAG = "ProfileActivity";
    SharedPreferenceHelper sharedPreferenceHelper;
    ScrollView languagesScrollView;
    RadioGroup languagesRadioGroup;
    Button saveButton;
    int languageChosen = -1; // Language checked on radioButton

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Profile");

        sharedPreferenceHelper = new SharedPreferenceHelper(this);
        setupUI();
        instantiateRadioGroup();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setActivityFields();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { // Creates the three dot action menu
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        if(menuId == R.id.profile_settings) { // If we click on the ... button
            switchMode(true, View.VISIBLE); // Switch to edit mode
        }
        return super.onOptionsItemSelected(item);
    }

    protected void setupUI() {
        languagesScrollView = findViewById(R.id.languagesScrollView);
        languagesRadioGroup = findViewById(R.id.languagesRadioGroup);
        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(view -> saveProfile());
    }

    private final void scrollToCheckedButton() {
        RadioButton checkedButton = findViewById(languagesRadioGroup.getCheckedRadioButtonId());
        if (checkedButton != null)
            languagesScrollView.post(() -> languagesScrollView.smoothScrollTo(0, checkedButton.getTop()));
    }

    protected void instantiateRadioGroup() {
        ArrayList<String> languages = Utils.getLanguageList();

        /* Setup the radioGroup list of languages */
        for (int i = 0; i < languages.size(); i++)
            addRadioButtons(languages.get(i), i);
    }

    protected void addRadioButtons(String languages, int i) {
        RadioButton rButton = new RadioButton(this);
        rButton.setId(i);
        rButton.setText(languages);
        rButton.setTextSize(20F);
        rButton.setOnCheckedChangeListener((compoundButton, b) -> {
            if (compoundButton.isChecked())
                languageChosen = compoundButton.getId(); // Set the current language chosen when touched
        });
        languagesRadioGroup.addView(rButton);
    }

    protected void setActivityFields() {
        if (sharedPreferenceHelper.getProfile().getLanguage() == -1) { // Info does not exist
            switchMode(true, View.VISIBLE); // Switch to the edit mode
        }
        else {
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
            Profile profile = new Profile(languageChosen, sharedPreferenceHelper.getLanguageOutput(), lensFacing, mode);
            sharedPreferenceHelper.saveProfile(profile);
            toastMessage("Your profile has been saved!");
            goToActivity(MainActivity.class);
        } else
            toastMessage("An error occured while saving your profile!");
    }

    protected void toastMessage(String message) { // Shows a toast message
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG); // Current pointer to the add, the string and the length if stays on
        toast.show(); // We display it
    }

    void goToActivity(Class activity) { // Function that goes from the main activity to another one
        Intent intent = new Intent(ProfileActivity.this, activity);
        startActivity(intent);
    }

}
