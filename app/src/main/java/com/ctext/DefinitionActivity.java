package com.ctext;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import com.ctext.objectdetection.definition.DefinitionListViewAdapter;
import com.ctext.objectdetection.definition.ObjectDefinitionAsyncTask;
import com.ctext.objectdetection.definition.DefinitionRowItem;
import com.ctext.objectdetection.definition.TranslateBackObjectAsyncTask;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DefinitionActivity extends AppCompatActivity {
    private String TAG = "DefinitionActivity";
    private String word = "";
    private String translatedWord = "";
    private int outputLanguage;
    private Boolean hasToTranslate;
    private TextView pronunciationTextView;
    private ListView definitionsListView;
    private List<DefinitionRowItem> definitionRowItems;
    private DefinitionListViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_definition);

        setupUI();
    }

    protected void setupUI() {
        Intent intent = getIntent();
        word = intent.getStringExtra("word");
        outputLanguage = intent.getIntExtra("outputLanguage", -1);
        getSupportActionBar().setTitle(word);

        pronunciationTextView = findViewById(R.id.pronunciationTextView);
        definitionsListView = findViewById(R.id.definitionsListView);
        definitionRowItems = new ArrayList<>();

        translateWordIfNeeded();

        adapter = new DefinitionListViewAdapter(this, R.layout.definition_list_item, definitionRowItems);
        definitionsListView.setAdapter(adapter);
    }

    protected void fetchDefinitions() {
        ObjectDefinitionAsyncTask objectDefinitionAsyncTask = new ObjectDefinitionAsyncTask();
        try {
            if (hasToTranslate)
                word = translatedWord;
            Log.d(TAG, word);
            JSONObject json = objectDefinitionAsyncTask.execute(word).get();
            if (json != null) {
                String pronunciation = json.getString("pronunciation");
                if (pronunciation == "null")
                    pronunciation = "";
                pronunciationTextView.setText(pronunciation);
                JSONArray definitions = json.getJSONArray("definitions");
                for (int i = 0; i < definitions.length(); i++) {
                    JSONObject definition = definitions.getJSONObject(i);
                    // Need type, definition & example
                    String type = definition.getString("type");
                    String def = definition.getString("definition");
                    String example = definition.getString("example");

                    // If no info on some string's item
                    if (type == "null")
                        type = i+1 + ". noun";
                    if (def == "null")
                        def = "";
                    if (example == "null")
                        example = "";
                    else
                        example = '"' + example + '"';
                    DefinitionRowItem item = new DefinitionRowItem(R.drawable.objects_detection, i+1 + ". " + type, def, example);
                    definitionRowItems.add(item);
                }
            }
        } catch (InterruptedException | ExecutionException | JSONException error) {
            error.printStackTrace();
        }
    }

    /* Used to translate all words back to english to get the response from dictionary API */
    protected void translateWordIfNeeded() {
        if (outputLanguage == FirebaseTranslateLanguage.EN) // Don't traslate since dictionary API already in english
            hasToTranslate = false;
        else {
            hasToTranslate = true;
            TranslateBackObjectAsyncTask tBackObjAsyncTask = new TranslateBackObjectAsyncTask(getApplicationContext(), outputLanguage);
            try {
                translatedWord = tBackObjAsyncTask.execute(word).get();
            } catch (InterruptedException | ExecutionException error) {
                error.printStackTrace();
            }
        }
        fetchDefinitions();
    }

}
