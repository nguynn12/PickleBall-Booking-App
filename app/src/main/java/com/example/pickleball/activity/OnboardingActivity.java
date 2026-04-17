package com.example.pickleball.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.pickleball.R;
import com.example.pickleball.adapter.OnboardingAdapter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabIndicator;
    private MaterialButton btnNext;
    private TextView tvSkip;

    private static final String PREFS_NAME = "PickleBallPrefs";
    private static final String KEY_ONBOARDING_DONE = "onboardingDone";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        viewPager    = findViewById(R.id.viewPager);
        tabIndicator = findViewById(R.id.tabIndicator);
        btnNext      = findViewById(R.id.btnNext);
        tvSkip       = findViewById(R.id.tvSkip);

        // Setup adapter
        OnboardingAdapter adapter = new OnboardingAdapter(this);
        viewPager.setAdapter(adapter);

        // Dots indicator
        new TabLayoutMediator(tabIndicator, viewPager, (tab, position) -> {}).attach();

        // Skip -> jump to last
        tvSkip.setOnClickListener(v ->
            viewPager.setCurrentItem(adapter.getItemCount() - 1, true));

        // Next / Get Started
        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < adapter.getItemCount() - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });

        // Update button text khi vuot trang
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == adapter.getItemCount() - 1) {
                    btnNext.setText("Bat dau ngay!");
                    tvSkip.setVisibility(View.GONE);
                } else {
                    btnNext.setText("Tiep theo");
                    tvSkip.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void finishOnboarding() {
        // Luu da xem onboarding -> khong hien thi lai
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply();

        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /** Kiem tra xem da xem onboarding chua (goi tu SplashActivity) */
    public static boolean isOnboardingDone(android.content.Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                  .getBoolean(KEY_ONBOARDING_DONE, false);
    }
}
