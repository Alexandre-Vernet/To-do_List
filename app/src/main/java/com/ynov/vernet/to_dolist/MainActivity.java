package com.ynov.vernet.to_dolist;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
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

import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    ProgressBar progressBar;
    TextView textViewAucuneTacheEnCours;
    ListView listView;
    FloatingActionButton floatingActionButtonAjoutTache;
    FirebaseFirestore db;

    Runnable runnable;
    private static final String TAG = "MainActivity";

    Vibrator vibe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);
        textViewAucuneTacheEnCours = findViewById(R.id.textViewAucuneTacheEnCours);
        listView = findViewById(R.id.listView);
        floatingActionButtonAjoutTache = findViewById(R.id.floatingActionButton);



        // Afficher les tâches en cours
        Handler handler = new Handler();
        runnable = () -> db.collection("taches")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        progressBar.setVisibility(View.INVISIBLE);

                        // Si aucune tâches n'est présente
                        if (Objects.requireNonNull(task.getResult()).isEmpty())
                            textViewAucuneTacheEnCours.setVisibility(View.VISIBLE);

                        else {
                            textViewAucuneTacheEnCours.setVisibility(View.INVISIBLE);

                            // Récupérer les tâches
                            ArrayList<String> tache = new ArrayList<>();
                            for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult()))
                                tache.add(Objects.requireNonNull(document.get("Description")).toString());

                            // Afficher les tâches dans la listView
                            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(listView.getContext(), android.R.layout.select_dialog_multichoice, tache);
                            listView.setAdapter(arrayAdapter);
                        }


                        // Erreur dans la récupération des tâches
                    } else {
                        Snackbar.make(findViewById(R.id.floatingActionButton), "Erreur dans la récupération des tâches \n" + task.getException(), Snackbar.LENGTH_LONG)
                                .setAction("Rééssayer", v -> handler.postDelayed(runnable, 0))
                                .show();
                        Log.w(TAG, "Erreur dans la récupération des tâches : ", task.getException());
                    }
                });
        handler.postDelayed(runnable, 0);


        // Au clic d'une tâche
        listView.setOnItemClickListener((parent, view, position, id) -> {

            // Récupérer son contenu
            String tache = (String) listView.getItemAtPosition(position);

            // La supprimer
            db.collection("taches").document(tache)
                    .delete()

                    // Tâche supprimée
                    .addOnSuccessListener(aVoid -> {
                        Snackbar.make(findViewById(R.id.floatingActionButton), "Tâche supprimée", Snackbar.LENGTH_LONG)
                                .show();
                        Log.d(TAG, "Tâche supprimée");
                        handler.postDelayed(runnable, 0);
                    })

                    // Erreur dans la suppression de la tâche
                    .addOnFailureListener(e -> {
                        Snackbar.make(findViewById(R.id.floatingActionButton), "Erreur lors de la suppression de la tâche " + e, Snackbar.LENGTH_LONG)
                                .show();
                        Log.w(TAG, "Erreur lors de la suppression de la tâche : " + e);
                    });
        });


        // Appui long sur une tâche
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            // Copier la tache
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("tâche", listView.getItemAtPosition(position).toString());
            assert clipboard != null;
            clipboard.setPrimaryClip(clip);

            Toast.makeText(MainActivity.this, "Texte copié !", Toast.LENGTH_SHORT).show();

            // Vibrer
            vibe = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
            vibe.vibrate(80);

            return false;
        });


        // Ajouter une tâche
        floatingActionButtonAjoutTache.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), AjoutTacheActivity.class));
            finish();
        });
    }
}