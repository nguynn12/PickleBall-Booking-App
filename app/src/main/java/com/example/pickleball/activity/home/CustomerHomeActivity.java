package com.example.pickleball.activity.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pickleball.R;
import com.example.pickleball.activity.HomeActivity; // Đảm bảo import đúng đường dẫn HomeActivity
import com.example.pickleball.activity.profile.ProfileActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CustomerHomeActivity extends AppCompatActivity {

    private TextView tvCustomerName, tvAvatarInitial, tvUpcomingCourtName, tvUpcomingTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);

        tvCustomerName      = findViewById(R.id.tvCustomerName);
        tvAvatarInitial     = findViewById(R.id.tvAvatarInitial);
        tvUpcomingCourtName = findViewById(R.id.tvUpcomingCourtName);
        tvUpcomingTime      = findViewById(R.id.tvUpcomingTime);

        loadUserInfo();

        // 1. Sửa nút tìm sân (nút tròn phía trên)
        findViewById(R.id.btnFindCourt).setOnClickListener(v -> {
            startActivity(new Intent(this, HomeActivity.class));
        });

        // 2. Sửa CardView "Tìm sân" (Phần bạn khoanh đỏ)
        findViewById(R.id.cardCourtList).setOnClickListener(v -> {
            Intent intent = new Intent(CustomerHomeActivity.this, HomeActivity.class);
            startActivity(intent);
        });

        // Các nút khác giữ nguyên hoặc cập nhật tương tự
        findViewById(R.id.cardMyBookings).setOnClickListener(v ->
                android.widget.Toast.makeText(this, "Lịch đặt sân - Coming soon!", android.widget.Toast.LENGTH_SHORT).show());

        findViewById(R.id.cardProfile).setOnClickListener(v ->
                startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void loadUserInfo() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("fullName");
                        tvCustomerName.setText(name != null ? name : "Khách hàng");
                        if (name != null && !name.isEmpty()) {
                            tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                        }
                    }
                });
    }
}