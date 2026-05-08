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
        "Tìm sân Pickleball",
        "Đặt sân dễ dàng",
        "Kết nối người chơi"
    };
    private static final String[] SUBTITLES = {
        "Khám phá hàng trăm sân Pickleball gần bạn, xem giá và tiện ích một cách nhanh chóng.",
        "Chọn ngày, chọn giờ trong vài chạm. Xác nhận tức thì, không cần gọi điện hỏi.",
        "Tìm đối thủ, lập nhóm chơi. Trải nghiệm Pickleball vui hơn bao giờ hết!"
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
