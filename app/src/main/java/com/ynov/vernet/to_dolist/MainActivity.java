package com.ynov.vernet.to_dolist;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
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
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Context context;
    ProgressBar progressBar;
    TextView textViewNoCurrentTask, textViewCountTaches, textViewRoom;
    SearchView searchView;
    ListView listView;
    FirebaseFirestore db;

    private Runnable runnable;

    private int countTask = 0;

    ArrayList<String> arrayListId;
    ArrayList<String> arrayListTask;
    ArrayList<String> arrayListName;
    ArrayList<Date> arrayListDate;
    ArrayAdapter<String> arrayAdapter;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        context = getApplicationContext();
        progressBar = findViewById(R.id.progressBar);
        textViewNoCurrentTask = findViewById(R.id.textViewNoCurrentTask);
        textViewCountTaches = findViewById(R.id.textViewCountTaches);
        textViewRoom = findViewById(R.id.textViewRoom);
        searchView = findViewById(R.id.searchView);
        listView = findViewById(R.id.listView);

        db = FirebaseFirestore.getInstance();


        // Check Internet connexion
        boolean internet = new Internet(this, this).internet();
        if (!internet) {
            // Display message
            Snackbar.make(findViewById(R.id.relativeLayout), R.string.internet_indisponible, Snackbar.LENGTH_LONG)
                    .setAction(R.string.activer, v -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)))
                    .show();
        }

        // Get name
        String name = new SettingsActivity().getName(this, this);
        Log.d(TAG, "onCreate: " + name);

        // Get room
        String room = new SettingsActivity().getRoom(this, this);

        // Menu
        new Menu(this, this);

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

                                    // Get all data in ArrayList
                                    arrayListId = new ArrayList<>();
                                    arrayListTask = new ArrayList<>();
                                    arrayListName = new ArrayList<>();
                                    arrayListDate = new ArrayList<>();


                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        arrayListId.add(document.getId());
                                        arrayListTask.add(document.get("description").toString());
                                        arrayListName.add(document.get("user").toString());
                                        arrayListDate.add(document.getTimestamp("date").toDate());
                                        countTask++;
                                    }


                                    // Display count of current tasks
                                    if (countTask <= 1)
                                        textViewCountTaches.setText(getString(R.string.nb_tache_en_cours, countTask));
                                    else
                                        textViewCountTaches.setText(getString(R.string.nb_taches_en_cours, countTask));

                                    // Display tasks in ListView
                                    arrayAdapter = new ArrayAdapter<>(listView.getContext(), android.R.layout.select_dialog_multichoice, arrayListTask);
                                    listView.setAdapter(arrayAdapter);
                                }

                                // Error while getting tasks
                            } else {
                                Snackbar.make(findViewById(R.id.relativeLayout), (getString(R.string.erreur_recup_taches) + task.getException()), Snackbar.LENGTH_LONG)
                                        .setAction(R.string.reessayer, v -> {
                                        })
                                        .show();
                                Log.w(TAG, getString(R.string.erreur_recup_taches) + task.getException());
                            }
                        });


        // Display room code
        textViewRoom.setText(room);

        // Search ba
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                arrayAdapter.getFilter().filter(query);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                arrayAdapter.getFilter().filter(newText);

                return false;
            }
        });


        // Click task
        listView.setOnItemClickListener((parent, view, position, id) -> {

            // Refresh view
            handler.postDelayed(runnable, 0);

            // Get taskId
            String taskId = arrayListId.get(position);

            // Get content of the task
            String task = (String) listView.getItemAtPosition(position);

            // Delete the task in database
            db.collection(room)
                    .document(taskId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Restore task with Snackbar
                        Snackbar.make(findViewById(R.id.relativeLayout), (R.string.tache_supprimee), Snackbar.LENGTH_LONG)
                                .setAction(R.string.annuler, v -> {

                                    // Restore content
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("description", task);

                                    Date date = Calendar.getInstance().getTime();
                                    map.put("date", date);

                                    map.put("user", name);

                                    // Add task to database
                                    db.collection(room)
                                            .add(map)
                                            .addOnSuccessListener(documentReference -> {
                                                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                                                finish();
                                            })

                                            // Error adding task
                                            .addOnFailureListener(e -> {
                                                Snackbar.make(findViewById(R.id.relativeLayout), (getString(R.string.erreur_ajout_tache)) + e, Snackbar.LENGTH_LONG)
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
                        Snackbar.make(findViewById(R.id.relativeLayout), (getString(R.string.erreur_suppression_tache)) + e, Snackbar.LENGTH_LONG)
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

            // Get taskId
            String taskId = arrayListId.get(position);

            // Get the name of the creator
            String taskWrittenBy = arrayListName.get(position);

            // Get date of creation
            Date taskCreatedAt = arrayListDate.get(position);
            String taskDate = new SimpleDateFormat("d/MM/y", Locale.getDefault()).format(taskCreatedAt);
            String taskHour = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(taskCreatedAt);

            // Get task description
            String taskDescription = (String) listView.getItemAtPosition(position);

            // Keyboard
            EditText editText = new EditText(this);
            editText.setText(taskDescription);

            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.edit_task)
                    .setTitle("Edit task")
                    .setMessage("Created by " + taskWrittenBy + "\nThe " + taskDate + " at " + taskHour)
                    .setView(editText)
                    .setNeutralButton("Copy", (dialog, which) -> {
                        // Copy task
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("task", taskDescription);
                        clipboard.setPrimaryClip(clip);

                        // Vibrate
                        long[] pattern = {0, 100};
                        vibe.vibrate(pattern, -1);

                        // Display Toast
                        Toast.makeText(context, "Task copied !", Toast.LENGTH_SHORT).show();
                    })
                    .setPositiveButton("Save", (dialogInterface, i) -> {

                        // Get edited task
                        String editedTask = editText.getText().toString();

                        Map<String, Object> map = new HashMap<>();
                        map.put("description", editedTask);

                        // Update database
                        db.collection(room)
                                .document(taskId)
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


        // Listen tasks
        Query query = db.collection(room);
        query.addSnapshotListener(
                (value, error) -> handler.postDelayed(runnable, 0));
    }
}