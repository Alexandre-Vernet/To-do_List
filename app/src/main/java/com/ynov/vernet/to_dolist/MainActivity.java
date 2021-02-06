package com.ynov.vernet.to_dolist;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
    TextView textViewCountTasks, textViewCountTaches, textViewRoom;
    ListView listView;
    FloatingActionButton floatingActionButtonAddTask;
    FirebaseFirestore db;

    private Runnable runnable;

    private int countTask = 0;
    String room;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);
        textViewCountTasks = findViewById(R.id.textViewCountTasks);
        textViewCountTaches = findViewById(R.id.textViewCountTaches);
        textViewRoom = findViewById(R.id.textViewRoom);
        listView = findViewById(R.id.listView);
        floatingActionButtonAddTask = findViewById(R.id.floatingActionButtonAddTask);


        // Check Internet connexion
        boolean internet = new Internet(this, this).internet();
        if (!internet) {
            floatingActionButtonAddTask.setVisibility(View.INVISIBLE);

            // Display message
            Snackbar.make(findViewById(R.id.test), R.string.internet_indisponible, Snackbar.LENGTH_LONG)
                    .setAction(R.string.activer, v -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)))
                    .show();
        }

        // Check if user had room
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        room = sharedPref.getString("room", null);

        // if no room
        if (room == null) {
            // Generate code room
            String characters = "1234567890AZERTYUIOPQSDFGHJKLMWXCVBN";
            final Random random = new Random();
            final StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; ++i)
                sb.append(characters.charAt(random.nextInt(characters.length())));
            room = sb.toString();

            // Save it in memory
            SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("room", room);
            editor.apply();
        }

        // Display room code
        textViewRoom.setText(room);

        // Display current task
        Handler handler = new Handler();
        runnable = () ->
                db.collection(room)
                        .orderBy("date", Query.Direction.DESCENDING)
                        .get()
                        .addOnCompleteListener(task -> {
                            countTask = 0;
                            if (task.isSuccessful()) {
                                progressBar.setVisibility(View.INVISIBLE);

                                // if no task
                                if (Objects.requireNonNull(task.getResult()).isEmpty()) {
                                    textViewCountTaches.setVisibility(View.INVISIBLE);
                                    textViewCountTasks.setVisibility(View.VISIBLE);
                                    listView.setVisibility(View.INVISIBLE);
                                } else {
                                    textViewCountTaches.setVisibility(View.VISIBLE);
                                    textViewCountTasks.setVisibility(View.INVISIBLE);
                                    listView.setVisibility(View.VISIBLE);

                                    // Get all tasks
                                    ArrayList<String> tache = new ArrayList<>();
                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                        tache.add(Objects.requireNonNull(document.get("Description")).toString());
                                        countTask++;
                                    }

                                    // Display count of current tasks
                                    if (countTask <= 1)
                                        textViewCountTaches.setText(getString(R.string.nb_tache_en_cours, countTask));
                                    else
                                        textViewCountTaches.setText(getString(R.string.nb_taches_en_cours, countTask));

                                    // Display tasks in ListView
                                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(listView.getContext(), android.R.layout.select_dialog_multichoice, tache);
                                    listView.setAdapter(arrayAdapter);
                                }

                                // Error while getting tasks
                            } else {
                                Snackbar.make(findViewById(R.id.floatingActionButtonAddTask), (getString(R.string.erreur_recup_taches) + task.getException()), Snackbar.LENGTH_LONG)
                                        .setAction(R.string.reessayer, v -> handler.postDelayed(runnable, 0))
                                        .show();
                                Log.w(TAG, getString(R.string.erreur_recup_taches) + task.getException());
                            }
                        });
        handler.postDelayed(runnable, 0);


        // Listen tasks
        Query query = db.collection(room);
        query.addSnapshotListener(
                (value, error) -> handler.postDelayed(runnable, 0));

        // Click task
        listView.setOnItemClickListener((parent, view, position, id) -> {

            countTask--;
            textViewCountTaches.setText(getString(R.string.nb_taches_en_cours, countTask));

            // Get content
            String tache = (String) listView.getItemAtPosition(position);

            // Delete it
            db.collection("taches").document(tache)
                    .delete()

                    .addOnSuccessListener(aVoid -> {
                        Snackbar.make(findViewById(R.id.test), (R.string.tache_supprimee), Snackbar.LENGTH_LONG)
                                .setAction(R.string.annuler, v -> {

                                    // Restore content
                                    Map<String, Object> taches = new HashMap<>();
                                    taches.put("Description", tache);
                                    Date date = Calendar.getInstance().getTime();
                                    taches.put("date", date);

                                    // Add task to database
                                    db.collection(room)
                                            .document(tache)
                                            .set(taches)
                                            .addOnSuccessListener(documentReference -> {
                                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                                finish();
                                            })

                                            // Error adding task
                                            .addOnFailureListener(e -> {
                                                Snackbar.make(findViewById(R.id.floatingActionButtonAddTask), (getString(R.string.erreur_ajout_tache)) + e, Snackbar.LENGTH_LONG)
                                                        .setAction(getString(R.string.reessayer), erreur -> handler.postDelayed(runnable, 0))
                                                        .show();
                                                Log.w(TAG, (getString(R.string.erreur_ajout_tache)) + e);
                                            });
                                })
                                .show();

                        Log.d(TAG, getString(R.string.tache_supprimee));

                        // Update view
                        handler.postDelayed(runnable, 0);
                    })

                    // Error deleting task
                    .addOnFailureListener(e -> {
                        Snackbar.make(findViewById(R.id.floatingActionButtonAddTask), (getString(R.string.erreur_suppression_tache)) + e, Snackbar.LENGTH_LONG)
                                .show();
                        Log.w(TAG, getString(R.string.erreur_suppression_tache) + e);
                    });
        });


        // Long press task
        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            // Copy
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("tâche", listView.getItemAtPosition(position).toString());
            assert clipboard != null;
            clipboard.setPrimaryClip(clip);

            Toast.makeText(MainActivity.this, getString(R.string.texte_copie), Toast.LENGTH_SHORT).show();

            // Vibrate
            Vibrator vibe = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
            assert vibe != null;
            vibe.vibrate(80);

            return true;
        });


        // Add a task
        floatingActionButtonAddTask.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), AddTaskActivity.class);
            intent.putExtra("room", room);
            startActivity(intent);
            finish();
        });
    }
}