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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    TextView textViewNoCurrentTask, textViewCountTask, textViewRoom;
    SearchView searchView;
    ImageView imageViewSort;
    ListView listView;

    int checkedItem;

    FirebaseFirestore db;

    ArrayList<Task> arrayList;
    int countTask = 0;

    // Debug
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textViewNoCurrentTask = findViewById(R.id.textViewNoCurrentTask);
        textViewCountTask = findViewById(R.id.textViewCountTask);
        textViewRoom = findViewById(R.id.textViewRoom);
        searchView = findViewById(R.id.searchView);
        imageViewSort = findViewById(R.id.imageViewSort);
        listView = findViewById(R.id.listView);

        db = FirebaseFirestore.getInstance();

        // Check Internet connexion
        boolean internet = new Internet(this, this).internet();
        if (!internet) {
            // Hide fab
            findViewById(R.id.fab).setVisibility(View.INVISIBLE);

            // Display message
            Snackbar.make(findViewById(R.id.relativeLayout), getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG)
                    .setAction(R.string.activate, v -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)))
                    .show();

            // Display fab
            new Handler().postDelayed(() -> findViewById(R.id.fab).setVisibility(View.VISIBLE), 2800);
        }

        // Menu
        new Menu(this, this);

        // Get room
        String room = new SettingsActivity().getRoom(this, this);

        // Display room code
        textViewRoom.setText(room);

        // Search bar
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                ArrayList<Task> results = new ArrayList<>();

                for (Task task : arrayList)
                    if (task.getDescription().toLowerCase().contains(newText.toLowerCase()) || task.getCreator().toLowerCase().contains(newText.toLowerCase()))
                        results.add(task);

                TaskListAdapter taskListAdapter = new TaskListAdapter(getApplicationContext(), 0, results);
                listView.setAdapter(taskListAdapter);

                return false;
            }
        });

        // Get pref item checked
        SharedPreferences sharedPreferencesSort = PreferenceManager.getDefaultSharedPreferences(this);
        checkedItem = sharedPreferencesSort.getInt("checkedItem", 0);

        // Sort tasks by
        final String[] sort = {getString(R.string.date), getString(R.string.creator)};

        // Get tasks from db
        Query query = db.collection(room);
        query.addSnapshotListener((value, error) -> refreshListTasks(sort[checkedItem]));

        // Sort by
        imageViewSort.setOnClickListener(v -> {
            AlertDialog.Builder alertDialogSort = new AlertDialog.Builder(this);
            alertDialogSort.setIcon(R.drawable.sort);
            alertDialogSort.setTitle(R.string.sort_by);
            alertDialogSort.setSingleChoiceItems(sort, checkedItem, (dialog, which) -> {

                // Sort tasks
                String selectedSort = sort[which];
                refreshListTasks(selectedSort);
                checkedItem = which;

                // Write pref sort
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putInt("checkedItem", which);
                editor.apply();

                // Dismiss dialog
                dialog.dismiss();
            });

            alertDialogSort.show();
        });

        // Click task
        listView.setOnItemClickListener((parent, view, position, id) -> {

            // Hide menu
            findViewById(R.id.fab).setVisibility(View.INVISIBLE);
            new Handler().postDelayed(() -> findViewById(R.id.fab).setVisibility(View.VISIBLE), 3100);

            // Get task
            Task task = arrayList.get(position);
            String taskId = task.getId();

            // Delete the task in database
            db.collection(room)
                    .document(taskId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        // Restore task with snackbar
                        Snackbar.make(findViewById(R.id.relativeLayout), getString(R.string.deleted_task), Snackbar.LENGTH_LONG)
                                .setAction(R.string.undo, v -> {

                                    // Restore content
                                    Map<String, Object> map = new HashMap<>();
                                    map.put("description", task.getDescription());
                                    map.put("date", task.getDate());
                                    map.put("creator", task.getCreator());

                                    // Add task to database
                                    db.collection(room)
                                            .add(map)
                                            .addOnFailureListener(e -> error(e, getString(R.string.error_while_adding_task)));
                                })
                                .show();
                    })

                    // Error deleting task
                    .addOnFailureListener(e -> error(e, getString(R.string.error_while_deleting_task)));
        });


        // Long press task
        listView.setOnItemLongClickListener((parent, view, position, id) -> {

            // Vibrate
            Vibrator vibe = (Vibrator) MainActivity.this.getSystemService(Context.VIBRATOR_SERVICE);
            vibe.vibrate(80);

            // Get task
            Task task = arrayList.get(position);
            String taskId = task.getId();
            String taskDescription = task.getDescription();
            String taskName = task.getCreator();
            Date taskDate = task.getDate();

            // Get date of creation
            String taskDateDay = new SimpleDateFormat("d/MM/y", Locale.getDefault()).format(taskDate);
            String taskDateHour = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(taskDate);

            // Keyboard
            EditText editText = new EditText(this);
            editText.setText(taskDescription);

            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.edit_task)
                    .setTitle(R.string.edit_task)
                    .setMessage(getString(R.string.created_by, taskName, taskDateDay, taskDateHour))
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

                        // Add edited task to map
                        Map<String, Object> map = new HashMap<>();
                        map.put("description", editedTask);

                        // Update database
                        db.collection(room)
                                .document(taskId)
                                .update(map)
                                .addOnFailureListener(e -> error(e, getString(R.string.error_while_adding_task)));
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();

            return true;
        });
    }

    public void refreshListTasks(String field) {

        arrayList = new ArrayList<>();
        String room = new SettingsActivity().getRoom(this, this);

        db.collection(room)
                .orderBy(field, Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(querySnapshotTask -> {
                    if (querySnapshotTask.isSuccessful()) {
                        countTask = 0;

                        // if no task
                        if (querySnapshotTask.getResult().isEmpty()) {
                            textViewCountTask.setVisibility(View.INVISIBLE);
                            textViewNoCurrentTask.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.INVISIBLE);
                        } else {
                            textViewCountTask.setVisibility(View.VISIBLE);
                            textViewNoCurrentTask.setVisibility(View.INVISIBLE);
                            listView.setVisibility(View.VISIBLE);

                            // Save data from database
                            for (QueryDocumentSnapshot document : querySnapshotTask.getResult()) {
                                // Get tasks
                                String id = document.getId();
                                String description = document.get("description").toString();
                                String creator = document.get("creator").toString();
                                Date date = document.getTimestamp("date").toDate();

                                // Create tasks
                                Task task = new Task(id, description, creator, date);

                                // Add tasks to array
                                arrayList.add(task);

                                // Increment count tasks
                                countTask++;
                            }

                            // Display count of current tasks
                            if (countTask <= 1)
                                textViewCountTask.setText(getString(R.string.current_task, countTask));
                            else
                                textViewCountTask.setText(getString(R.string.current_tasks, countTask));

                            // Display all tasks in ListView
                            TaskListAdapter adapter = new TaskListAdapter(this, R.layout.list_tasks, arrayList);
                            listView.setAdapter(adapter);
                        }

                        // Error while getting tasks
                    } else {
                        error(querySnapshotTask.getException(), getString(R.string.error_while_getting_tasks));
                    }
                });
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