package com.example.pickleball.fragment.admin;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.pickleball.R;
import com.example.pickleball.fragment.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class AdminMainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_main);

        bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            loadFragment(new AdminDashboardFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new AdminDashboardFragment());
                return true;
            } else if (id == R.id.nav_users) {
                loadFragment(new AdminUsersFragment());
                return true;
            } else if (id == R.id.nav_courts) {
                loadFragment(new AdminCourtsFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Kiểm tra xem tab hiện tại có phải là tab Home hay không
                if (bottomNav.getSelectedItemId() != R.id.nav_home) {
                    // Nếu đang ở tab khác (ví dụ Users), quay về tab Home
                    bottomNav.setSelectedItemId(R.id.nav_home);
                } else {
                    // Nếu đã ở tab Home rồi, tắt vòng lặp can thiệp này đi
                    setEnabled(false);
                    // Kích hoạt lại sự kiện Back để hệ thống tự xử lý (thoát app)
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    public void navigateTo(int index) {
        int[] ids = {R.id.nav_home, R.id.nav_users, R.id.nav_courts, R.id.nav_profile};
        if (index >= 0 && index < ids.length) {
            bottomNav.setSelectedItemId(ids[index]);
        }
    }
}
