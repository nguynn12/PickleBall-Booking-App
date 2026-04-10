package com.example.pickleball.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pickleball.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ẩn thanh Action Bar để màn hình splash bao phủ toàn bộ
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_splash);

        TextView tvSplashTitle = findViewById(R.id.tvSplashTitle);

        // Hiệu ứng: Chữ hiện dần (Fade-in) trong 1.2 giây
        tvSplashTitle.animate()
                .alpha(1f)
                .setDuration(1200)
                .start();

        // Tự động chuyển sang LoginActivity sau 2500ms (2.5 giây)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Đóng SplashActivity để không quay lại được khi bấm Back
        }, 2500);
    }
}