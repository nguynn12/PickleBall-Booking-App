package com.example.pickleball.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pickleball.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminHomeActivity extends AppCompatActivity {

    private TextView tvAdminName, tvTotalUsers, tvTotalCourtsAdmin, tvTotalBookings, tvPendingApproval;
    private LinearLayout btnManageUsers, btnManageCourtsAdmin, btnManageBookingsAdmin,
            btnAdminProfile, btnAdminLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        tvAdminName         = findViewById(R.id.tvAdminName);
        tvTotalUsers        = findViewById(R.id.tvTotalUsers);
        tvTotalCourtsAdmin  = findViewById(R.id.tvTotalCourtsAdmin);
        tvTotalBookings     = findViewById(R.id.tvTotalBookings);
        tvPendingApproval   = findViewById(R.id.tvPendingApproval);
        btnManageUsers      = findViewById(R.id.btnManageUsers);
        btnManageCourtsAdmin= findViewById(R.id.btnManageCourtsAdmin);
        btnManageBookingsAdmin = findViewById(R.id.btnManageBookingsAdmin);
        btnAdminProfile     = findViewById(R.id.btnAdminProfile);
        btnAdminLogout      = findViewById(R.id.btnAdminLogout);

        loadAdminInfo();
        loadSystemStats();

        btnManageUsers.setOnClickListener(v ->
                Toast.makeText(this, "Quan ly nguoi dung - Coming soon!", Toast.LENGTH_SHORT).show());
                        btnManageCourtsAdmin.setOnClickListener(v ->
                                Toast.makeText(this, "Duyet & quan ly san - Coming soon!", Toast.LENGTH_SHORT).show());
                                        btnManageBookingsAdmin.setOnClickListener(v ->
                                                Toast.makeText(this, "Tat ca don dat san - Coming soon!", Toast.LENGTH_SHORT).show());
                                                        btnAdminProfile.setOnClickListener(v ->
                                                                startActivity(new Intent(this, ProfileActivity.class)));
        btnAdminLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void loadAdminInfo() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                                tvAdminName.setText(name != null ? name : "Admin");
                    }
                });
    }

    private void loadSystemStats() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("Users").get()
                .addOnSuccessListener(snap -> tvTotalUsers.setText(String.valueOf(snap.size())));

        db.collection("Courts").get()
                .addOnSuccessListener(snap -> tvTotalCourtsAdmin.setText(String.valueOf(snap.size())));

        db.collection("Bookings").get()
                .addOnSuccessListener(snap -> tvTotalBookings.setText(String.valueOf(snap.size())));

        db.collection("Bookings").whereEqualTo("status", "pending").get()
                .addOnSuccessListener(snap -> tvPendingApproval.setText(String.valueOf(snap.size())));
    }
}
