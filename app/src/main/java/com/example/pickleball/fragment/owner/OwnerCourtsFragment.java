package com.example.pickleball.fragment.owner;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.activity.court.AddCourtActivity;
import com.example.pickleball.activity.court.CourtDetailActivity;
import com.example.pickleball.adapter.CourtAdapter;
import com.example.pickleball.model.Court;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class OwnerCourtsFragment extends Fragment {

    private RecyclerView rvCourts;
    private CourtAdapter adapter;
    private final List<Court> courtList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner_courts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvCourts = view.findViewById(R.id.rvCourts);
        rvCourts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new CourtAdapter(requireContext(), courtList, court -> {
            Intent intent = new Intent(requireContext(), CourtDetailActivity.class);
            intent.putExtra(CourtDetailActivity.EXTRA_COURT, court);
            startActivity(intent);
        });
        rvCourts.setAdapter(adapter);

        FloatingActionButton fab = view.findViewById(R.id.fabAddCourt);
        fab.setOnClickListener(v -> startActivity(new Intent(requireContext(), AddCourtActivity.class)));

        loadMyCourts();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyCourts(); // Reload khi quay lại sau khi thêm sân
    }

    private void loadMyCourts() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("Courts")
                .whereEqualTo("ownerId", uid)
                .get()
                .addOnSuccessListener(snap -> {
                    courtList.clear();
                    for (var doc : snap.getDocuments()) {
                        Court c = doc.toObject(Court.class);
                        if (c != null) {
                            if (c.getCourtId() == null) c.setCourtId(doc.getId());
                            courtList.add(c);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Lỗi tải sân: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
