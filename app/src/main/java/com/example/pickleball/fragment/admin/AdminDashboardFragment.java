package com.example.pickleball.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.pickleball.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminDashboardFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvAdminName      = view.findViewById(R.id.tvAdminName);
        TextView tvTotalUsers     = view.findViewById(R.id.tvTotalUsers);
        TextView tvTotalCourtsAdmin = view.findViewById(R.id.tvTotalCourtsAdmin);
        TextView tvTotalBookings  = view.findViewById(R.id.tvTotalBookings);
        TextView tvPendingApproval = view.findViewById(R.id.tvPendingApproval);
        LinearLayout btnManageUsers = view.findViewById(R.id.btnManageUsers);
        LinearLayout btnManageCourtsAdmin = view.findViewById(R.id.btnManageCourtsAdmin);
        LinearLayout btnManageBookingsAdmin = view.findViewById(R.id.btnManageBookingsAdmin);
        LinearLayout btnAdminProfile = view.findViewById(R.id.btnAdminProfile);

        loadAdminInfo(tvAdminName);
        loadStats(tvTotalUsers, tvTotalCourtsAdmin, tvTotalBookings, tvPendingApproval);

        btnManageUsers.setOnClickListener(v -> {
            if (getActivity() instanceof AdminMainActivity) {
                ((AdminMainActivity) getActivity()).navigateTo(1);
            }
        });
        btnManageCourtsAdmin.setOnClickListener(v -> {
            if (getActivity() instanceof AdminMainActivity) {
                ((AdminMainActivity) getActivity()).navigateTo(2);
            }
        });
        btnManageBookingsAdmin.setOnClickListener(v ->
                android.widget.Toast.makeText(requireContext(), "Đang phát triển!", android.widget.Toast.LENGTH_SHORT).show());
        btnAdminProfile.setOnClickListener(v -> {
            if (getActivity() instanceof AdminMainActivity) {
                ((AdminMainActivity) getActivity()).navigateTo(3);
            }
        });

        // Ẩn nút logout khỏi dashboard (đã có trong Profile tab)
        LinearLayout btnAdminLogout = view.findViewById(R.id.btnAdminLogout);
        if (btnAdminLogout != null) btnAdminLogout.setVisibility(View.GONE);
    }

    private void loadAdminInfo(TextView tvName) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        tvName.setText(name != null ? name : "Admin");
                    }
                });
    }

    private void loadStats(TextView tvUsers, TextView tvCourts, TextView tvBookings, TextView tvPending) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Real-time listeners
        db.collection("Users").addSnapshotListener((s, e) -> {
            if (s != null) tvUsers.setText(String.valueOf(s.size()));
        });
        db.collection("Courts").addSnapshotListener((s, e) -> {
            if (s != null) tvCourts.setText(String.valueOf(s.size()));
        });
        db.collection("Bookings").addSnapshotListener((s, e) -> {
            if (s != null) tvBookings.setText(String.valueOf(s.size()));
        });
        db.collection("Bookings").whereEqualTo("status", "pending")
                .addSnapshotListener((s, e) -> {
                    if (s != null) tvPending.setText(String.valueOf(s.size()));
                });
    }
}
