package com.ctext;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

/*
 * The User Profile to change his language (input language)
 */

public class ProfileActivity extends AppCompatActivity {
    private String TAG = "ProfileActivity";
    SharedPreferenceHelper sharedPreferenceHelper;
    RadioGroup languagesRadioGroup;
    Button saveButton;
    int languageChosen = 0; // Language checked on radioButton

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Profile");

        sharedPreferenceHelper = new SharedPreferenceHelper(getApplicationContext());
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
        saveButton = findViewById(R.id.saveButton);
        saveButton.setOnClickListener(view -> saveProfile());
    }

    protected void instantiateRadioGroup() {
        languagesRadioGroup = findViewById(R.id.languagesRadioGroup);

        String[] languages = getResources().getStringArray(R.array.languages_array);

        /* Setup the radioGroup list of languages */
        for (int i = 0; i < languages.length; i++)
            addRadioButtons(languages, i);
        languagesRadioGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                languageChosen = languagesRadioGroup.indexOfChild(view);
            }
        });
    }

    protected void addRadioButtons(String[] languages, int i) {
        RadioButton rButton = new RadioButton(this);
        rButton.setId(i + 1);
        rButton.setText(languages[i]);
        languagesRadioGroup.addView(rButton);
    }

    protected void setActivityFields() {
        if (sharedPreferenceHelper.getProfile() == null) { // Info does not exist
            switchMode(true, View.VISIBLE); // Switch to the edit mode
        }
        else {
            switchMode(false, View.INVISIBLE); // Switch to display mode

            // Getting the profile and displaying it
            Profile profile = sharedPreferenceHelper.getProfile();
            int currentLanguage = profile.getLanguage();
            languagesRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup radioGroup, int i) {
                    if (currentLanguage == i)
                        radioGroup.check(currentLanguage);
                }
            });
        }
    }

    protected void switchMode(boolean enabled, int view) { // Toggle between display mode and edit mode
        for (int i = 0; i < languagesRadioGroup.getChildCount(); i++) {
            languagesRadioGroup.getChildAt(i).setEnabled(enabled);
        }
        saveButton.setVisibility(view);
        saveButton.setEnabled(enabled);
    }

    protected void saveProfile() {
        if (languageChosen != 0) {
            Profile profile = new Profile(languageChosen);
            sharedPreferenceHelper.saveProfile(profile);
            toastMessage("not 0");
        }
        toastMessage(languageChosen+"");
    }

    protected void toastMessage(String message) { // Shows a toast message
        Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG); // Current pointer to the add, the string and the length if stays on
        toast.show(); // We display it
    }

}
