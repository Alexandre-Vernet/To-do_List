package com.ynov.vernet.to_dolist;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.text.InputType;
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
    FloatingActionButton addTaskFab, shareFab, settingsFab;
    Boolean isAllFabsVisible;

    FirebaseFirestore db;

    private static final String TAG = "Menu";

    public Menu(Activity activity, Context context) {

        this.activity = activity;
        this.context = context;

        // Database
        db = FirebaseFirestore.getInstance();

        fab = this.activity.findViewById(R.id.fab);
        addTaskFab = this.activity.findViewById(R.id.addTaskFab);
        shareFab = this.activity.findViewById(R.id.shareFab);
        settingsFab = this.activity.findViewById(R.id.settingsFab);

        // Expand menu
        addTaskFab.setVisibility(View.GONE);
        settingsFab.setVisibility(View.GONE);
        shareFab.setVisibility(View.GONE);
        isAllFabsVisible = false;

        fab.setOnClickListener(v -> {
            if (!isAllFabsVisible)
                this.showMenu();
            else
                this.hideMenu();
        });

        // Add a task
        addTaskFab.setOnClickListener(v -> {

            this.hideMenu();

            // Get name
            String name = this.getName();

            // If creator has no name
            EditText editText = new EditText(context);
            if (name == null) {
                editText.setHint(R.string.your_name);

                // Ask him
                AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setIcon(R.drawable.person)
                        .setTitle(R.string.welcome)
                        .setMessage(R.string.what_s_your_name)
                        .setView(editText)
                        .setPositiveButton(R.string.ok, (dialogInterface, i) -> {

                            // Get the name from EditText
                            String editName = editText.getText().toString();

                            // Name can't be empty
                            if (editName.isEmpty())
                                return;

                            // Save creator's name
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                            editor.putString("name", editName);
                            editor.apply();

                            // Add a task
                            this.addTask();

                        })
                        .show();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.setCancelable(false);

                // If user already has a name
            } else {
                // Add a task
                this.addTask();
            }
        });

        // Share room
        shareFab.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "Hey ! Join my To-Do List with the code " + this.getRoom() + " !");
            this.activity.startActivity(Intent.createChooser(intent, "Share with"));

            this.hideMenu();
        });


        // Settings
        settingsFab.setOnClickListener(v -> {
            this.activity.startActivity(new Intent(context, SettingsActivity.class));
            this.activity.finish();
        });
    }

    public void addTask() {

        // Get room code
        String room = this.getRoom();

        // Get name
        String name = this.getName();

        EditText editText = new EditText(context);

        // Open edit text
        editText.setHint(R.string.add_some_text_here);

        // First letter in uppercase
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        new AlertDialog.Builder(context)
                .setIcon(R.drawable.add)
                .setTitle(R.string.add_a_task)
                .setView(editText)
                .setPositiveButton(R.string.add, (dialogInterface, i) -> {

                    // Get entered task
                    String task = editText.getText().toString();

                    // Edit text can't be empty
                    if (task.isEmpty())
                        return;

                    // Add task
                    Map<String, Object> map = new HashMap<>();
                    map.put("description", task);

                    // Add creator's name
                    map.put("creator", name);

                    Date date = Calendar.getInstance().getTime();
                    map.put("date", date);

                    // Add all to database
                    db.collection(room)
                            .add(map)
                            .addOnFailureListener(e -> error(e, getString(R.string.error_while_adding_task)));

                    // Save data to notification
                    SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                    editor.putString("name", name);
                    editor.putString("task", task);
                    editor.apply();

                    // Send notification to other creator's in same room
                    AlarmManager manager = (AlarmManager) this.activity.getSystemService(Context.ALARM_SERVICE);
                    Intent alarmIntent = new Intent(context, Notification.class);
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
                    manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), 0, pendingIntent);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();

        this.hideMenu();
    }

    public void hideMenu() {
        ViewCompat.animate(fab)
                .rotation(0.0F)
                .withLayer()
                .setDuration(300L)
                .setInterpolator(new OvershootInterpolator(10.0F))
                .start();
        addTaskFab.hide();
        settingsFab.hide();
        shareFab.hide();
        isAllFabsVisible = false;
    }

    public void showMenu() {
        ViewCompat.animate(fab)
                .rotation(135.0F)
                .withLayer()
                .setDuration(300L)
                .setInterpolator(new OvershootInterpolator(10.0F))
                .start();
        addTaskFab.show();
        settingsFab.show();
        shareFab.show();
        fab.extend();
        isAllFabsVisible = true;
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


    String getRoom() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("room", null);
    }

    String getName() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("name", null);
    }
}
