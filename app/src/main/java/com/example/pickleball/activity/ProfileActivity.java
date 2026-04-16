package com.example.pickleball.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pickleball.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextInputEditText edtName, edtPhone;
    private TextView tvEmail, tvRole, tvUserName;
    private MaterialButton btnSaveProfile;
    private LinearLayout btnChangePassword, btnLogout;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        currentUid = user.getUid();

        edtName         = findViewById(R.id.edtName);
        edtPhone        = findViewById(R.id.edtPhone);
        tvEmail         = findViewById(R.id.tvEmail);
        tvRole          = findViewById(R.id.tvRole);
        tvUserName      = findViewById(R.id.tvUserName);
        btnSaveProfile  = findViewById(R.id.btnSaveProfile);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout       = findViewById(R.id.btnLogout);

        loadUserProfile();

        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadUserProfile() {
        db.collection("Users").document(currentUid).get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    String name = doc.getString("fullName");
                    edtName.setText(name);
                    edtPhone.setText(doc.getString("phone"));
                    tvEmail.setText(doc.getString("email"));
                    tvUserName.setText(name != null ? name : "Nguoi dung");
                    tvRole.setText(getRoleDisplay(doc.getString("role")));
                }
            });
    }

    private String getRoleDisplay(String role) {
        if ("admin".equals(role)) return "Quan tri vien";
        if ("owner".equals(role)) return "Chu san";
        return "Khach hang";
    }

    private void saveProfile() {
        String name  = edtName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(this, "Vui long nhap du thong tin!", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("phone", phone);
        db.collection("Users").document(currentUid).update(updates)
            .addOnSuccessListener(v -> {
                tvUserName.setText(name);
                Toast.makeText(this, "Cap nhat thanh cong!", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e ->
                Toast.makeText(this, "Loi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showChangePasswordDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Doi mat khau");

        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(60, 20, 60, 20);

        android.widget.EditText edtNewPass = new android.widget.EditText(this);
        edtNewPass.setHint("Mat khau moi (it nhat 6 ky tu)");
        edtNewPass.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        layout.addView(edtNewPass);
        builder.setView(layout);

        builder.setPositiveButton("Xac nhan", (dialog, which) -> {
            String newPass = edtNewPass.getText().toString().trim();
            if (newPass.length() < 6) {
                Toast.makeText(this, "Mat khau phai tu 6 ky tu!", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                user.updatePassword(newPass)
                    .addOnSuccessListener(v ->
                        Toast.makeText(this, "Doi mat khau thanh cong!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                        Toast.makeText(this, "Can dang nhap lai truoc khi doi mat khau!", Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton("Huy", null);
        builder.show();
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
