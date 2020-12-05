package com.ynov.vernet.to_dolist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    ProgressBar progressBar;
    TextView textViewAucuneTacheEnCours;
    ListView listView;
    FloatingActionButton floatingActionButtonAjoutTache, floatingActionButtonSupprimerTache;
    FirebaseFirestore db;

    Runnable runnable;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);
        textViewAucuneTacheEnCours = findViewById(R.id.textViewAucuneTacheEnCours);
        listView = findViewById(R.id.listView);
        floatingActionButtonAjoutTache = findViewById(R.id.floatingActionButton);
        floatingActionButtonSupprimerTache = findViewById(R.id.floatingActionButtonSupprimerTache);


        // Afficher les tâches en cours
        Handler handler = new Handler();
        runnable = () -> db.collection("taches")
                .orderBy("tache", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        progressBar.setVisibility(View.INVISIBLE);

                        // Si aucune tâches n'est présente
                        if (Objects.requireNonNull(task.getResult()).isEmpty())
                            textViewAucuneTacheEnCours.setVisibility(View.VISIBLE);

                        else {
                            textViewAucuneTacheEnCours.setVisibility(View.INVISIBLE);

                            // Afficher les tâches dans la listView
                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(listView.getContext(), R.layout.element, R.id.checkboxTache);
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                arrayAdapter.add(Objects.requireNonNull(document.get("tache")).toString());
                            }

                            listView.setAdapter(arrayAdapter);
                        }


                        // Erreur dans la récupération des tâches
                    } else {
                        Snackbar.make(findViewById(R.id.floatingActionButton), "Erreur dans la récupération des tâches \n" + task.getException(), Snackbar.LENGTH_LONG)
                                .setAction("Rééssayer", v -> handler.postDelayed(runnable, 0))
                                .show();
                        Log.w(TAG, "Erreur dans la récupération des tâches :", task.getException());
                    }
                });
        handler.postDelayed(runnable, 0);


        // Au clic d'une tâche
        listView.setOnItemClickListener((parent, view, position, id) -> {
            // Afficher un toast avec le nom du contact
            String tache = (String) listView.getItemAtPosition(position);
            Toast.makeText(MainActivity.this, tache, Toast.LENGTH_SHORT).show();
        });


        // Ajouter une tâche
        floatingActionButtonAjoutTache.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), AjoutTacheActivity.class));
            finish();
        });


        // Supprimer une tâche manuellement
        floatingActionButtonSupprimerTache.setOnClickListener(v -> {
            db.collection("taches").document("pRcZ3jFJpW79swybthJ4")
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Snackbar.make(findViewById(R.id.floatingActionButton), "Tâche supprimée", Snackbar.LENGTH_LONG)
                                .show();
                        Log.d(TAG, "DocumentSnapshot successfully deleted!");
                        handler.postDelayed(runnable, 0);
                    })
                    .addOnFailureListener(e -> {
                        Snackbar.make(findViewById(R.id.floatingActionButton), "Erreur lors de la suppression de la tâche " + e, Snackbar.LENGTH_LONG)
                                .show();
                        Log.w(TAG, "Erreur lors de la suppression de la tâche ", e);
                    });
        });
    }
}