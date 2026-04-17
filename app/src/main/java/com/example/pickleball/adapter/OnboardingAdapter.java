package com.example.pickleball.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import com.example.pickleball.fragment.OnboardingFragment;

public class OnboardingAdapter extends FragmentStateAdapter {

    // Du lieu 3 slide Onboarding
    private static final int[]    IMAGES   = {
        android.R.drawable.ic_menu_gallery,   // thay bang anh that sau
        android.R.drawable.ic_menu_compass,
        android.R.drawable.ic_menu_today
    };
    private static final String[] TITLES   = {
        "Tim san Pickleball",
        "Dat san de dang",
        "Ket noi nguoi choi"
    };
    private static final String[] SUBTITLES = {
        "Kham pha hang tram san Pickleball gan ban, xem gia va tien ich mot cach nhanh chong.",
        "Chon ngay, chon gio trong vai cham. Xac nhan tuc thi, khong can goi dien hoi.",
        "Tim doi thu, lap nhom choi. Trai nghiem Pickleball vui hon bao gio het!"
    };

    public OnboardingAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public androidx.fragment.app.Fragment createFragment(int position) {
        return OnboardingFragment.newInstance(
            IMAGES[position],
            TITLES[position],
            SUBTITLES[position]
        );
    }

    @Override
    public int getItemCount() {
        return TITLES.length; // 3 slides
    }
}
