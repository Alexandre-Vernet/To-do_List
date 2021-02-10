package com.ynov.vernet.to_dolist;

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
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

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
    TextView textViewNoCurrentTask, textViewCountTaches, textViewRoom;
    ListView listView;
    FloatingActionButton floatingActionButtonAddTask, floatingActionButtonEditRoom;
    FirebaseFirestore db;

    private Runnable runnable;

    private int countTask = 0;

    String room;
    String name;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        progressBar = findViewById(R.id.progressBar);
        textViewNoCurrentTask = findViewById(R.id.textViewNoCurrentTask);
        textViewCountTaches = findViewById(R.id.textViewCountTaches);
        textViewRoom = findViewById(R.id.textViewRoom);
        listView = findViewById(R.id.listView);
        floatingActionButtonAddTask = findViewById(R.id.floatingActionButtonAddTask);
        floatingActionButtonEditRoom = findViewById(R.id.floatingActionButtonEditRoom);


        // Check Internet connexion
        boolean internet = new Internet(this, this).internet();
        if (!internet) {
            floatingActionButtonAddTask.setVisibility(View.INVISIBLE);

            // Display message
            Snackbar.make(findViewById(R.id.relativeLayout), R.string.internet_indisponible, Snackbar.LENGTH_LONG)
                    .setAction(R.string.activer, v -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)))
                    .show();
        }

        // Check if user entered his name
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        name = sharedPreferences.getString("name", null);

        // if user has no name
        if (name == null) {
            EditText editText = new EditText(this);
            editText.setHint("Your name");

            // Ask him
            AlertDialog alertDialog = new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setTitle("Welcome")
                    .setMessage("What's your name ?")
                    .setView(editText)
                    .setPositiveButton("OK", (dialogInterface, i) -> {

                        // Get the name from EditText
                        name = editText.getText().toString();

                        // Save user's name
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                        editor.putString("name", name);
                        editor.apply();
                    })
                    .show();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.setCancelable(false);
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
                                    textViewNoCurrentTask.setVisibility(View.VISIBLE);
                                    listView.setVisibility(View.INVISIBLE);
                                } else {
                                    textViewCountTaches.setVisibility(View.VISIBLE);
                                    textViewNoCurrentTask.setVisibility(View.INVISIBLE);
                                    listView.setVisibility(View.VISIBLE);

                                    // Get all tasks
                                    ArrayList<String> arrayList = new ArrayList<>();
                                    for (QueryDocumentSnapshot document : Objects.requireNonNull(task.getResult())) {
                                        arrayList.add(Objects.requireNonNull(document.get("Description")).toString());
                                        countTask++;
                                    }

                                    // Display count of current tasks
                                    if (countTask <= 1)
                                        textViewCountTaches.setText(getString(R.string.nb_tache_en_cours, countTask));
                                    else
                                        textViewCountTaches.setText(getString(R.string.nb_taches_en_cours, countTask));

                                    // Display tasks in ListView
                                    ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(listView.getContext(), android.R.layout.select_dialog_multichoice, arrayList);
                                    listView.setAdapter(arrayAdapter);
                                }

                                // Error while getting tasks
                            } else {
                                Snackbar.make(findViewById(R.id.floatingActionButtonAddTask), (getString(R.string.erreur_recup_taches) + task.getException()), Snackbar.LENGTH_LONG)
                                        .setAction(R.string.reessayer, v -> {
                                        })
                                        .show();
                                Log.w(TAG, getString(R.string.erreur_recup_taches) + task.getException());
                            }
                        });

        // Click task
        listView.setOnItemClickListener((parent, view, position, id) -> {

            // Remove 1 task in count
            countTask--;
            textViewCountTaches.setText(getString(R.string.nb_taches_en_cours, countTask));

            // Get content of the task
            String task = (String) listView.getItemAtPosition(position);

            // Delete the task in database
            db.collection(room)
                    .document(task)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Restore task with Snackbar
                        Snackbar.make(findViewById(R.id.relativeLayout), (R.string.tache_supprimee), Snackbar.LENGTH_LONG)
                                .setAction(R.string.annuler, v -> {

                                    // Restore content
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("Description", task);

                                    Date date = Calendar.getInstance().getTime();
                                    map.put("date", date);

                                    map.put("Utilisateur", name);

                                    // Add task to database
                                    db.collection(room)
                                            .document(task)
                                            .set(map)
                                            .addOnSuccessListener(documentReference -> {
                                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                                finish();
                                            })

                                            // Error adding task
                                            .addOnFailureListener(e -> {
                                                Snackbar.make(findViewById(R.id.floatingActionButtonAddTask), (getString(R.string.erreur_ajout_tache)) + e, Snackbar.LENGTH_LONG)
                                                        .setAction(getString(R.string.reessayer), error -> {
                                                        })
                                                        .show();
                                                Log.w(TAG, (getString(R.string.erreur_ajout_tache)) + e);
                                            });
                                })
                                .show();

                        Log.d(TAG, getString(R.string.tache_supprimee));
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

            // Vibrate
            Vibrator vibe = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
            assert vibe != null;
            vibe.vibrate(80);

            // Get task
            String task = (String) listView.getItemAtPosition(position);

            // Keybord
            EditText editText = new EditText(this);
            editText.setText(task);

            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle("Edit task")
                    .setMessage("Created by " + name)
                    .setView(editText)
                    .setPositiveButton("Save", (dialogInterface, i) -> {

                        // Get edited task
                        String edittedTask = editText.getText().toString();

                        Map<String, Object> map = new HashMap<>();
                        map.put("Description", edittedTask);

                        // Update database
                        db.collection(room)
                                .document(task)
                                .update(map)
                                .addOnSuccessListener(documentReference -> {
                                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                    finish();
                                })

                                // Error updating database
                                .addOnFailureListener(e -> {
                                    Snackbar.make(findViewById(R.id.relativeLayout), (getString(R.string.erreur_ajout_tache)) + e, Snackbar.LENGTH_LONG)
                                            .setAction(getString(R.string.reessayer), error -> {
                                            })
                                            .show();
                                    Log.w(TAG, (getString(R.string.erreur_ajout_tache)) + e);
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            return true;
        });


        // Add a task
        floatingActionButtonAddTask.setOnClickListener(v -> {
            EditText editText = new EditText(this);
            editText.setHint("Add some text here");

            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.ajouter_tache)
                    .setTitle("Add a task")
                    .setView(editText)
                    .setPositiveButton("Add", (dialogInterface, i) -> {

                        // Get entered task
                        String task = editText.getText().toString();

                        // Add task
                        Map<String, Object> map = new HashMap<>();
                        map.put("Description", task);

                        // Add username
                        map.put("Utilisateur", name);

                        // Add date
                        Date date = Calendar.getInstance().getTime();
                        map.put("date", date);

                        // Add all to database
                        db.collection(room)
                                .document(task)
                                .set(map)
                                .addOnSuccessListener(documentReference -> {
                                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                    finish();
                                })

                                // Error adding database
                                .addOnFailureListener(e -> {
                                    Snackbar.make(findViewById(R.id.relativeLayout), (getString(R.string.erreur_ajout_tache)) + e, Snackbar.LENGTH_LONG)
                                            .setAction(getString(R.string.reessayer), erreur -> {
                                            })
                                            .show();
                                    Log.w(TAG, (getString(R.string.erreur_ajout_tache)) + e);
                                });

                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });


        // Edit room
        floatingActionButtonEditRoom.setOnClickListener(v -> {

        });


        // Listen tasks
        Query query = db.collection(room);
        query.addSnapshotListener(
                (value, error) -> handler.postDelayed(runnable, 0));

    }
}