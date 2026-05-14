package com.example.pickleball.fragment.customer;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.example.pickleball.R;
import com.example.pickleball.fragment.ProfileFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class CustomerMainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_main);

        bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment());
                return true;
            } else if (id == R.id.nav_explore) {
                loadFragment(new CourtListFragment());
                return true;
            } else if (id == R.id.nav_map) {
                loadFragment(new MapFragment());
                return true;
            } else if (id == R.id.nav_bookings) {
                loadFragment(new MyBookingsFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
    }

    /** Cho phép fragment điều hướng sang tab khác */
    public void navigateTo(int index) {
        // 0=Trang chủ, 1=Khám phá, 2=Bản đồ, 3=Lịch đặt, 4=Tài khoản
        int[] ids = {R.id.nav_home, R.id.nav_explore, R.id.nav_map, R.id.nav_bookings, R.id.nav_profile};
        if (index >= 0 && index < ids.length) {
            bottomNav.setSelectedItemId(ids[index]);
        }
    }
}
