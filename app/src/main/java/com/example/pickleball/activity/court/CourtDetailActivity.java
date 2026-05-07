package com.example.pickleball.activity.court;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.model.Court;

import java.text.NumberFormat;
import java.util.Locale;

public class CourtDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COURT = "EXTRA_COURT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_court_detail);

        TextView tvName = findViewById(R.id.tvDetailName);
        TextView tvPrice = findViewById(R.id.tvDetailPrice);
        TextView tvAddress = findViewById(R.id.tvDetailAddress);
        ImageView imgCover = findViewById(R.id.imgDetailCover);

        Court court = (Court) getIntent().getSerializableExtra(EXTRA_COURT);
        if (court == null) {
            finish();
            return;
        }

        tvName.setText(court.getCourtName());
        tvAddress.setText("📍 " + court.getAddress());
        tvPrice.setText(formatVnd(court.getPricePerHour()));

        Glide.with(this)
                .load(court.getImageUrl())
                .centerCrop()
                .into(imgCover);
    }

    private String formatVnd(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount) + "đ/giờ";
    }
}
