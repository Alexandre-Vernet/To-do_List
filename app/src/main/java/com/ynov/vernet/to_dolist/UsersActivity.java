package com.ynov.vernet.to_dolist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class UsersActivity extends AppCompatActivity {

    TextView textViewNoUser;
    ListView listViewUsers;
    ArrayList<String> arrayListUsers;
    String room;

    FirebaseFirestore db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        db = FirebaseFirestore.getInstance();

        textViewNoUser = findViewById(R.id.textViewNoUser);
        listViewUsers = findViewById(R.id.listViewUsers);

        // Get current room
        room = new SettingsActivity().getRoom(this, this);

        // Listen database
        Query query = db.collection(room);
        query.addSnapshotListener((value, error) -> this.getUsers());
    }

    public void getUsers() {
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

                    // Check count of users
                    if (arrayListUsers.size() <= 0)
                        textViewNoUser.setVisibility(View.VISIBLE);

                    else {
                        textViewNoUser.setVisibility(View.INVISIBLE);

                        // Display users in ListView
                        ArrayAdapter<String> arrayAdapterUsers;
                        arrayAdapterUsers = new ArrayAdapter<>(this, android.R.layout.select_dialog_item, arrayListUsers);
                        listViewUsers.setAdapter(arrayAdapterUsers);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
        finish();
    }
}