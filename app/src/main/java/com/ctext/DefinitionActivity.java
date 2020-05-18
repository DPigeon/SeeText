package com.ctext;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.ctext.objectdetection.ObjectDefinitionAsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

public class DefinitionActivity extends AppCompatActivity {
    private String TAG = "DefinitionActivity";
    private String word = "";
    private TextView pronunciationTextView;
    private ListView definitionsListView;
    private ArrayAdapter adapter;
    private ArrayList<String> definitionsString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_definition);

        setupUI();
    }

    protected void setupUI() {
        Intent intent = getIntent();
        word = intent.getStringExtra("word");
        getSupportActionBar().setTitle("Meaning of " + word);

        pronunciationTextView = findViewById(R.id.pronunciationTextView);
        definitionsListView = findViewById(R.id.definitionsListView);
        definitionsString = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, definitionsString);
        definitionsListView.setAdapter(adapter);

        fetchDefinitions();
    }

    protected void fetchDefinitions() {
        ObjectDefinitionAsyncTask objectDefinitionAsyncTask = new ObjectDefinitionAsyncTask();
        try {
            JSONObject json = objectDefinitionAsyncTask.execute(word).get();
            if (json != null) {
                String pronunciation = json.getString("pronunciation");
                pronunciationTextView.setText(pronunciation);
                JSONArray definitions = json.getJSONArray("definitions");
                for (int i = 0; i < definitions.length(); i++) {
                    JSONObject definition = definitions.getJSONObject(i);
                    // Need type, definition & example
                    String type = definition.getString("type");
                    String def = definition.getString("definition");
                    String example = definition.getString("example");

                    String item = "   " + type + "\n"
                            + "  Definition: " + def + "\n"
                            + "  Example: '" + example + "'" + "\n";
                    definitionsString.add(item);
                }
            }
        } catch (InterruptedException | ExecutionException | JSONException error) {
            error.printStackTrace();
        }

    }

}
