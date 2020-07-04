package com.seetext.activities.profile;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;

import com.seetext.R;
import com.seetext.activities.AbstractActivity;
import com.seetext.profile.SharedPreferenceHelper;

import java.util.Objects;

public abstract class AbstractProfileActivity extends AbstractActivity {

    protected abstract void setupUI();
    protected abstract void instantiateRadioGroup();
    protected abstract void setActivityFields();
    protected abstract void switchMode(boolean b, int visible);

    private String TAG = "AbstractProfileActivity";
    SharedPreferenceHelper sharedPreferenceHelper;
    ScrollView languagesScrollView;
    RadioGroup languagesRadioGroup;
    RadioButton checkedButton;
    Button saveButton;
    int languageChosen = -1; // Language checked on radioButton

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setTitle("Profile");

        sharedPreferenceHelper = new SharedPreferenceHelper(this.getSharedPreferences("ProfilePreference", Context.MODE_PRIVATE));
        setupUI();
        instantiateRadioGroup();
    }

    @Override
    protected void onStart() {
        super.onStart();
        setActivityFields();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_profile;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) { // Creates the three dot action menu
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int menuId = item.getItemId();
        if (menuId == R.id.profile_settings) { // If we click on the ... button
            switchMode(true, View.VISIBLE); // Switch to edit mode
        }
        return super.onOptionsItemSelected(item);
    }
}