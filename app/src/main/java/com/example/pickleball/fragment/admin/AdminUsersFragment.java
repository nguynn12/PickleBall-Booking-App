package com.example.pickleball.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.UserAdapter;
import com.example.pickleball.model.User;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersFragment extends Fragment {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private final List<User> userList = new ArrayList<>();
    private TextView tvUserCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_admin_user_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvUsers     = view.findViewById(R.id.rvUserList);
        tvUserCount = view.findViewById(R.id.tvUserCount);

        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UserAdapter(requireContext(), userList);
        rvUsers.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        FirebaseFirestore.getInstance().collection("Users").get()
                .addOnSuccessListener(snap -> {
                    userList.clear();
                    for (var doc : snap) {
                        User u = doc.toObject(User.class);
                        if (u.getUserId() == null) u.setUserId(doc.getId());
                        userList.add(u);
                    }
                    adapter.notifyDataSetChanged();
                    if (tvUserCount != null) {
                        tvUserCount.setText("Tổng số: " + userList.size() + " người dùng");
                    }
                });
    }
}
