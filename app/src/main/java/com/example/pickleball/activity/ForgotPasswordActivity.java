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

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText edtEmailReset;
    private MaterialButton btnSendReset;
    private TextView tvBackToLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        mAuth = FirebaseAuth.getInstance();
        edtEmailReset = findViewById(R.id.edtEmailReset);
        btnSendReset  = findViewById(R.id.btnSendReset);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);

        btnSendReset.setOnClickListener(v -> {
            String email = edtEmailReset.getText().toString().trim();
            if (email.isEmpty()) {
                edtEmailReset.setError("Vui lòng nhập email!");
                return;
            }
            sendResetEmail(email);
        });

        tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void sendResetEmail(String email) {
        btnSendReset.setEnabled(false);
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Đã gửi link đặt lại mật khẩu đến " + email,
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        btnSendReset.setEnabled(true);
                        Toast.makeText(this,
                                "Email không tồn tại hoặc lỗi mạng!",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
