package com.example.pickleball.activity.auth;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pickleball.R;
import com.example.pickleball.fragment.admin.AdminMainActivity;
import com.example.pickleball.fragment.customer.CustomerMainActivity;
import com.example.pickleball.fragment.owner.OwnerMainActivity;
import com.example.pickleball.activity.profile.SettingsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply dark mode BEFORE super.onCreate() to avoid recreate() loop
        SettingsActivity.applySavedDarkMode(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);


        new Handler().postDelayed(() -> {
            // 1. Kiểm tra đã xem onboarding chưa
            if (!OnboardingActivity.isOnboardingDone(this)) {
                startActivity(new Intent(this, OnboardingActivity.class));
                finish();
                return;
            }

            // 2. Kiểm tra đã đăng nhập chưa -> auto-login
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                checkRoleAndNavigate(currentUser.getUid());
            } else {
                // Không đăng nhập → vào app với role khách (CustomerMainActivity)
                Intent intent = new Intent(this, com.example.pickleball.fragment.customer.CustomerMainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        }, 2000);
    }

    private void checkRoleAndNavigate(String uid) {
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                            navigateByRole(role);
                })
                .addOnFailureListener(e -> {
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                });
    }

    /** Dùng chung cho cả app: chuyển trang theo role */
    public static void navigateByRole(android.content.Context context, String role) {
        Intent intent;
        if ("admin".equals(role)) {
            intent = new Intent(context, AdminMainActivity.class);
        } else if ("owner".equals(role)) {
            intent = new Intent(context, OwnerMainActivity.class);
        } else {
            intent = new Intent(context, CustomerMainActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

private void navigateByRole(String role) {
    navigateByRole(this, role);
}
}
