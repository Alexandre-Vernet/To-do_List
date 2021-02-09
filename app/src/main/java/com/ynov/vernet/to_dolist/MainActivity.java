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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

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
        textViewNoCurrentTask = findViewById(R.id.textViewNoCurrentTask);
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
                                        .setAction(R.string.reessayer, v -> handler.postDelayed(runnable, 0))
                                        .show();
                                Log.w(TAG, getString(R.string.erreur_recup_taches) + task.getException());
                            }
                        });

        // Update view
        handler.postDelayed(runnable, 0);

        // Listen tasks
        Query query = db.collection(room);
        query.addSnapshotListener(
                (value, error) -> handler.postDelayed(runnable, 0));


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
                        // Restore task with snackbar
                        Snackbar.make(findViewById(R.id.test), (R.string.tache_supprimee), Snackbar.LENGTH_LONG)
                                .setAction(R.string.annuler, v -> {

                                    // Restore content
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("Description", task);
                                    Date date = Calendar.getInstance().getTime();
                                    map.put("date", date);

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
                                                        .setAction(getString(R.string.reessayer), error -> handler.postDelayed(runnable, 0))
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
                    .setMessage("Created by Alex")
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
                                    Snackbar.make(findViewById(R.id.btnValidate), (getString(R.string.erreur_ajout_tache)) + e, Snackbar.LENGTH_LONG)
                                            .setAction(getString(R.string.reessayer), error -> handler.postDelayed(runnable, 0))
                                            .show();
                                    Log.w(TAG, (getString(R.string.erreur_ajout_tache)) + e);
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            // Update view
            handler.postDelayed(runnable, 0);

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