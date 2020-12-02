package com.ynov.vernet.to_dolist;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AjoutTacheActivity extends AppCompatActivity {

    EditText editTextTache;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajout_tache);

        db = FirebaseFirestore.getInstance();
        editTextTache = findViewById(R.id.editTextTache);

        findViewById(R.id.btnValider).setOnClickListener(v -> {
            // Ajouter la tâche saisie à la BDD
            Map<String, Object> user = new HashMap<>();
            user.put("tache", editTextTache.getText().toString());

            db.collection("taches")
                    .add(user)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            finish();
                            Log.d("Ajout", "Tâche " + documentReference.getId() + " ajoutée");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w("Erreur", "Erreur lors de l'ajout de la tâche", e);
                        }
                    });

        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}