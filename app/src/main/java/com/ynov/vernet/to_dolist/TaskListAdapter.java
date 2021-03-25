package com.ynov.vernet.to_dolist;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Date;

public class TaskListAdapter extends ArrayAdapter<Task> {

    Context context;
    int resource;

    private static final String TAG = "TaskListAdapter";

    public TaskListAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Task> objects) {
        super(context, resource, objects);

        this.context = context;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // Get info task
        String id = getItem(position).getId();
        String description = getItem(position).getDescription();
        String creator = getItem(position).getCreator();
        Date date = getItem(position).getDate();

        // Create task
        new Task(id, description, creator, date);

        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_tasks, parent, false);

        // Write data in layout
        TextView textViewTask = convertView.findViewById(R.id.textViewTask);
        TextView textViewAddBy = convertView.findViewById(R.id.textViewAddBy);

        // Get name of user
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String name = prefs.getString("name", null);

        // Get color of user
        int text_color = prefs.getInt("text_color", 0);

        // Set custom color & font for user logged
        if (creator.equals(name)) {
            textViewTask.setText(description);
            textViewAddBy.setTextColor(text_color);
            textViewAddBy.setTypeface(Typeface.DEFAULT_BOLD);
        }
        textViewTask.setText(description);
        textViewAddBy.setText(creator);

        return convertView;
    }
}
