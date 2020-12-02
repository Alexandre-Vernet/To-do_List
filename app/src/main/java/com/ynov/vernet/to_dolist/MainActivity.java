package com.ynov.vernet.to_dolist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    FloatingActionButton floatingActionButton;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        listView = findViewById(R.id.listView);
        floatingActionButton = findViewById(R.id.floatingActionButton);

        db.collection("taches")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Afficher les tâches
                        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(listView.getContext(), R.layout.element, R.id.textViewTache);
                        for (QueryDocumentSnapshot document : task.getResult())
                            arrayAdapter.add(document.getData().toString());

                        listView.setAdapter(arrayAdapter);


                        // Erreur dans la récupération des tâches
                    } else {
                        Toast.makeText(this, "Erreur dans la récupération des tâches", Toast.LENGTH_SHORT).show();
                        Log.w("Erreur", "Erreur dans la récupération des tâches", task.getException());
                    }
                });


        // Ajouter une tâche
        floatingActionButton.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), AjoutTacheActivity.class));
            finish();
        });
    }
}