package com.example.pickleball.activity;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.pickleball.R;
import com.example.pickleball.model.Court;

public class CourtDetailActivity extends AppCompatActivity {

    public static final String EXTRA_COURT = "EXTRA_COURT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        tvPrice.setText(HomeActivity.formatVnd(court.getPricePerHour()));

        Glide.with(this)
                .load(court.getImageUrl())
                .centerCrop()
                .into(imgCover);
    }
}
