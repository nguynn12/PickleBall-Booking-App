package com.example.pickleball.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pickleball.R;
import com.example.pickleball.model.User;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edtFullName, edtPhone, edtEmail, edtPassword;
    private Spinner spinnerRole, spinnerSkill;
    private MaterialButton btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Role
    private final String[] ROLES_DISPLAY = {"Khach hang", "Chu san"};
    private final String[] ROLES_VALUE   = {"user",       "owner"};

    // Skill Level (theo PDF)
    private final String[] SKILL_DISPLAY = {"Nguoi moi bat dau (Beginner)",
            "Trung binh (Intermediate)",
            "Chuyen nghiep (Pro)"};
    private final String[] SKILL_VALUE   = {"beginner", "intermediate", "pro"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        edtFullName  = findViewById(R.id.edtFullName);
        edtPhone     = findViewById(R.id.edtPhone);
        edtEmail     = findViewById(R.id.edtEmail);
        edtPassword  = findViewById(R.id.edtPassword);
        spinnerRole  = findViewById(R.id.spinnerRole);
        spinnerSkill = findViewById(R.id.spinnerSkill);
        btnRegister  = findViewById(R.id.btnRegister);
        tvGoToLogin  = findViewById(R.id.tvGoToLogin);

        // Setup role spinner
        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, ROLES_DISPLAY);
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(roleAdapter);

        // Setup skill spinner
        ArrayAdapter<String> skillAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SKILL_DISPLAY);
        skillAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSkill.setAdapter(skillAdapter);

        btnRegister.setOnClickListener(v -> {
            String name  = edtFullName.getText().toString().trim();
            String phone = edtPhone.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String pass  = edtPassword.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui long nhap du thong tin!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (pass.length() < 6) {
                Toast.makeText(this, "Mat khau phai tu 6 ky tu!", Toast.LENGTH_SHORT).show();
                return;
            }

            String role  = ROLES_VALUE[spinnerRole.getSelectedItemPosition()];
            String skill = SKILL_VALUE[spinnerSkill.getSelectedItemPosition()];
            registerUser(name, phone, email, pass, role, skill);
        });

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser(String name, String phone, String email,
                              String pass, String role, String skill) {
        btnRegister.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();
                            // Dung constructor co skillLevel
                            User newUser = new User(uid, name, email, phone, role, skill);
                            db.collection("Users").document(uid).set(newUser)
                                    .addOnSuccessListener(v -> {
                                        Toast.makeText(this, "Dang ky thanh cong!", Toast.LENGTH_LONG).show();
                                                startActivity(new Intent(this, LoginActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        btnRegister.setEnabled(true);
                                        Toast.makeText(this, "Loi luu du lieu!", Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        String msg = task.getException() != null
                                ? task.getException().getMessage() : "Loi khong xac dinh";
                        Toast.makeText(this, "Dang ky that bai: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
