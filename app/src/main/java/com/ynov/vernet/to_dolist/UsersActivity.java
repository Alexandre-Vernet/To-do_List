package com.ynov.vernet.to_dolist;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class UsersActivity extends AppCompatActivity {

    FirebaseFirestore db;
    ListView listViewUsers;
    ArrayList<String> arrayListUsers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        db = FirebaseFirestore.getInstance();

        listViewUsers = findViewById(R.id.listViewUsers);

        // Get current room
        String room = new SettingsActivity().getRoom(this, this);

        Handler handler = new Handler();
        Runnable runnable = () ->
                db.collection(room)
                        .orderBy("date", Query.Direction.DESCENDING)
                        .get()
                        .addOnCompleteListener(task -> {
                            // Get all users in ArrayList
                            arrayListUsers = new ArrayList<>();

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (!arrayListUsers.contains(document.get("user").toString()))
                                    arrayListUsers.add(document.get("user").toString());
                            }

                            // Display users in ListView
                            ArrayAdapter<String> arrayAdapterUsers;
                            arrayAdapterUsers = new ArrayAdapter<>(this, android.R.layout.select_dialog_item, arrayListUsers);
                            listViewUsers.setAdapter(arrayAdapterUsers);
                        });

        handler.postDelayed(runnable, 0);

        // Listen database
        Query query = db.collection(room);
        query.addSnapshotListener((value, error) -> handler.postDelayed(runnable, 0));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
        finish();
    }
}