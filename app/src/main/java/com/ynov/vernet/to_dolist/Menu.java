package com.ynov.vernet.to_dolist;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;
import androidx.preference.PreferenceManager;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Menu extends Activity {

    Activity activity;
    Context context;

    ExtendedFloatingActionButton fab;
    FloatingActionButton editRoomFab, addTaskFab, settingsFab;
    Boolean isAllFabsVisible;

    FirebaseFirestore db;

    private static final String TAG = "Menu";

    public Menu(Activity activity, Context context) {

        this.activity = activity;
        this.context = context;

        // Database
        db = FirebaseFirestore.getInstance();

        fab = this.activity.findViewById(R.id.fab);
        editRoomFab = this.activity.findViewById(R.id.editRoomFab);
        addTaskFab = this.activity.findViewById(R.id.addTaskFab);
        settingsFab = this.activity.findViewById(R.id.settingsFab);


        // Get room code
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String room = prefs.getString("room", null);

        // Get name
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String name = sharedPreferences.getString("name", null);

        Log.d(TAG, "Menu: " + room + name);

        // Hide widget
        editRoomFab.setVisibility(View.GONE);
        addTaskFab.setVisibility(View.GONE);
        settingsFab.setVisibility(View.GONE);
        isAllFabsVisible = false;

        // Toggle menu
        fab.setOnClickListener(v -> {
            if (!isAllFabsVisible) {
                ViewCompat.animate(fab)
                        .rotation(135.0F)
                        .withLayer()
                        .setDuration(300L)
                        .setInterpolator(new OvershootInterpolator(10.0F))
                        .start();
                editRoomFab.show();
                addTaskFab.show();
                settingsFab.show();
                fab.extend();
                isAllFabsVisible = true;
            } else {
                ViewCompat.animate(fab)
                        .rotation(0.0F)
                        .withLayer()
                        .setDuration(300L)
                        .setInterpolator(new OvershootInterpolator(10.0F))
                        .start();
                editRoomFab.hide();
                addTaskFab.hide();
                settingsFab.hide();
                fab.shrink();
                isAllFabsVisible = false;
            }
        });

        // Add a task
        addTaskFab.setOnClickListener(v -> {

            EditText editText = new EditText(context);
            editText.setHint("Add some text here");

            new AlertDialog.Builder(context)
                    .setIcon(R.drawable.add_task)
                    .setTitle("Add a task")
                    .setView(editText)
                    .setPositiveButton("Add", (dialogInterface, i) -> {

                        // Get entered task
                        String task = editText.getText().toString();

                        // Edit text can't be empty
                        if (task.isEmpty())
                            return;

                        // Add task
                        Map<String, Object> map = new HashMap<>();
                        map.put("description", task);

                        // Add username
                        map.put("user", name);

                        // Add date
                        Date date = Calendar.getInstance().getTime();
                        map.put("date", date);

                        // Add all to database
                        db.collection(room)
                                .add(map)

                                // Error adding database
                                .addOnFailureListener(e -> Snackbar.make(findViewById(R.id.relativeLayout), (getString(R.string.erreur_ajout_tache)) + e, Snackbar.LENGTH_LONG)
                                        .setAction(getString(R.string.reessayer), error -> {
                                        })
                                        .show());
                    })
                    .setNegativeButton("Cancel", null)
                    .show();

            // Hide menu
            ViewCompat.animate(fab)
                    .rotation(0.0F)
                    .withLayer()
                    .setDuration(300L)
                    .setInterpolator(new OvershootInterpolator(10.0F))
                    .start();
            editRoomFab.hide();
            addTaskFab.hide();
            settingsFab.hide();
            isAllFabsVisible = false;
        });


        // Join / Edit room
        editRoomFab.setOnClickListener(v -> {

        });


        // Settings
        settingsFab.setOnClickListener(v -> {
            this.activity.startActivity(new Intent(context, SettingsActivity.class));
            this.activity.finish();
        });
    }
}
