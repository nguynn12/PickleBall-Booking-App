package com.example.pickleball.activity.admin;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.UserAdapter;
import com.example.pickleball.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminUserListActivity extends AppCompatActivity {

    private RecyclerView rvUsers;

    private List<User> userList;
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_list);

        rvUsers = findViewById(R.id.rvUserList);

        userList = new ArrayList<>();

        adapter = new UserAdapter(this, userList);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {

        FirebaseFirestore.getInstance()
                .collection("Users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {

                    userList.clear();

                    for(var doc : queryDocumentSnapshots) {

                        User user = doc.toObject(User.class);

                        if(user.getUserId() == null) {
                            user.setUserId(doc.getId());
                        }

                        userList.add(user);
                    }

                    adapter.notifyDataSetChanged();
                });
    }
}