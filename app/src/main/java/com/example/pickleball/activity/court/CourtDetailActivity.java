package com.example.pickleball.activity.court;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.activity.booking.BookingScheduleActivity;
import com.example.pickleball.adapter.CourtDetailPagerAdapter;
import com.example.pickleball.model.Court;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.auth.FirebaseAuth;

public class CourtDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COURT = "EXTRA_COURT";

    private static final String[] TAB_TITLES = {
            "Thông tin", "Dịch vụ", "Hình ảnh", "Điều khoản\n& quy định", "Đánh giá"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_court_detail);

        Court court = (Court) getIntent().getSerializableExtra(EXTRA_COURT);
        if (court == null) { finish(); return; }

        // Views
        ImageView imgCover    = findViewById(R.id.imgDetailCover);
        ImageView imgLogo     = findViewById(R.id.imgCourtLogo);
        TextView tvName       = findViewById(R.id.tvDetailName);
        TextView tvType       = findViewById(R.id.tvCourtType);
        TextView tvAddress    = findViewById(R.id.tvDetailAddress);
        TextView tvOpenHours  = findViewById(R.id.tvOpenHours);
        TextView tvPhone      = findViewById(R.id.tvPhone);
        TextView tvRatingBadge = findViewById(R.id.tvRatingBadge);
        TabLayout tabLayout   = findViewById(R.id.tabLayout);
        ViewPager2 viewPager  = findViewById(R.id.viewPager);
        MaterialButton btnBook = findViewById(R.id.btnBookDetail);

        // Bind data
        tvName.setText(court.getCourtName() != null ? court.getCourtName() : "Sân Pickleball");
        tvType.setText(court.getType() != null ? court.getType() : "Pickleball");
        tvAddress.setText(court.getAddress() != null ? court.getAddress() : "Chưa có địa chỉ");
        tvOpenHours.setText(court.getOpenHours());
        tvPhone.setText(court.getPhone() != null && !court.getPhone().isEmpty()
                ? court.getPhone() : "Liên hệ");

        // Load rating từ Firestore (async)
        loadAvgRating(court.getCourtId(), tvRatingBadge);

        // Ảnh bìa
        if (court.getImageUrl() != null && !court.getImageUrl().isEmpty()) {
            Glide.with(this).load(court.getImageUrl()).centerCrop().into(imgCover);
            Glide.with(this).load(court.getImageUrl()).centerCrop().into(imgLogo);
        } else {
            imgCover.setBackgroundColor(getColor(R.color.green_light));
        }

        // Nút back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Nút ĐẶT LỊCH
        btnBook.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookingScheduleActivity.class);
            intent.putExtra(BookingScheduleActivity.EXTRA_COURT, court);
            startActivity(intent);
        });

        // Phone click → gọi điện
        findViewById(R.id.layoutPhone).setOnClickListener(v -> {
            String phone = court.getPhone();
            if (phone != null && !phone.isEmpty()) {
                Intent call = new Intent(Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:" + phone));
                startActivity(call);
            }
        });

        // ViewPager2 + TabLayout
        CourtDetailPagerAdapter pagerAdapter = new CourtDetailPagerAdapter(this, court);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(4); // pre-load tất cả tab

        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> tab.setText(TAB_TITLES[position])
        ).attach();
    }

    private void loadAvgRating(String courtId, TextView tvBadge) {
        if (courtId == null) return;
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("Reviews")
                .whereEqualTo("courtId", courtId)
                .get()
                .addOnSuccessListener(snap -> {
                    if (snap.isEmpty()) {
                        tvBadge.setText("Chưa có đánh giá");
                        return;
                    }
                    float total = 0;
                    for (var doc : snap.getDocuments()) {
                        Double r = doc.getDouble("rating");
                        if (r != null) total += r.floatValue();
                    }
                    float avg = total / snap.size();
                    tvBadge.setText(String.format(java.util.Locale.getDefault(),
                            "%.1f  (%d đánh giá)", avg, snap.size()));
                });
    }
}
