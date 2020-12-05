package com.ynov.vernet.to_dolist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AjoutTacheActivity extends AppCompatActivity {

    FirebaseFirestore db;
    EditText editTextTache;
    ProgressBar progressBar;

    Runnable runnable;
    private static final String TAG = "AjoutTacheActivity";
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajout_tache);

        db = FirebaseFirestore.getInstance();
        editTextTache = findViewById(R.id.editTextTache);
        progressBar = findViewById(R.id.progressBar);

        editTextTache.requestFocus();
        editTextTache.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);


        // Ajouter une tâche
        Handler handler = new Handler();
        runnable = () -> findViewById(R.id.btnValider).setOnClickListener(v -> {
            if (!editTextTache.getText().toString().isEmpty()) {

                progressBar.setVisibility(View.VISIBLE);

                // Récupérer la tâche saisie
                Map<String, Object> tache = new HashMap<>();
                tache.put("Description", editTextTache.getText().toString());

                // Ajouter la tâche saisie à la BDD
                db.collection("taches")
                        .document(editTextTache.getText().toString())
                        .set(tache)
                        .addOnSuccessListener(documentReference -> {
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            finish();
                        })

                        // Erreur dans l'ajout de la tâche à la BDD
                        .addOnFailureListener(e -> {
                            Snackbar.make(findViewById(R.id.btnValider), "Erreur lors de l'ajout de la tâche \n" + e, Snackbar.LENGTH_LONG)
                                    .setAction("Rééessayer", erreur -> handler.postDelayed(runnable, 0))
                                    .show();
                            Log.w(TAG, "Erreur lors de l'ajout de la tâche : " + e);
                        });

                // Zone de texte vide
            } else {
                editTextTache.setError("La zone de texte ne peut pas être vide");
                new Handler().postDelayed(() -> editTextTache.setError(null), 1000);
            }
        });
        handler.postDelayed(runnable, 0);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}