package com.example.pickleball.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pickleball.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText edtEmail, edtPassword;
    private MaterialButton btnLogin;
    private TextView tvGoToRegister, tvForgotPassword;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        edtEmail = findViewById(R.id.edtEmailLogin);
        edtPassword = findViewById(R.id.edtPasswordLogin);
        btnLogin = findViewById(R.id.btnLogin);
        tvGoToRegister = findViewById(R.id.tvGoToRegister);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        btnLogin.setOnClickListener(v -> {
            String email = edtEmail.getText().toString().trim();
            String pass = edtPassword.getText().toString().trim();
            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui long nhap du thong tin!", Toast.LENGTH_SHORT).show();
                return;
            }
            loginUser(email, pass);
        });

        tvGoToRegister.setOnClickListener(v ->
            startActivity(new Intent(this, RegisterActivity.class)));

        tvForgotPassword.setOnClickListener(v ->
            startActivity(new Intent(this, ForgotPasswordActivity.class)));
    }

    private void loginUser(String email, String pass) {
        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    String uid = mAuth.getCurrentUser().getUid();
                    FirebaseFirestore.getInstance()
                        .collection("Users").document(uid).get()
                        .addOnSuccessListener(doc -> {
                            String role = doc.getString("role");
                            Toast.makeText(this, "Dang nhap thanh cong!", Toast.LENGTH_SHORT).show();
                            SplashActivity.navigateByRole(this, role);
                        })
                        .addOnFailureListener(e -> {
                            btnLogin.setEnabled(true);
                            Toast.makeText(this, "Loi lay thong tin nguoi dung!", Toast.LENGTH_SHORT).show();
                        });
                } else {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, "Sai email hoac mat khau!", Toast.LENGTH_SHORT).show();
                }
            });
    }
}
