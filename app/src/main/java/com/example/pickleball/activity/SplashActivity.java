package com.example.pickleball.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pickleball.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                checkRoleAndNavigate(currentUser.getUid());
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
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

    /** Dung chung cho ca app (goi tu LoginActivity sau khi dang nhap) */
    public static void navigateByRole(android.content.Context context, String role) {
        Intent intent;
        if ("admin".equals(role)) {
            intent = new Intent(context, AdminHomeActivity.class);
        } else if ("owner".equals(role)) {
            intent = new Intent(context, OwnerHomeActivity.class);
        } else {
            intent = new Intent(context, CustomerHomeActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    private void navigateByRole(String role) {
        navigateByRole(this, role);
    }
}
