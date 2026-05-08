package com.example.pickleball.activity.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.pickleball.R;
import com.example.pickleball.activity.profile.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class OwnerHomeActivity extends AppCompatActivity {

    private TextView tvOwnerName, tvTotalCourts, tvPendingCount, tvConfirmedCount;
    private LinearLayout btnManageCourts, btnManageBookings, btnAddCourt, btnOwnerProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_home);

        tvOwnerName      = findViewById(R.id.tvOwnerName);
        tvTotalCourts    = findViewById(R.id.tvTotalCourts);
        tvPendingCount   = findViewById(R.id.tvPendingCount);
        tvConfirmedCount = findViewById(R.id.tvConfirmedCount);
        btnManageCourts  = findViewById(R.id.btnManageCourts);
        btnManageBookings= findViewById(R.id.btnManageBookings);
        btnAddCourt      = findViewById(R.id.btnAddCourt);
        btnOwnerProfile  = findViewById(R.id.btnOwnerProfile);

        loadOwnerInfo();
        loadStats();

        btnManageCourts.setOnClickListener(v ->
                Toast.makeText(this, "Quản lý sân - Đang phát triển!", Toast.LENGTH_SHORT).show());
        btnManageBookings.setOnClickListener(v ->
                Toast.makeText(this, "Đơn đặt sân - Đang phát triển!", Toast.LENGTH_SHORT).show());
        btnAddCourt.setOnClickListener(v ->
                Toast.makeText(this, "Thêm sân mới - Đang phát triển!", Toast.LENGTH_SHORT).show());
                                                        btnOwnerProfile.setOnClickListener(v ->
                                                                startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void loadOwnerInfo() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                                tvOwnerName.setText(name != null ? name : "Chủ sân");
                    }
                });
    }

    private void loadStats() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Count owner's courts
        db.collection("Courts").whereEqualTo("ownerId", uid).get()
                .addOnSuccessListener(snap -> tvTotalCourts.setText(String.valueOf(snap.size())));

        // Count pending bookings for owner's courts
        db.collection("Bookings").whereEqualTo("ownerId", uid)
                .whereEqualTo("status", "pending").get()
                        .addOnSuccessListener(snap -> tvPendingCount.setText(String.valueOf(snap.size())));

        // Count confirmed
        db.collection("Bookings").whereEqualTo("ownerId", uid)
                .whereEqualTo("status", "confirmed").get()
                        .addOnSuccessListener(snap -> tvConfirmedCount.setText(String.valueOf(snap.size())));
    }
}
