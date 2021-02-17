package com.ynov.vernet.to_dolist;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.util.Random;

public class Room {

    Context context;
    Activity activity;
    private static final String TAG = "Room";

    Room(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    String getRoom() {
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
}
