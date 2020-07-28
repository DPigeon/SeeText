package com.seetext.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/*
 * The top base abstract activity for main activities
 */

public abstract class AbstractActivity extends AppCompatActivity {

    protected abstract int getLayoutResourceId();

    protected String TAG = "AbstractActivity:";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutResourceId());
    }
}
