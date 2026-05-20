package com.example.pickleball.activity.player;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.fragment.customer.CustomerMainActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

public class MatchFoundActivity extends AppCompatActivity {

    public static final String EXTRA_MATCHED_USER_ID = "matchedUserId";

    private static final long AUTO_CLOSE_MS = 30_000L;

    private TextView tvMatchedName, tvMatchedSkill, tvMatchedInitial, tvCountdown;
    private CircleImageView imgMatchedAvatar;
    private MaterialButton btnBookCourt, btnSearchAgain;

    private CountDownTimer autoCloseTimer;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match_found);

        String matchedUserId = getIntent().getStringExtra(EXTRA_MATCHED_USER_ID);
        if (matchedUserId == null) { finish(); return; }

        bindViews();
        loadMatchedUser(matchedUserId);
        startAutoCloseTimer();

        btnBookCourt.setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomerMainActivity.class);
            intent.putExtra("openTab", 2); // tab bản đồ để tìm sân gần
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnSearchAgain.setOnClickListener(v -> {
            startActivity(new Intent(this, RadarMatchActivity.class));
            finish();
        });
    }

    private void bindViews() {
        tvMatchedName    = findViewById(R.id.tvMatchedName);
        tvMatchedSkill   = findViewById(R.id.tvMatchedSkill);
        tvMatchedInitial = findViewById(R.id.tvMatchedInitial);
        tvCountdown      = findViewById(R.id.tvCountdown);
        imgMatchedAvatar = findViewById(R.id.imgMatchedAvatar);
        btnBookCourt     = findViewById(R.id.btnBookCourt);
        btnSearchAgain   = findViewById(R.id.btnSearchAgain);
    }

    private void loadMatchedUser(String uid) {
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String name  = doc.getString("fullName");
                    String skill = doc.getString("skillLevel");
                    String avatar = doc.getString("avatarUrl");

                    if (name == null || name.isEmpty()) name = "Người chơi";
                    tvMatchedName.setText(name);
                    tvMatchedSkill.setText(formatSkill(skill));

                    if (avatar != null && !avatar.isEmpty()) {
                        Glide.with(this).load(avatar).circleCrop().into(imgMatchedAvatar);
                        imgMatchedAvatar.setVisibility(View.VISIBLE);
                        tvMatchedInitial.setVisibility(View.GONE);
                    } else {
                        imgMatchedAvatar.setVisibility(View.GONE);
                        tvMatchedInitial.setVisibility(View.VISIBLE);
                        tvMatchedInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
                    }
                });
    }

    private void startAutoCloseTimer() {
        autoCloseTimer = new CountDownTimer(AUTO_CLOSE_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                tvCountdown.setText("Tự động đóng sau " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                finish();
            }
        };
        autoCloseTimer.start();
    }

    private String formatSkill(String skill) {
        if (skill == null) return "Người mới";
        switch (skill.toLowerCase()) {
            case "intermediate": return "Trung bình";
            case "pro":          return "Chuyên nghiệp";
            default:             return "Người mới";
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoCloseTimer != null) autoCloseTimer.cancel();
    }
}
