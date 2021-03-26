package com.ynov.vernet.to_dolist;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

public class Notification extends BroadcastReceiver {

    Intent intent;
    private static final String CANAL = "New task";

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        this.intent = intent;

        // Get name & task
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String name = prefs.getString("name", null);
        String task = prefs.getString("task", null);

        // Prepare onclick notification redirection
        Intent repeating_intent = new Intent(context, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 100, repeating_intent, PendingIntent.FLAG_UPDATE_CURRENT);

        // Display notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL)
                .setContentTitle(name + " added a new task !")
                .setContentText(task)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.plus)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.icon))
                .setColor(ContextCompat.getColor(context, R.color.blue))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);


        // Create channel
        String channelId = "id";
        String channelDescription = "desc";
        NotificationChannel notificationChannel = new NotificationChannel(channelId, CANAL, NotificationManager.IMPORTANCE_DEFAULT);
        notificationChannel.setDescription(channelDescription);
        notificationManager.createNotificationChannel(notificationChannel);
        builder.setChannelId(channelId);

        // Get notifications pref
        SharedPreferences sharedPreferencesNotification = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notification = sharedPreferencesNotification.getBoolean("notification", true);

        // Send notification
        if (notification)
            notificationManager.notify(100, builder.build());
    }
}
