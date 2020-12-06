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
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    ProgressBar progressBar;
    TextView textViewAucuneTacheEnCours, textViewNbTaches;
    ListView listView;
    FloatingActionButton floatingActionButtonAjoutTache;
    FirebaseFirestore db;

    private Runnable runnable;
    private static final String TAG = "MainActivity";
    private Vibrator vibe;
    private int nbTaches = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);
        textViewAucuneTacheEnCours = findViewById(R.id.textViewAucuneTacheEnCours);
        textViewNbTaches = findViewById(R.id.textViewNbTaches);
        listView = findViewById(R.id.listView);
        floatingActionButtonAjoutTache = findViewById(R.id.floatingActionButton);


        // Afficher les tâches en cours
        Handler handler = new Handler();
        runnable = () ->
                db.collection("taches")
                        .orderBy("date", Query.Direction.DESCENDING)
                        .get()
                        .addOnCompleteListener(task -> {
                            nbTaches = 0;
                            if (task.isSuccessful()) {
                                progressBar.setVisibility(View.INVISIBLE);

                                // Si aucune tâches n'est présente
                                if (Objects.requireNonNull(task.getResult()).isEmpty()) {
                                    textViewNbTaches.setVisibility(View.INVISIBLE);
                                    textViewAucuneTacheEnCours.setVisibility(View.VISIBLE);
                                    listView.setVisibility(View.INVISIBLE);
                                } else {
                                    textViewAucuneTacheEnCours.setVisibility(View.INVISIBLE);

                                    // Récupérer les tâches
                                    ArrayList<String> tache = new ArrayList<>();
                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                        tache.add(Objects.requireNonNull(document.get("Description")).toString());
                                        nbTaches++;
                                    }

                                    // Afficher le nombre de tâches en cours
                                    if (nbTaches <= 1)
                                        textViewNbTaches.setText(getString(R.string.nb_tache_en_cours, nbTaches));
                                    else
                                        textViewNbTaches.setText(getString(R.string.nb_taches_en_cours, nbTaches));


                                    // Afficher les tâches dans la listView
                                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(listView.getContext(), android.R.layout.select_dialog_multichoice, tache);
                                    listView.setAdapter(arrayAdapter);
                                }


                                // Erreur dans la récupération des tâches
                            } else {
                                Snackbar.make(findViewById(R.id.floatingActionButton), (getString(R.string.erreur_recup_taches) + task.getException()), Snackbar.LENGTH_LONG)
                                        .setAction(R.string.reessayer, v -> handler.postDelayed(runnable, 0))
                                        .show();
                                Log.w(TAG, getString(R.string.erreur_recup_taches) + task.getException());
                            }
                        });
        handler.postDelayed(runnable, 0);


        // Au clic d'une tâche
        listView.setOnItemClickListener((parent, view, position, id) -> {
            nbTaches--;
            textViewNbTaches.setText(getString(R.string.nb_taches_en_cours, nbTaches));

            // Récupérer son contenu
            String tache = (String) listView.getItemAtPosition(position);

            // La supprimer
            db.collection("taches").document(tache)
                    .delete()

                    // Tâche supprimée
                    .addOnSuccessListener(aVoid -> {
                        Snackbar.make(findViewById(R.id.test), (R.string.tache_supprimee), Snackbar.LENGTH_LONG)
                                .setAction(R.string.annuler, v -> {

                                    // Restaurer son contenu
                                    Map<String, Object> taches = new HashMap<>();
                                    taches.put("Description", tache);

                                    // Ajouter la tâche saisie à la BDD
                                    db.collection("taches")
                                            .document(tache)
                                            .set(taches)
                                            .addOnSuccessListener(documentReference -> {
                                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                                finish();
                                            })

                                            // Erreur dans l'ajout de la tâche à la BDD
                                            .addOnFailureListener(e -> {
                                                Snackbar.make(findViewById(R.id.btnValider), (getString(R.string.erreur_ajout_tache)) + e, Snackbar.LENGTH_LONG)
                                                        .setAction(getString(R.string.reessayer), erreur -> handler.postDelayed(runnable, 0))
                                                        .show();
                                                Log.w(TAG, (getString(R.string.erreur_ajout_tache)) + e);
                                            });
                                })
                                .show();

                        Log.d(TAG, getString(R.string.tache_supprimee));

                        // Mettre à jour l'affichage
                        handler.postDelayed(runnable, 0);
                    })

                    // Erreur dans la suppression de la tâche
                    .addOnFailureListener(e -> {
                        Snackbar.make(findViewById(R.id.floatingActionButton), (getString(R.string.erreur_suppression_tache)) + e, Snackbar.LENGTH_LONG)
                                .show();
                        Log.w(TAG, getString(R.string.erreur_suppression_tache) + e);
                    });
        });


        // Appui long sur une tâche
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            // Copier la tache
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("tâche", listView.getItemAtPosition(position).toString());
            assert clipboard != null;
            clipboard.setPrimaryClip(clip);

            Toast.makeText(MainActivity.this, getString(R.string.texte_copie), Toast.LENGTH_SHORT).show();

            // Vibrer
            vibe = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
            assert vibe != null;
            vibe.vibrate(80);

            return true;
        });


        // Ajouter une tâche
        floatingActionButtonAjoutTache.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), AjoutTacheActivity.class));
            finish();
        });
    }
}