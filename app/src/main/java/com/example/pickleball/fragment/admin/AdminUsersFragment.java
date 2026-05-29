package com.example.pickleball.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.UserAdapter;
import com.example.pickleball.model.User;
import com.example.pickleball.utils.Constants;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminUsersFragment extends Fragment {

    private RecyclerView rvUsers;
    private UserAdapter adapter;
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> displayList = new ArrayList<>();
    private TextView tvUserCount;
    private TextView tabAll, tabCustomer, tabOwner;
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_users, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvUsers = view.findViewById(R.id.rvUserList);
        tvUserCount = view.findViewById(R.id.tvUserCount);
        tabAll = view.findViewById(R.id.tabAll);
        tabCustomer = view.findViewById(R.id.tabCustomer);
        tabOwner = view.findViewById(R.id.tabOwner);

        rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new UserAdapter(requireContext(), displayList);
        rvUsers.setAdapter(adapter);

        tabAll.setOnClickListener(v -> applyFilter("all", tabAll, tabCustomer, tabOwner));
        tabCustomer.setOnClickListener(v -> applyFilter(Constants.ROLE_CUSTOMER, tabCustomer, tabAll, tabOwner));
        tabOwner.setOnClickListener(v -> applyFilter(Constants.ROLE_OWNER, tabOwner, tabAll, tabCustomer));

        setTabSelected(tabAll);
        setTabUnselected(tabCustomer);
        setTabUnselected(tabOwner);

        loadUsers();
    }

    private void loadUsers() {
        FirebaseFirestore.getInstance().collection(Constants.COLLECTION_USERS)
                .addSnapshotListener((snap, err) -> {
                    if (snap == null) return;

                    allUsers.clear();
                    for (var doc : snap) {
                        User user = doc.toObject(User.class);
                        if (user != null) {
                            if (user.getUserId() == null) user.setUserId(doc.getId());
                            allUsers.add(user);
                        }
                    }
                    filterAndShow();
                });
    }

    private void applyFilter(String filter, TextView selected, TextView u1, TextView u2) {
        currentFilter = filter;
        setTabSelected(selected);
        setTabUnselected(u1);
        setTabUnselected(u2);
        filterAndShow();
    }

    private void filterAndShow() {
        displayList.clear();
        for (User user : allUsers) {
            if ("all".equals(currentFilter) || currentFilter.equals(user.getRole())) {
                displayList.add(user);
            }
        }

        adapter.notifyDataSetChanged();
        if (tvUserCount != null) {
            tvUserCount.setText("Tổng số: " + displayList.size() + " người dùng");
        }
    }

    private void setTabSelected(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_chip_selected);
        tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_primary));
    }

    private void setTabUnselected(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_chip_unselected);
        tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
    }
}
