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
    private Spinner spinnerRole;
    private MaterialButton btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private final String[] ROLES_DISPLAY = {"Khach hang", "Chu san"};
    private final String[] ROLES_VALUE   = {"user",       "owner"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        edtFullName = findViewById(R.id.edtFullName);
        edtPhone    = findViewById(R.id.edtPhone);
        edtEmail    = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        spinnerRole = findViewById(R.id.spinnerRole);
        btnRegister = findViewById(R.id.btnRegister);
        tvGoToLogin = findViewById(R.id.tvGoToLogin);

        // Populate role spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, ROLES_DISPLAY);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRole.setAdapter(adapter);

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
            String selectedRole = ROLES_VALUE[spinnerRole.getSelectedItemPosition()];
            registerUser(name, phone, email, pass, selectedRole);
        });

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void registerUser(String name, String phone, String email, String pass, String role) {
        btnRegister.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        String uid = firebaseUser.getUid();
                        User newUser = new User(uid, name, email, phone, role);
                        db.collection("Users").document(uid).set(newUser)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(this, "Dang ky thanh cong!", Toast.LENGTH_LONG).show();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                btnRegister.setEnabled(true);
                                Toast.makeText(this, "Loi luu du lieu: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                    }
                } else {
                    btnRegister.setEnabled(true);
                    Toast.makeText(this, "Dang ky that bai: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                }
            });
    }
}
