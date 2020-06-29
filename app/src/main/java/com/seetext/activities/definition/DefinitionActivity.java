package com.seetext.activities.definition;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import com.seetext.R;
import com.seetext.objectdetection.definition.DefinitionListViewAdapter;
import com.seetext.objectdetection.definition.ObjectDefinitionAsyncTask;
import com.seetext.objectdetection.definition.DefinitionRowItem;
import com.seetext.objectdetection.definition.TranslateBackObjectAsyncTask;
import com.seetext.utils.Utils;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

public class DefinitionActivity extends AppCompatActivity {
    private String TAG = "DefinitionActivity";
    private String word = "";
    private String translatedWord = "";
    private int inputLanguage;
    private int outputLanguage;
    private Boolean hasToTranslate;
    private TextView pronunciationTextView;
    private ListView definitionsListView;
    private List<DefinitionRowItem> definitionRowItems;
    private List<DefinitionRowItem> transDefinitionRowItems;
    private DefinitionListViewAdapter adapter;
    private Switch languageSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_definition);

        setupUI();
    }

    protected void setupUI() {
        Intent intent = getIntent();
        word = intent.getStringExtra("word");
        inputLanguage = intent.getIntExtra("inputLanguage", -1);
        outputLanguage = intent.getIntExtra("outputLanguage", -1);

        int wordCount = wordCount(word); // We need 1 word
        if (wordCount > 1) {
            word = word.substring(word.lastIndexOf(" ") + 1); // Get last word (usually the one we look for)
        }

        String title = word;
        String inputLanguageString = Utils.getLanguageList().get(inputLanguage);
        String outputLanguageString = Utils.getLanguageList().get(outputLanguage);
        if (outputLanguage != FirebaseTranslateLanguage.EN) {
            title = word + " [" + outputLanguageString + "]";
        }
        Objects.requireNonNull(getSupportActionBar()).setTitle(title);

        pronunciationTextView = findViewById(R.id.pronunciationTextView);
        definitionsListView = findViewById(R.id.definitionsListView);
        languageSwitch = findViewById(R.id.languageSwitch);
        languageSwitch.setText(inputLanguageString);
        languageSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                languageSwitch.setText(outputLanguageString);
                adapter = new DefinitionListViewAdapter(this, R.layout.definition_list_item, definitionRowItems); // Set default translated definitions
            } else {
                languageSwitch.setText(inputLanguageString);
                adapter = new DefinitionListViewAdapter(this, R.layout.definition_list_item, transDefinitionRowItems); // Set default translated definitions
            }
            definitionsListView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        });
        // Don't show switch option if input same as output
        if (inputLanguage == outputLanguage) {
            languageSwitch.setVisibility(View.INVISIBLE);
        }

        definitionRowItems = new ArrayList<>();
        transDefinitionRowItems = new ArrayList<>();

        translateWordIfNeeded();

        adapter = new DefinitionListViewAdapter(this, R.layout.definition_list_item, transDefinitionRowItems); // Set default translated definitions
        definitionsListView.setAdapter(adapter);
    }

    /* Used to translate all words back to english to get the response from dictionary API */
    protected void translateWordIfNeeded() {
        if (outputLanguage == FirebaseTranslateLanguage.EN) // Don't traslate since dictionary API already in english
            hasToTranslate = false;
        else {
            hasToTranslate = true;
            TranslateBackObjectAsyncTask tBackObjAsyncTask = new TranslateBackObjectAsyncTask(getApplicationContext(), outputLanguage);
            try {
                translatedWord = tBackObjAsyncTask.execute(word).get()[0];
            } catch (InterruptedException | ExecutionException error) {
                error.printStackTrace();
            }
        }
        fetchDefinitions();
    }

    protected void fetchDefinitions() {
        ObjectDefinitionAsyncTask objectDefinitionAsyncTask = new ObjectDefinitionAsyncTask();
        try {
            if (hasToTranslate) {
                word = translatedWord;
            }
            JSONObject json = objectDefinitionAsyncTask.execute(word).get();
            if (json != null) {
                String pronunciation = json.getString("pronunciation");
                if (pronunciation.equals("null") || hasToTranslate)
                    pronunciation = "";
                pronunciationTextView.setText(pronunciation);
                JSONArray definitions = json.getJSONArray("definitions");
                for (int i = 0; i < definitions.length(); i++) {
                    JSONObject definition = definitions.getJSONObject(i);
                    // Need type, definition & example
                    String type = definition.getString("type");
                    String def = definition.getString("definition");
                    String example = definition.getString("example");

                    correctWords(i, type, def, example);
                    translateActivity(inputLanguage, type, def, example, i, definitionRowItems);
                    translateActivity(outputLanguage, type, def, example, i, transDefinitionRowItems);
                }
            }
        } catch (InterruptedException | ExecutionException | JSONException error) {
            error.printStackTrace();
        }
    }

    private void correctWords(int i, String type, String def, String example) {
        // If no info on some string's item
        if (type.equals("null"))
            type = i + 1 + ". noun";
        if (def.equals("null"))
            def = "";
        if (example.equals("null"))
            example = "";
        else
            example = '"' + example + '"';

        // Changing this definition for inappropriate words from API... ex: oven
        if (def.contains("a cremation chamber in a Nazi concentration camp")) {
            def = "a small furnace or kiln.";
        }
    }

    private void translateActivity(int language, String type, String definition, String example, int i, List<DefinitionRowItem> list) {
        // Translate for toggle option
        String[] transInfo = new String[3]; // Translating to show the definition in output language to learn
        TranslateBackObjectAsyncTask translateInfo = new TranslateBackObjectAsyncTask(getApplicationContext(), language);
        try {
            transInfo = translateInfo.execute(type, definition, example).get();
        } catch (InterruptedException | ExecutionException error) {
            error.printStackTrace();
        }

        DefinitionRowItem transItem = new DefinitionRowItem(R.drawable.objects_detection, i + 1 + ". " + transInfo[0], transInfo[1], transInfo[2]);
        list.add(transItem);
    }

    /* Get number of words in title from: https://www.javatpoint.com/java-program-to-count-the-number-of-words-in-a-string */
    protected int wordCount(String string) {
        int count = 0;

        char[] ch = new char[string.length()];
        for (int i = 0; i < string.length(); i++) {
            ch[i]= string.charAt(i);
            if (((i > 0 ) && (ch[i] != ' ') && (ch[i - 1] == ' ')) || ((ch[0] != ' ') && (i == 0)))
                count++;
        }
        return count;
    }
}