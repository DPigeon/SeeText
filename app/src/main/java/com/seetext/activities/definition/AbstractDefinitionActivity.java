package com.seetext.activities.definition;

import android.os.Bundle;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.seetext.R;
import com.seetext.activities.AbstractActivity;
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
}
