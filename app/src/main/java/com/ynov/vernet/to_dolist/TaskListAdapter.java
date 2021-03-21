package com.ynov.vernet.to_dolist;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class TaskListAdapter extends ArrayAdapter<Task> {

    Context context;
    int resource;

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
        String creator = getItem(position).getName();
        Date date = getItem(position).getDate();

        // Create task
        new Task(id, description, creator, date);

        LayoutInflater inflater = LayoutInflater.from(context);
        convertView = inflater.inflate(resource, parent, false);

        // Write data in layout
        TextView textViewTask = convertView.findViewById(R.id.textViewTask);
        TextView textViewAddBy = convertView.findViewById(R.id.textViewAddBy);
        textViewTask.setText(description);
        textViewAddBy.setText(creator);

        return convertView;
    }
}
