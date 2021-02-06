package com.ynov.vernet.to_dolist;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    ProgressBar progressBar;
    TextView textViewAucuneTacheEnCours, textViewNbTaches, textViewSalon;
    ListView listView;
    FloatingActionButton floatingActionButtonAjoutTache;
    FirebaseFirestore db;

    private Runnable runnable;

    private int nbTaches = 0;
    String salon;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);
        textViewAucuneTacheEnCours = findViewById(R.id.textViewAucuneTacheEnCours);
        textViewNbTaches = findViewById(R.id.textViewNbTaches);
        textViewSalon = findViewById(R.id.textViewSalon);
        listView = findViewById(R.id.listView);
        floatingActionButtonAjoutTache = findViewById(R.id.floatingActionButton);


        // Check Internet connexion
        boolean internet = new Internet(this, this).internet();
        if (!internet) {
            floatingActionButtonAjoutTache.setVisibility(View.INVISIBLE);

            // Display message
            Snackbar.make(findViewById(R.id.test), R.string.internet_indisponible, Snackbar.LENGTH_LONG)
                    .setAction(R.string.activer, v -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)))
                    .show();
        }

        // Vérifier si l'utilisateur possède déjà un salon
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        salon = sharedPref.getString("salon", null);

        // S'il n'a pas de salon
        if (salon == null) {
            // Générer un code de salon
            String characters = "1234567890AZERTYUIOPQSDFGHJKLMWXCVBN";
            final Random random = new Random();
            final StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; ++i)
                sb.append(characters.charAt(random.nextInt(characters.length())));
            salon = sb.toString();

            // Sauver le code dans la mémoire
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("salon", salon);
            editor.apply();
        }

        // Afficher le numéro de salon
        textViewSalon.setText(salon);

        // Afficher les tâches en cours
        Handler handler = new Handler();
        runnable = () ->
                db.collection(salon)
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
                                    textViewNbTaches.setVisibility(View.VISIBLE);
                                    textViewAucuneTacheEnCours.setVisibility(View.INVISIBLE);
                                    listView.setVisibility(View.VISIBLE);

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


        // Ecouter les tâches entrantes
        Query query = db.collection(salon);
        query.addSnapshotListener(
                (value, error) -> handler.postDelayed(runnable, 0));

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
                                    Date date = Calendar.getInstance().getTime();
                                    taches.put("date", date);

                                    // Ajouter la tâche saisie à la BDD
                                    db.collection(salon)
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
            Vibrator vibe = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
            assert vibe != null;
            vibe.vibrate(80);

            return true;
        });


        // Ajouter une tâche
        floatingActionButtonAjoutTache.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), AjoutTacheActivity.class);
            intent.putExtra("salon", salon);
            startActivity(intent);
            finish();
        });
    }
}