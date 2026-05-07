package com.example.pickleball.activity.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.pickleball.R;
import com.example.pickleball.model.User;
import com.example.pickleball.utils.Constants;
import com.example.pickleball.utils.ValidationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private TextInputEditText edtFullName, edtPhone, edtEmail, edtPassword, edtConfirmPassword;
    private TextInputLayout tilFullName, tilPhone, tilEmail, tilPassword, tilConfirmPassword;
    private Spinner spinnerRole, spinnerSkill;
    private MaterialButton btnRegister;
    private TextView tvGoToLogin;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // Role
    private final String[] ROLES_DISPLAY = {"Khach hang", "Chu san"};
    private final String[] ROLES_VALUE   = {"user",       "owner"};

    // Skill Level
    private final String[] SKILL_DISPLAY = {"Người mới bắt đầu (Beginner)",
            "Trung bình (Intermediate)",
            "Chuyên nghiệp (Pro)"};
    private final String[] SKILL_VALUE   = {Constants.SKILL_BEGINNER,
            Constants.SKILL_INTERMEDIATE,
            Constants.SKILL_PRO};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db    = FirebaseFirestore.getInstance();

        edtFullName  = findViewById(R.id.edtFullName);
        edtPhone     = findViewById(R.id.edtPhone);
        edtEmail     = findViewById(R.id.edtEmail);
        edtPassword  = findViewById(R.id.edtPassword);
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword);

        tilFullName  = findViewById(R.id.tilFullName);
        tilPhone     = findViewById(R.id.tilPhone);
        tilEmail     = findViewById(R.id.tilEmail);
        tilPassword  = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

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
            if (validateForm()) {
                String name  = edtFullName.getText().toString().trim();
                String phone = edtPhone.getText().toString().trim();
                String email = edtEmail.getText().toString().trim();
                String pass  = edtPassword.getText().toString().trim();
                String role  = ROLES_VALUE[spinnerRole.getSelectedItemPosition()];
                String skill = SKILL_VALUE[spinnerSkill.getSelectedItemPosition()];

                registerUser(name, phone, email, pass, role, skill);
            }
        });

        tvGoToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    /**
     * Validate all form fields using ValidationUtils
     */
    private boolean validateForm() {
        clearErrors();

        String name  = edtFullName.getText().toString().trim();
        String phone = edtPhone.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String pass  = edtPassword.getText().toString();
        String confirmPass = edtConfirmPassword.getText().toString();

        boolean isValid = true;

        // Validate name
        String nameError = ValidationUtils.getNameError(name);
        if (nameError != null) {
            tilFullName.setError(nameError);
            isValid = false;
        }

        // Validate phone
        String phoneError = ValidationUtils.getPhoneError(phone);
        if (phoneError != null) {
            tilPhone.setError(phoneError);
            isValid = false;
        }

        // Validate email
        String emailError = ValidationUtils.getEmailError(email);
        if (emailError != null) {
            tilEmail.setError(emailError);
            isValid = false;
        }

        // Validate password
        String passError = ValidationUtils.getPasswordError(pass);
        if (passError != null) {
            tilPassword.setError(passError);
            isValid = false;
        }

        // Validate confirm password
        if (!ValidationUtils.doPasswordsMatch(pass, confirmPass)) {
            tilConfirmPassword.setError(Constants.ERROR_PASSWORD_NOT_MATCH);
            isValid = false;
        }

        return isValid;
    }

    /**
     * Clear all error messages
     */
    private void clearErrors() {
        tilFullName.setError(null);
        tilPhone.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void registerUser(String name, String phone, String email,
                              String pass, String role, String skill) {
        btnRegister.setEnabled(false);
        btnRegister.setText(R.string.loading);

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String uid = firebaseUser.getUid();
                            User newUser = new User(uid, name, email, phone, role, skill);

                            db.collection(Constants.COLLECTION_USERS).document(uid).set(newUser)
                                    .addOnSuccessListener(v -> {
                                        Toast.makeText(this, Constants.SUCCESS_REGISTER, Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(this, LoginActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        btnRegister.setEnabled(true);
                                        btnRegister.setText(R.string.btn_register);
                                        Toast.makeText(this, "Lỗi lưu dữ liệu!", Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        btnRegister.setText(R.string.btn_register);
                        String msg = task.getException() != null
                                ? task.getException().getMessage() : Constants.ERROR_UNKNOWN;
                        Toast.makeText(this, "Đăng ký thất bại: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
