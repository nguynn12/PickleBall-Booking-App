package com.example.pickleball.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pickleball.R;
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

        // Quick action: Find court
        findViewById(R.id.btnFindCourt).setOnClickListener(v ->
            android.widget.Toast.makeText(this, "Tinh nang tim san - Coming soon!", android.widget.Toast.LENGTH_SHORT).show());

        // Quick cards
        findViewById(R.id.cardMyBookings).setOnClickListener(v ->
            android.widget.Toast.makeText(this, "Lich dat san - Coming soon!", android.widget.Toast.LENGTH_SHORT).show());

        findViewById(R.id.cardCourtList).setOnClickListener(v ->
            android.widget.Toast.makeText(this, "Tim san - Coming soon!", android.widget.Toast.LENGTH_SHORT).show());

        findViewById(R.id.cardProfile).setOnClickListener(v ->
            startActivity(new Intent(this, ProfileActivity.class)));
    }

    private void loadUserInfo() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("fullName");
                    tvCustomerName.setText(name != null ? name : "Khach hang");
                    if (name != null && !name.isEmpty()) {
                        tvAvatarInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    }
                }
            });
    }
}
