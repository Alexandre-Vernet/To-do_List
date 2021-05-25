package com.ynov.vernet.to_dolist;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    Context context;
    ArrayList<Task> arrayList;

    final View.OnClickListener onClickListener;
    final View.OnLongClickListener onLongClickListener;

    FirebaseFirestore fStore;

    private static final String TAG = "TaskListAdapter";

    public TaskAdapter(Context context, ArrayList<Task> arrayList, View.OnClickListener onClickListener, View.OnLongClickListener onLongClickListener) {
        this.context = context;
        this.arrayList = arrayList;
        this.onClickListener = onClickListener;
        this.onLongClickListener = onLongClickListener;

        fStore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public TaskAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.task_item, parent, false);
        view.setOnClickListener(onClickListener);
        view.setOnLongClickListener(onLongClickListener);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        // Get description & creator
        String description = arrayList.get(position).getDescription();
        String creator = arrayList.get(position).getCreator();

        // Get name of user
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String name = prefs.getString("name", null);

        // Get color of user
        int text_color = prefs.getInt("text_color", Color.parseColor("#BF0000"));

        // Set custom color & font for user logged
        if (creator.equals(name)) {
            holder.textViewTask.setText(description);
            holder.textViewAddBy.setText(creator);
            holder.textViewAddBy.setTextColor(text_color);
            holder.textViewAddBy.setTypeface(Typeface.DEFAULT_BOLD);
        }
        holder.textViewTask.setText(description);
        holder.textViewAddBy.setText(creator);
    }

    @Override
    public int getItemCount() {
        return arrayList.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTask, textViewAddBy;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            textViewTask = itemView.findViewById(R.id.textViewTask);
            textViewAddBy = itemView.findViewById(R.id.textViewAddBy);
        }
    }
}
