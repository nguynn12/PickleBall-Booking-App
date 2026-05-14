package com.example.pickleball.fragment.owner;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.pickleball.R;
import com.example.pickleball.activity.court.AddCourtActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class OwnerDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvOwnerName     = view.findViewById(R.id.tvOwnerName);
        TextView tvTotalCourts   = view.findViewById(R.id.tvTotalCourts);
        TextView tvPendingCount  = view.findViewById(R.id.tvPendingCount);
        TextView tvConfirmedCount = view.findViewById(R.id.tvConfirmedCount);
        LinearLayout btnManageCourts  = view.findViewById(R.id.btnManageCourts);
        LinearLayout btnManageBookings = view.findViewById(R.id.btnManageBookings);
        LinearLayout btnAddCourt      = view.findViewById(R.id.btnAddCourt);
        LinearLayout btnOwnerProfile  = view.findViewById(R.id.btnOwnerProfile);

        loadOwnerInfo(tvOwnerName);
        loadStats(tvTotalCourts, tvPendingCount, tvConfirmedCount);

        btnManageCourts.setOnClickListener(v -> {
            if (getActivity() instanceof OwnerMainActivity) {
                ((OwnerMainActivity) getActivity()).navigateTo(1);
            }
        });
        btnManageBookings.setOnClickListener(v -> {
            if (getActivity() instanceof OwnerMainActivity) {
                ((OwnerMainActivity) getActivity()).navigateTo(2);
            }
        });
        btnAddCourt.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddCourtActivity.class)));
        btnOwnerProfile.setOnClickListener(v -> {
            if (getActivity() instanceof OwnerMainActivity) {
                ((OwnerMainActivity) getActivity()).navigateTo(3);
            }
        });
    }

    private void loadOwnerInfo(TextView tvName) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        tvName.setText(name != null ? name : "Chủ sân");
                    }
                });
    }

    private void loadStats(TextView tvCourts, TextView tvPending, TextView tvConfirmed) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Real-time
        db.collection("Courts").whereEqualTo("ownerId", uid)
                .addSnapshotListener((s, e) -> { if (s != null) tvCourts.setText(String.valueOf(s.size())); });
        db.collection("Bookings").whereEqualTo("ownerId", uid).whereEqualTo("status", "pending")
                .addSnapshotListener((s, e) -> { if (s != null) tvPending.setText(String.valueOf(s.size())); });
        db.collection("Bookings").whereEqualTo("ownerId", uid).whereEqualTo("status", "confirmed")
                .addSnapshotListener((s, e) -> { if (s != null) tvConfirmed.setText(String.valueOf(s.size())); });
    }
}
