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

    ProgressBar progressBar;
    TextView textViewNoCurrentTask, textViewCountTask, textViewRoom;
    SearchView searchView;
    ListView listViewTasks;
    FirebaseFirestore db;

    private Runnable runnable;

    private int countTask;

    ArrayList<String> arrayListId;
    ArrayList<String> arrayListDescription;
    ArrayList<String> arrayListName;
    ArrayList<Date> arrayListDate;
    ArrayAdapter<String> arrayAdapter;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = findViewById(R.id.progressBar);
        textViewNoCurrentTask = findViewById(R.id.textViewNoCurrentTask);
        textViewCountTask = findViewById(R.id.textViewCountTask);
        textViewRoom = findViewById(R.id.textViewRoom);
        searchView = findViewById(R.id.searchView);
        listViewTasks = findViewById(R.id.listViewTasks);

        db = FirebaseFirestore.getInstance();

//        startActivity(new Intent(getApplicationContext(), UsersActivity.class));
//        finish();


        // Check Internet connexion
        boolean internet = new Internet(this, this).internet();
        if (!internet) {
            // Display message
            Snackbar.make(findViewById(R.id.relativeLayout), getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG)
                    .setAction(R.string.activate, v -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)))
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
                            if (task.isSuccessful()) {
                                progressBar.setVisibility(View.INVISIBLE);
                                countTask = 0;

                                // if no task
                                if (Objects.requireNonNull(task.getResult()).isEmpty()) {
                                    textViewCountTask.setVisibility(View.INVISIBLE);
                                    textViewNoCurrentTask.setVisibility(View.VISIBLE);
                                    listViewTasks.setVisibility(View.INVISIBLE);
                                } else {
                                    textViewCountTask.setVisibility(View.VISIBLE);
                                    textViewNoCurrentTask.setVisibility(View.INVISIBLE);
                                    listViewTasks.setVisibility(View.VISIBLE);

                                    // Get all data in ArrayList
                                    arrayListId = new ArrayList<>();
                                    arrayListDescription = new ArrayList<>();
                                    arrayListName = new ArrayList<>();
                                    arrayListDate = new ArrayList<>();

                                    // Save data from database
                                    for (QueryDocumentSnapshot document : task.getResult()) {
                                        arrayListId.add(document.getId());
                                        arrayListDescription.add(document.get("description").toString());
                                        arrayListName.add(document.get("user").toString());
                                        arrayListDate.add(document.getTimestamp("date").toDate());
                                        countTask++;
                                    }

                                    // Display count of current tasks
                                    if (countTask <= 1)
                                        textViewCountTask.setText(getString(R.string.current_task, countTask));
                                    else
                                        textViewCountTask.setText(getString(R.string.current_tasks, countTask));

                                    // Display tasks in ListView
                                    arrayAdapter = new ArrayAdapter<>(this, R.layout.tasks, R.id.textViewTask, arrayListDescription);
                                    listViewTasks.setAdapter(arrayAdapter);
                                }

                                // Error while getting tasks
                            } else {
                                error(task.getException(), getString(R.string.error_while_deleting_task));
                            }
                        });


        // Display room code
        textViewRoom.setText(room);

        // Search bar
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
        listViewTasks.setOnItemClickListener((parent, view, position, id) -> {

            // Refresh view
            handler.postDelayed(runnable, 0);

            // Get taskId
            String taskId = arrayListId.get(position);

            // Get content of the task
            String task = (String) listViewTasks.getItemAtPosition(position);

            // Delete the task in database
            db.collection(room)
                    .document(taskId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Restore task with Snackbar
                        Snackbar.make(findViewById(R.id.relativeLayout), getString(R.string.deleted_task), Snackbar.LENGTH_LONG)
                                .setAction(R.string.undo, v -> {

                                    // Restore content
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("description", task);

                                    Date date = Calendar.getInstance().getTime();
                                    map.put("date", date);

                                    map.put("user", name);

                                    // Add task to database
                                    db.collection(room)
                                            .add(map)
                                            .addOnSuccessListener(documentReference -> handler.postDelayed(runnable, 0))

                                            // Error adding task
                                            .addOnFailureListener(e -> error(e, getString(R.string.error_while_adding_task)));
                                })
                                .show();
                    })

                    // Error deleting task
                    .addOnFailureListener(e -> error(e, getString(R.string.error_while_deleting_task)));
        });


        // Long press task
        listViewTasks.setOnItemLongClickListener((parent, view, position, id) -> {

            // Vibrate
            Vibrator vibe = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
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
            String taskDescription = (String) listViewTasks.getItemAtPosition(position);

            // Keyboard
            EditText editText = new EditText(this);
            editText.setText(taskDescription);

            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.edit_task)
                    .setTitle(R.string.edit_task)
                    .setMessage(getString(R.string.created_by) + " " + taskWrittenBy + "\nThe " + taskDate + " at " + taskHour)
                    .setView(editText)
                    .setNeutralButton(R.string.copy, (dialog, which) -> {
                        // Copy task
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("task", taskDescription);
                        clipboard.setPrimaryClip(clip);

                        // Vibrate
                        long[] pattern = {0, 100};
                        vibe.vibrate(pattern, -1);

                        // Display Toast
                        Toast.makeText(this, getString(R.string.task_copied), Toast.LENGTH_SHORT).show();
                    })
                    .setPositiveButton(R.string.save, (dialogInterface, i) -> {

                        // Get edited task
                        String editedTask = editText.getText().toString();

                        Map<String, Object> map = new HashMap<>();
                        map.put("description", editedTask);

                        // Update database
                        db.collection(room)
                                .document(taskId)
                                .update(map)
                                .addOnSuccessListener(documentReference -> handler.postDelayed(runnable, 0))

                                // Error updating database
                                .addOnFailureListener(e -> error(e, getString(R.string.error_while_adding_task)));
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();

            return true;
        });


        // Listen tasks
        Query query = db.collection(room);
        query.addSnapshotListener((value, error) -> handler.postDelayed(runnable, 0));
    }

    public void error(Throwable error, String msg) {

        // Hide fab
        findViewById(R.id.fab).setVisibility(View.INVISIBLE);

        // Display error
        Snackbar.make(findViewById(R.id.relativeLayout), msg, Snackbar.LENGTH_LONG)
                .show();
        Log.w(TAG, "onCreate: ", error);

        // Display fab
        new Handler().postDelayed(() -> findViewById(R.id.fab).setVisibility(View.VISIBLE), 2800);
    }
}