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
            } else if (id == R.id.nav_finance) {
                loadFragment(new AdminFinanceFragment());
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
                if (bottomNav.getSelectedItemId() != R.id.nav_home) {
                    bottomNav.setSelectedItemId(R.id.nav_home);
                } else {
                    setEnabled(false);
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
        // Order: Home, Users, Finance, Courts, Profile
        int[] ids = {R.id.nav_home, R.id.nav_users, R.id.nav_finance, R.id.nav_courts, R.id.nav_profile};
        if (index >= 0 && index < ids.length) {
            bottomNav.setSelectedItemId(ids[index]);
        }
    }
}
