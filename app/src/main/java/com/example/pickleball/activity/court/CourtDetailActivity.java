package com.example.pickleball.activity.court;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
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
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CourtDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COURT = "EXTRA_COURT";

    private static final String[] TAB_TITLES = {
            "Thông tin", "Dịch vụ", "Hình ảnh", "Điều khoản\n& quy định", "Đánh giá"
    };

    private boolean isFavorite = false; // Biến lưu trạng thái yêu thích

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
        ImageView btnFavorite = findViewById(R.id.btnFavorite); // Nút tim
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

        // Load rating từ Firestore
        loadAvgRating(court.getCourtId(), tvRatingBadge);

        // Ảnh bìa
        if (court.getImageUrl() != null && !court.getImageUrl().isEmpty()) {
            if (court.getImageUrl().startsWith("data:image")) {
                String pureBase64 = court.getImageUrl().substring(court.getImageUrl().indexOf(",") + 1);
                byte[] decodedString = android.util.Base64.decode(pureBase64, android.util.Base64.DEFAULT);
                Glide.with(this).load(decodedString).centerCrop().into(imgCover);
                Glide.with(this).load(decodedString).centerCrop().into(imgLogo);
            } else {
                Glide.with(this).load(court.getImageUrl()).centerCrop().into(imgCover);
                Glide.with(this).load(court.getImageUrl()).centerCrop().into(imgLogo);
            }
        } else {
            imgCover.setBackgroundColor(getColor(R.color.green_light));
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // --- XỬ LÝ NÚT YÊU THÍCH ---
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String favDocId = uid + "_" + court.getCourtId(); // Tạo ID document duy nhất
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Kiểm tra xem lúc mở lên đã thích chưa
        db.collection("Favorites").document(favDocId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                isFavorite = true;
                btnFavorite.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            }
        });

        // Bấm nút yêu thích
        btnFavorite.setOnClickListener(v -> {
            if (isFavorite) {
                // Bỏ thích
                db.collection("Favorites").document(favDocId).delete();
                isFavorite = false;
                btnFavorite.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary));
                Toast.makeText(this, "Đã bỏ khỏi danh sách yêu thích", Toast.LENGTH_SHORT).show();
            } else {
                // Thêm vào thích
                Map<String, Object> favData = new HashMap<>();
                favData.put("userId", uid);
                favData.put("courtId", court.getCourtId());
                favData.put("timestamp", System.currentTimeMillis());

                db.collection("Favorites").document(favDocId).set(favData);
                isFavorite = true;
                btnFavorite.setColorFilter(ContextCompat.getColor(this, android.R.color.holo_red_dark));
                Toast.makeText(this, "Đã thêm vào danh sách yêu thích", Toast.LENGTH_SHORT).show();
            }
        });
        // ---------------------------

        btnBook.setOnClickListener(v -> {
            Intent intent = new Intent(this, BookingScheduleActivity.class);
            intent.putExtra(BookingScheduleActivity.EXTRA_COURT, court);
            startActivity(intent);
        });

        findViewById(R.id.layoutPhone).setOnClickListener(v -> {
            String phone = court.getPhone();
            if (phone != null && !phone.isEmpty()) {
                Intent call = new Intent(Intent.ACTION_DIAL,
                        android.net.Uri.parse("tel:" + phone));
                startActivity(call);
            }
        });

        CourtDetailPagerAdapter pagerAdapter = new CourtDetailPagerAdapter(this, court);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(4);

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