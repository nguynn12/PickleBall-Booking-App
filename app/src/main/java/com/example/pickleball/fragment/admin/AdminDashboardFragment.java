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
import com.example.pickleball.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

        TextView tvAdminName = view.findViewById(R.id.tvAdminName);
        TextView tvTotalUsers = view.findViewById(R.id.tvTotalUsers);
        TextView tvTotalCourtsAdmin = view.findViewById(R.id.tvTotalCourtsAdmin);
        TextView tvTotalBookings = view.findViewById(R.id.tvTotalBookings);
        TextView tvPendingApproval = view.findViewById(R.id.tvPendingApproval);
        LinearLayout btnManageUsers = view.findViewById(R.id.btnManageUsers);
        LinearLayout btnManageCourtsAdmin = view.findViewById(R.id.btnManageCourtsAdmin);
        LinearLayout btnManageBookingsAdmin = view.findViewById(R.id.btnManageBookingsAdmin);
        LinearLayout btnAdminProfile = view.findViewById(R.id.btnAdminProfile);

        loadAdminInfo(tvAdminName);
        loadStats(tvTotalUsers, tvTotalCourtsAdmin, tvTotalBookings, tvPendingApproval);

        btnManageUsers.setOnClickListener(v -> navigateTo(1));
        btnManageBookingsAdmin.setOnClickListener(v -> navigateTo(2));
        btnManageCourtsAdmin.setOnClickListener(v -> navigateTo(3));
        btnAdminProfile.setOnClickListener(v -> navigateTo(4));

        LinearLayout btnAdminLogout = view.findViewById(R.id.btnAdminLogout);
        if (btnAdminLogout != null) btnAdminLogout.setVisibility(View.GONE);
    }

    private void navigateTo(int index) {
        if (getActivity() instanceof AdminMainActivity) {
            ((AdminMainActivity) getActivity()).navigateTo(index);
        }
    }

    private void loadAdminInfo(TextView tvName) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            tvName.setText("Admin");
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(Constants.COLLECTION_USERS)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString(Constants.FIELD_FULL_NAME);
                        tvName.setText(name != null ? name : "Admin");
                    }
                });
    }

    private void loadStats(TextView tvUsers, TextView tvCourts, TextView tvBookings, TextView tvPending) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection(Constants.COLLECTION_USERS).addSnapshotListener((snap, err) -> {
            if (snap != null) tvUsers.setText(String.valueOf(snap.size()));
        });
        db.collection(Constants.COLLECTION_COURTS).addSnapshotListener((snap, err) -> {
            if (snap != null) tvCourts.setText(String.valueOf(snap.size()));
        });
        db.collection(Constants.COLLECTION_BOOKINGS).addSnapshotListener((snap, err) -> {
            if (snap != null) tvBookings.setText(String.valueOf(snap.size()));
        });
        db.collection(Constants.COLLECTION_BOOKINGS)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snap, err) -> {
                    if (snap != null) tvPending.setText(String.valueOf(snap.size()));
                });
    }
}
