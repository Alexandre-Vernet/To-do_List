package com.ynov.vernet.to_dolist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AjoutTacheActivity extends AppCompatActivity {

    FirebaseFirestore db;
    EditText editTextTache;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajout_tache);

        db = FirebaseFirestore.getInstance();
        editTextTache = findViewById(R.id.editTextTache);
        progressBar = findViewById(R.id.progressBar);

        editTextTache.requestFocus();
        editTextTache.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        findViewById(R.id.btnValider).setOnClickListener(v -> {
            if (!editTextTache.getText().toString().isEmpty()) {

                progressBar.setVisibility(View.VISIBLE);

                // Ajouter la tâche saisie à la BDD
                Map<String, Object> tache = new HashMap<>();
                tache.put("tache", editTextTache.getText().toString());

                db.collection("taches")
                        .add(tache)
                        .addOnSuccessListener(documentReference -> {
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            finish();
                            Log.d("Ajout", "Tâche " + documentReference.getId() + " ajoutée");
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AjoutTacheActivity.this, "Erreur lors de l'ajout de la tâche \n" + e, Toast.LENGTH_SHORT).show();
                            Log.w("Erreur", "Erreur lors de l'ajout de la tâche", e);
                        });

            } else {
                editTextTache.setError("La zone de texte ne peut pas être vide");
                new Handler().postDelayed(() -> editTextTache.setError(null), 1000);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}