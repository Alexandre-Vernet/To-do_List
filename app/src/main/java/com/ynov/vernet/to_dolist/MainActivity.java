package com.ynov.vernet.to_dolist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    ProgressBar progressBar;
    ListView listView;
    FloatingActionButton floatingActionButton;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listView);
        floatingActionButton = findViewById(R.id.floatingActionButton);

        db.collection("taches")
                .orderBy("tache", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        progressBar.setVisibility(View.INVISIBLE);

                        // Afficher les tâches
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(listView.getContext(), R.layout.element, R.id.textViewTache);
                        for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult()))
                            arrayAdapter.add(document.get("tache").toString());

                        listView.setAdapter(arrayAdapter);


                        // Erreur dans la récupération des tâches
                    } else {
                        Toast.makeText(this, "Erreur dans la récupération des tâches", Toast.LENGTH_SHORT).show();
                        Log.w("Erreur", "Erreur dans la récupération des tâches", task.getException());
                    }
                });

        // Au clic d'un élément
        listView.setOnItemClickListener((parent, view, position, id) -> {
            // Afficher un toast avec le nom du contact
            String tache = (String) listView.getItemAtPosition(position);
            Toast.makeText(MainActivity.this, tache, Toast.LENGTH_SHORT).show();
        });


        // Ajouter une tâche
        floatingActionButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), AjoutTacheActivity.class));
            finish();
        });
    }
}