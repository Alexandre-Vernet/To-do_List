package com.ynov.vernet.to_dolist;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    FloatingActionButton floatingActionButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        floatingActionButton = findViewById(R.id.floatingActionButton);

        // Afficher la liste des tâches
        ArrayAdapter<String> tableau = new ArrayAdapter<>(listView.getContext(), R.layout.element, R.id.textViewTache);
        for (int i = 0; i < 40; i++) {
            tableau.add("coucou " + i);
        }
        listView.setAdapter(tableau);

        floatingActionButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), AjoutTacheActivity.class));
            finish();
        });
    }
}