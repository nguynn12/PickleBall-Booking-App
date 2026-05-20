package com.example.pickleball.activity.booking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.pickleball.R;
import com.example.pickleball.fragment.customer.CustomerMainActivity;
import com.google.android.material.button.MaterialButton;

import java.text.NumberFormat;
import java.util.Locale;

public class BookingSuccessActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "bookingId";
    public static final String EXTRA_COURT_NAME = "courtName";
    public static final String EXTRA_DATE       = "bookingDate";
    public static final String EXTRA_REMAINING  = "remainingAmount";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_success);

        String courtName       = getIntent().getStringExtra(EXTRA_COURT_NAME);
        String bookingDate     = getIntent().getStringExtra(EXTRA_DATE);
        double remainingAmount = getIntent().getDoubleExtra(EXTRA_REMAINING, 0);

        bindData(courtName, bookingDate, remainingAmount);

        ((MaterialButton) findViewById(R.id.btnViewBookings)).setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomerMainActivity.class);
            intent.putExtra("openTab", 3); // tab My Bookings
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        ((MaterialButton) findViewById(R.id.btnGoHome)).setOnClickListener(v -> {
            Intent intent = new Intent(this, CustomerMainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void bindData(String courtName, String bookingDate, double remaining) {
        if (courtName != null) {
            ((TextView) findViewById(R.id.tvSuccessCourtName))
                    .setText("Sân: " + courtName);
        }
        if (bookingDate != null) {
            ((TextView) findViewById(R.id.tvSuccessDate))
                    .setText("Ngày: " + bookingDate);
        }
        if (remaining > 0) {
            NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
            ((TextView) findViewById(R.id.tvRemainingReminder))
                    .setText("💰 Vui lòng thanh toán " + fmt.format((long) remaining)
                            + "đ còn lại tại sân khi đến chơi.");
        }
    }
}
