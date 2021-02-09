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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class AddTaskActivity extends AppCompatActivity {

    FirebaseFirestore db;
    EditText editTextTask;
    ProgressBar progressBar;
    Runnable runnable;
    private static final String TAG = "AddTaskActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_task);

        db = FirebaseFirestore.getInstance();
        editTextTask = findViewById(R.id.editTextTask);
        progressBar = findViewById(R.id.progressBar);

        // Open keyboard
        editTextTask.requestFocus();
        editTextTask.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        // Get room
        String room = getIntent().getStringExtra("room");

        // Add a task
        Handler handler = new Handler();
        runnable = () -> findViewById(R.id.btnValidate).setOnClickListener(v -> {
            if (!editTextTask.getText().toString().isEmpty()) {

                progressBar.setVisibility(View.VISIBLE);

                // Get entered task
                Map<String, Object> map = new HashMap<>();
                map.put("Description", editTextTask.getText().toString());

                // Add username
                map.put("Utilisateur", "Alex");

                // Add date
                Date date = Calendar.getInstance().getTime();
                map.put("date", date);

                String id = "0001";

                // Add to database
                db.collection(room)
                        .document(editTextTask.getText().toString())
                        .set(map)
                        .addOnSuccessListener(documentReference -> {
                            startActivity(new Intent(getApplicationContext(), MainActivity.class));
                            finish();
                        })

                        // Error adding database
                        .addOnFailureListener(e -> {
                            Snackbar.make(findViewById(R.id.btnValidate), (getString(R.string.erreur_ajout_tache)) + e, Snackbar.LENGTH_LONG)
                                    .setAction(getString(R.string.reessayer), erreur -> handler.postDelayed(runnable, 0))
                                    .show();
                            Log.w(TAG, (getString(R.string.erreur_ajout_tache)) + e);
                        });

                // Empty text box
            } else {
                editTextTask.setError(getString(R.string.zone_txt_ne_peut_pas_etre_vide));
                new Handler().postDelayed(() -> editTextTask.setError(null), 1000);
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