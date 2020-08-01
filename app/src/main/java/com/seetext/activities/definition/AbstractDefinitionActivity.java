package com.seetext.activities.definition;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.seetext.R;
import com.seetext.activities.AbstractActivity;
import com.seetext.activities.main.MainActivity;
import com.seetext.objectdetection.definition.DefinitionListViewAdapter;
import com.seetext.objectdetection.definition.DefinitionRowItem;

import java.util.List;

public abstract class AbstractDefinitionActivity extends AbstractActivity {

    protected abstract void setupUI();

    String TAG = "AbstractDefinitionActivity";
    String word = "";
    String translatedWord = "";
    int inputLanguage;
    int outputLanguage;
    Boolean hasToTranslate;
    TextView pronunciationTextView;
    ListView definitionsListView;
    List<DefinitionRowItem> definitionRowItems;
    List<DefinitionRowItem> transDefinitionRowItems;
    DefinitionListViewAdapter adapter;
    Switch languageSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupUI();
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.activity_definition;
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        return true;
    }
}
