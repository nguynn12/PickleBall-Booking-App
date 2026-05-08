package com.example.pickleball.activity.court;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.activity.booking.CreateBookingActivity;
import com.example.pickleball.model.Court;
import com.google.firebase.auth.FirebaseAuth;

import java.text.NumberFormat;
import java.util.Locale;

public class CourtDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COURT = "EXTRA_COURT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_court_detail);

        TextView tvName    = findViewById(R.id.tvDetailName);
        TextView tvPrice   = findViewById(R.id.tvDetailPrice);
        TextView tvAddress = findViewById(R.id.tvDetailAddress);
        ImageView imgCover = findViewById(R.id.imgDetailCover);
        Button btnBook     = findViewById(R.id.btnBookDetail);

        Court court = (Court) getIntent().getSerializableExtra(EXTRA_COURT);
        if (court == null) { finish(); return; }

        tvName.setText(court.getCourtName() != null ? court.getCourtName() : "Sân Pickleball");
        tvAddress.setText("📍 " + (court.getAddress() != null ? court.getAddress() : "Chưa có địa chỉ"));
        tvPrice.setText(formatVnd(court.getPricePerHour()));

        if (court.getImageUrl() != null && !court.getImageUrl().isEmpty()) {
            Glide.with(this).load(court.getImageUrl()).centerCrop().into(imgCover);
        } else {
            imgCover.setImageResource(R.drawable.ic_nav_court);
            imgCover.setBackgroundColor(getColor(R.color.green_light));
        }

        btnBook.setOnClickListener(v -> {
            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                android.widget.Toast.makeText(this, "Vui lòng đăng nhập để đặt sân!", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, CreateBookingActivity.class);
            intent.putExtra(CreateBookingActivity.EXTRA_COURT, court);
            startActivity(intent);
        });
    }

    private String formatVnd(double amount) {
        if (amount <= 0) return "Liên hệ";
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ/giờ";
    }
}
