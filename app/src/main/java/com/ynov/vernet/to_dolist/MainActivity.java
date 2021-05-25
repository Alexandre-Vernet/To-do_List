package com.ynov.vernet.to_dolist;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    Context context;

    RelativeLayout relativeLayout;
    TextView textViewNoCurrentTask, textViewCountTask, textViewRoom;
    SearchView searchView;
    ImageView imageViewSort;
    RecyclerView recyclerView;

    int checkedItem;

    FirebaseFirestore fStore;

    ArrayList<Task> arrayList;
    int countTask = 0;

    // Debug
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        relativeLayout = findViewById(R.id.relativeLayout);
        textViewNoCurrentTask = findViewById(R.id.textViewNoCurrentTask);
        textViewCountTask = findViewById(R.id.textViewCountTask);
        textViewRoom = findViewById(R.id.textViewRoom);
        searchView = findViewById(R.id.searchView);
        imageViewSort = findViewById(R.id.imageViewSort);
        recyclerView = findViewById(R.id.recyclerView);

        context = getApplicationContext();
        arrayList = new ArrayList<>();

        fStore = FirebaseFirestore.getInstance();


        // Set background color
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int color = prefs.getInt("background_color", Color.parseColor("#FFFFFF"));
        relativeLayout.setBackgroundColor(color);

        // Check Internet connexion
        boolean internet = new Internet(this, this).internet();
        if (!internet) {

            // Display message
            Snackbar.make(findViewById(R.id.fab), getString(R.string.no_internet_connection), Snackbar.LENGTH_LONG)
                    .setAction(R.string.activate, v -> startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS)))
                    .show();
        }

        // Menu
        new Menu(this, this);

        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        // Get room
        String room = new SettingsActivity().getRoom(this);

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
                    if (task.getDescription().toLowerCase().contains(newText.toLowerCase()) ||
                            task.getCreator().toLowerCase().contains(newText.toLowerCase()))
                        results.add(task);

                TaskAdapter taskAdapter = new TaskAdapter(context, results);
                recyclerView.setAdapter(taskAdapter);

                return false;
            }
        });

        // Get pref item checked
        SharedPreferences sharedPreferencesSort = PreferenceManager.getDefaultSharedPreferences(this);
        checkedItem = sharedPreferencesSort.getInt("checkedItem", 0);

        // Sort tasks by
        final String[] sort = {getString(R.string.date), getString(R.string.creator), getString(R.string.description)};

        // Get tasks from fStore
        Query query = fStore.collection(room);
        query.addSnapshotListener((value, error) -> refreshListTasks(sort[checkedItem]));

        // Sort by
        imageViewSort.setOnClickListener(v -> {
            AlertDialog.Builder alertDialogSort = new AlertDialog.Builder(context);
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
    }

    public void refreshListTasks(String field) {

        arrayList = new ArrayList<>();
        String room = new SettingsActivity().getRoom(this);

        fStore.collection(room)
                .orderBy(field, getDescending(field))
                .get()
                .addOnCompleteListener(querySnapshotTask -> {
                    if (querySnapshotTask.isSuccessful()) {
                        countTask = 0;

                        // if no task
                        if (querySnapshotTask.getResult().isEmpty()) {
                            textViewCountTask.setVisibility(View.INVISIBLE);
                            textViewNoCurrentTask.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.INVISIBLE);
                        } else {
                            textViewCountTask.setVisibility(View.VISIBLE);
                            textViewNoCurrentTask.setVisibility(View.INVISIBLE);
                            recyclerView.setVisibility(View.VISIBLE);

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
                            TaskAdapter adapter = new TaskAdapter(this, arrayList);
                            recyclerView.setAdapter(adapter);
                        }

                        // Error while getting tasks
                    } else {
                        error(querySnapshotTask.getException(), getString(R.string.error_while_getting_tasks));
                    }
                });
    }

    private Query.Direction getDescending(String field) {
        if (field.equals(getString(R.string.description)) || field.equals(getString(R.string.creator)))
            return Query.Direction.ASCENDING;
        else
            return Query.Direction.DESCENDING;
    }

    public void error(Throwable error, String msg) {

        // Display error
        Snackbar.make(findViewById(R.id.fab), msg, Snackbar.LENGTH_LONG)
                .show();
        Log.w(TAG, "onCreate: ", error);

    }
}