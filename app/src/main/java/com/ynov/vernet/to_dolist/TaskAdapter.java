package com.ynov.vernet.to_dolist;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Vibrator;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    Context context;
    ArrayList<Task> arrayList;

    FirebaseFirestore fStore;

    private static final String TAG = "TaskListAdapter";

    public TaskAdapter(Context context, ArrayList<Task> arrayList) {
        this.context = context;
        this.arrayList = arrayList;

        fStore = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public TaskAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View view = layoutInflater.inflate(R.layout.task_item, parent, false);
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


        // Get room
        String room = new SettingsActivity().getRoom(context);

        holder.textViewTask.setOnClickListener(v -> {
            // Get task
            Task task = arrayList.get(position);
            String taskId = task.getId();

            // Delete the task in database
            fStore.collection(room)
                    .document(taskId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {

                        // Restore task with snackbar
//                        Snackbar.make(holder.textViewAddBy, context.getString(R.string.deleted_task), Snackbar.LENGTH_LONG)
//                                .setAction(context.getString(R.string.undo), view -> {
//
//                                    // Restore content
//                                    Map<String, Object> map = new HashMap<>();
//                                    map.put("description", task.getDescription());
//                                    map.put("date", task.getDate());
//                                    map.put("creator", task.getCreator());
//
//                                    // Add task to database
//                                    fStore.collection(room)
//                                            .add(map)
//                                            .addOnFailureListener(e -> {
//                                                Snackbar.make(holder.textViewTask, context.getString(R.string.error_while_adding_task), Snackbar.LENGTH_LONG)
//                                                        .show();
//                                                Log.w(TAG, "onCreate: ", e);
//                                            });
//                                })
//                                .show();
                    });
        });

        holder.textViewTask.setOnLongClickListener(v -> {

            // Vibrate
            Vibrator vibe = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            vibe.vibrate(80);

            // Get task
            Task task = arrayList.get(position);
            String taskId = task.getId();
            String taskDescription = task.getDescription();
            String taskName = task.getCreator();
            Date taskDate = task.getDate();

            // Get date of creation
            String taskDateDay = new SimpleDateFormat("d/MM/y", Locale.getDefault()).format(taskDate);
            String taskDateHour = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(taskDate);

            // Keyboard
            EditText editText = new EditText(context);
            editText.setText(taskDescription);

            new AlertDialog.Builder(context)
                    .setIcon(R.drawable.edit_task)
                    .setTitle(R.string.edit_task)
                    .setMessage(context.getString(R.string.created_by, taskName, taskDateDay, taskDateHour))
                    .setView(editText)
                    .setNeutralButton(R.string.copy, (dialog, which) -> {
                        // Copy task
                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        ClipData clip = ClipData.newPlainText("task", taskDescription);
                        clipboard.setPrimaryClip(clip);

                        // Vibrate
                        long[] pattern = {0, 100};
                        vibe.vibrate(pattern, -1);

                        // Display Toast
                        Toast.makeText(context, context.getString(R.string.task_copied), Toast.LENGTH_SHORT).show();
                    })
                    .setPositiveButton(R.string.save, (dialogInterface, i) -> {

                        // Get edited task
                        String editedTask = editText.getText().toString();

                        // Add edited task to map
                        Map<String, Object> map = new HashMap<>();
                        map.put("description", editedTask);

                        // Update database
                        fStore.collection(room)
                                .document(taskId)
                                .update(map)
                                .addOnFailureListener(e -> {
                                    Snackbar.make(holder.textViewTask, context.getString(R.string.error_while_adding_task), Snackbar.LENGTH_LONG)
                                            .show();
                                    Log.w(TAG, "onCreate: ", e);
                                });
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();

            return false;
        });

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
