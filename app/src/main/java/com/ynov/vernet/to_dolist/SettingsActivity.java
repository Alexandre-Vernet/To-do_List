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

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.rarepebble.colorpicker.ColorPreference;

import java.util.Random;

public class SettingsActivity extends AppCompatActivity {

    Context context;
    Activity activity;
    private static final String TAG = "SettingsActivity";

    String generateRoom() {
        // Generate code room
        String characters = "1234567890AZERTYUIOPQSDFGHJKLMWXCVBN";
        final Random random = new Random();
        final StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; ++i)
            sb.append(characters.charAt(random.nextInt(characters.length())));
        String room = sb.toString();

        // Save it in memory
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putString("room", room);
        editor.apply();

        return room;
    }

    String getRoom(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;

        // Check if user had room
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String room = prefs.getString("room", null);

        // if no room
        if (room == null)
            room = this.generateRoom();

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

        public void onDisplayPreferenceDialog(Preference preference) {
            PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("color", 0xffff0000);


            if (preference instanceof ColorPreference) {
                ((ColorPreference) preference).showDialog(this, 0);
            } else super.onDisplayPreferenceDialog(preference);
        }
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


            // Generate new random room code
            Preference preferenceNewRoomCode = findPreference("generate_new_room_code");
            preferenceNewRoomCode.setOnPreferenceClickListener(preference -> {
                this.generateRoom();
                startActivity(new Intent(getContext(), MainActivity.class));
                return true;
            });



            // View person in this room
            Preference preferenceUsers = findPreference("person_in_room");
            preferenceUsers.setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getContext(), UsersActivity.class));
                return true;
            });
        }

        String generateRoom() {
            // Generate code room
            String characters = "1234567890AZERTYUIOPQSDFGHJKLMWXCVBN";
            final Random random = new Random();
            final StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; ++i)
                sb.append(characters.charAt(random.nextInt(characters.length())));
            String room = sb.toString();

            // Save it in memory
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
            editor.putString("room", room);
            editor.apply();

            return room;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        startActivity(new Intent(getApplicationContext(), MainActivity.class));
        finish();
    }
}