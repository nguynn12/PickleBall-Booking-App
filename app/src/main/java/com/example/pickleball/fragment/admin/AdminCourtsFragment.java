package com.example.pickleball.fragment.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.activity.court.CourtDetailActivity;
import com.example.pickleball.adapter.AdminCourtAdapter;
import com.example.pickleball.model.Court;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminCourtsFragment extends Fragment {

    private RecyclerView rvCourts;
    private AdminCourtAdapter adapter;
    private final List<Court> allCourts   = new ArrayList<>();
    private final List<Court> displayList = new ArrayList<>();
    private TextView tabAll, tabActive, tabInactive;
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_courts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCourts    = view.findViewById(R.id.rvCourts);
        tabAll      = view.findViewById(R.id.tabAll);
        tabActive   = view.findViewById(R.id.tabActive);
        tabInactive = view.findViewById(R.id.tabInactive);

        rvCourts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdminCourtAdapter(displayList, new AdminCourtAdapter.OnActionListener() {
            @Override public void onActivate(Court court)   { confirmToggle(court, true); }
            @Override public void onDeactivate(Court court) { confirmToggle(court, false); }
            @Override public void onView(Court court) {
                Intent i = new Intent(requireContext(), CourtDetailActivity.class);
                i.putExtra(CourtDetailActivity.EXTRA_COURT, court);
                startActivity(i);
            }
        });
        rvCourts.setAdapter(adapter);

        tabAll.setOnClickListener(v      -> applyFilter("all",      tabAll,    tabActive, tabInactive));
        tabActive.setOnClickListener(v   -> applyFilter("active",   tabActive, tabAll,    tabInactive));
        tabInactive.setOnClickListener(v -> applyFilter("inactive", tabInactive, tabAll,  tabActive));

        // Set initial tab style
        setTabSelected(tabAll);
        setTabUnselected(tabActive);
        setTabUnselected(tabInactive);

        loadAllCourts();
    }

    private void loadAllCourts() {
        FirebaseFirestore.getInstance().collection("Courts")
                .addSnapshotListener((snap, err) -> {
                    if (snap == null) return;
                    allCourts.clear();
                    for (var doc : snap.getDocuments()) {
                        Court c = doc.toObject(Court.class);
                        if (c != null) {
                            if (c.getCourtId() == null) c.setCourtId(doc.getId());
                            allCourts.add(c);
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
        for (Court c : allCourts) {
            if ("all".equals(currentFilter)) {
                displayList.add(c);
            } else if ("active".equals(currentFilter) && !"inactive".equals(c.getStatus())) {
                displayList.add(c);
            } else if ("inactive".equals(currentFilter) && "inactive".equals(c.getStatus())) {
                displayList.add(c);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void confirmToggle(Court court, boolean activate) {
        String action = activate ? "kích hoạt" : "vô hiệu hóa";
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận")
                .setMessage("Bạn muốn " + action + " sân \"" + court.getCourtName() + "\"?")
                .setPositiveButton("Xác nhận", (d, w) -> toggleStatus(court, activate))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void toggleStatus(Court court, boolean activate) {
        String newStatus = activate ? "active" : "inactive";
        FirebaseFirestore.getInstance().collection("Courts")
                .document(court.getCourtId())
                .update("status", newStatus)
                .addOnSuccessListener(v -> {
                    court.setStatus(newStatus);
                    filterAndShow();
                    Toast.makeText(requireContext(),
                            activate ? "Đã kích hoạt sân!" : "Đã vô hiệu hóa sân!",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void setTabSelected(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_chip_selected);
        tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_primary));
    }

    private void setTabUnselected(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_chip_unselected);
        tab.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
    }
}
