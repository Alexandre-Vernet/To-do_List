package com.ynov.vernet.to_dolist;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.Random;

public class SettingsActivity extends AppCompatActivity {

    Context context;
    Activity activity;
    private static final String TAG = "SettingsActivity";

    String getName(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;

        // Check if user entered his name
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String name = sharedPreferences.getString("name", null);

        // if user has no name
        if (name == null) {
            EditText editText = new EditText(context);
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

                        // Save user's name
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                        editor.putString("name", editName);
                        editor.apply();
                    })
                    .show();
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.setCancelable(false);
        }

        return name;
    }

    String getRoom(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;

        // Check if user had room
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String room = prefs.getString("room", null);
        Log.d(TAG, "Room: " + room);

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
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putString("room", room);
            editor.apply();
        }

        return room;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);


        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            // Copy room
            Preference preferenceRoom = findPreference("room");
            preferenceRoom.setOnPreferenceClickListener(preference -> {

                // Get room
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                String room = prefs.getString("room", null);

                // Copy room code
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("room", room);
                clipboard.setPrimaryClip(clip);

                // Vibrate
                Vibrator vibe = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0, 100};
                vibe.vibrate(pattern, -1);

                return true;
            });

            // Redirect to Instagram
            Preference preferenceDeveloper = findPreference("developer");
            preferenceDeveloper.setOnPreferenceClickListener(preference -> {

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://www.instagram.com/alexandre_vernet/?hl=fr"));
                startActivity(intent);
                return true;
            });
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}