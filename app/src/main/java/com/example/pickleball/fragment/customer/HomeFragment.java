package com.example.pickleball.fragment.customer;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.pickleball.R;
import com.example.pickleball.activity.HomeActivity;
import com.example.pickleball.activity.profile.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_customer_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvCustomerName   = view.findViewById(R.id.tvCustomerName);
        TextView tvAvatarInitial  = view.findViewById(R.id.tvAvatarInitial);
        TextView tvUpcomingCourtName = view.findViewById(R.id.tvUpcomingCourtName);
        TextView tvUpcomingTime   = view.findViewById(R.id.tvUpcomingTime);

        loadUserInfo(tvCustomerName, tvAvatarInitial);

        // Nút "Đặt sân ngay" trong hero banner
        view.findViewById(R.id.btnFindCourt).setOnClickListener(v -> {
            // Chuyển sang tab Tìm sân (index 1)
            if (getActivity() instanceof CustomerMainActivity) {
                ((CustomerMainActivity) getActivity()).navigateTo(1);
            }
        });

        // Card "Tìm sân"
        view.findViewById(R.id.cardCourtList).setOnClickListener(v -> {
            if (getActivity() instanceof CustomerMainActivity) {
                ((CustomerMainActivity) getActivity()).navigateTo(1);
            }
        });

        // Card "Lịch đặt"
        view.findViewById(R.id.cardMyBookings).setOnClickListener(v -> {
            if (getActivity() instanceof CustomerMainActivity) {
                ((CustomerMainActivity) getActivity()).navigateTo(2);
            }
        });

        // Card "Hồ sơ"
        view.findViewById(R.id.cardProfile).setOnClickListener(v -> {
            if (getActivity() instanceof CustomerMainActivity) {
                ((CustomerMainActivity) getActivity()).navigateTo(3);
            }
        });

        // Avatar button → Profile tab
        view.findViewById(R.id.cardProfileBtn).setOnClickListener(v -> {
            if (getActivity() instanceof CustomerMainActivity) {
                ((CustomerMainActivity) getActivity()).navigateTo(3);
            }
        });
    }

    private void loadUserInfo(TextView tvName, TextView tvInitial) {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        tvName.setText(name != null ? name : "Khách hàng");
                        if (name != null && !name.isEmpty()) {
                            tvInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                        }
                    }
                });
    }
}
