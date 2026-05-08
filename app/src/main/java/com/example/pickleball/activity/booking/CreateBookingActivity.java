package com.example.pickleball.activity.booking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.pickleball.R;
import com.example.pickleball.model.Booking;
import com.example.pickleball.model.Court;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

public class CreateBookingActivity extends AppCompatActivity {

    public static final String EXTRA_COURT = "EXTRA_COURT";

    private TextView tvCourtName, tvCourtAddress, tvCourtPrice;
    private TextView tvSelectedDate, tvStartTime, tvEndTime, tvTotalPrice;
    private LinearLayout btnPickDate, btnPickStartTime, btnPickEndTime;
    private TextInputEditText edtNote;
    private MaterialButton btnConfirmBooking;

    private Court court;
    private String selectedDate = "";
    private String selectedStartTime = "";
    private String selectedEndTime = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_booking);

        court = (Court) getIntent().getSerializableExtra(EXTRA_COURT);
        if (court == null) { finish(); return; }

        initViews();
        bindCourtInfo();
        setupListeners();
    }

    private void initViews() {
        tvCourtName       = findViewById(R.id.tvCourtNameBooking);
        tvCourtAddress    = findViewById(R.id.tvCourtAddressBooking);
        tvCourtPrice      = findViewById(R.id.tvCourtPriceBooking);
        tvSelectedDate    = findViewById(R.id.tvSelectedDate);
        tvStartTime       = findViewById(R.id.tvStartTime);
        tvEndTime         = findViewById(R.id.tvEndTime);
        tvTotalPrice      = findViewById(R.id.tvTotalPrice);
        btnPickDate       = findViewById(R.id.btnPickDate);
        btnPickStartTime  = findViewById(R.id.btnPickStartTime);
        btnPickEndTime    = findViewById(R.id.btnPickEndTime);
        edtNote           = findViewById(R.id.edtNote);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void bindCourtInfo() {
        tvCourtName.setText(court.getCourtName() != null ? court.getCourtName() : "Sân Pickleball");
        tvCourtAddress.setText("📍 " + (court.getAddress() != null ? court.getAddress() : "Chưa có địa chỉ"));
        tvCourtPrice.setText("💰 " + formatPrice(court.getPricePerHour()) + "/giờ");
    }

    private void setupListeners() {
        btnPickDate.setOnClickListener(v -> showDatePicker());
        btnPickStartTime.setOnClickListener(v -> showTimePicker(true));
        btnPickEndTime.setOnClickListener(v -> showTimePicker(false));
        btnConfirmBooking.setOnClickListener(v -> confirmBooking());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            tvSelectedDate.setText(selectedDate);
            updateTotalPrice();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                .show();
    }

    private void showTimePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        new TimePickerDialog(this, (view, hour, minute) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            if (isStart) {
                selectedStartTime = time;
                tvStartTime.setText(time);
            } else {
                selectedEndTime = time;
                tvEndTime.setText(time);
            }
            updateTotalPrice();
        }, cal.get(Calendar.HOUR_OF_DAY), 0, true).show();
    }

    private void updateTotalPrice() {
        if (selectedStartTime.isEmpty() || selectedEndTime.isEmpty()) return;
        try {
            int startH = Integer.parseInt(selectedStartTime.split(":")[0]);
            int endH   = Integer.parseInt(selectedEndTime.split(":")[0]);
            int hours  = endH - startH;
            if (hours <= 0) {
                tvTotalPrice.setText("Giờ không hợp lệ");
                return;
            }
            double total = hours * court.getPricePerHour();
            tvTotalPrice.setText(formatPrice(total));
        } catch (Exception e) {
            tvTotalPrice.setText("0đ");
        }
    }

    private void confirmBooking() {
        if (selectedDate.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ngày!", Toast.LENGTH_SHORT).show(); return;
        }
        if (selectedStartTime.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn giờ bắt đầu!", Toast.LENGTH_SHORT).show(); return;
        }
        if (selectedEndTime.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn giờ kết thúc!", Toast.LENGTH_SHORT).show(); return;
        }

        int startH = Integer.parseInt(selectedStartTime.split(":")[0]);
        int endH   = Integer.parseInt(selectedEndTime.split(":")[0]);
        if (endH <= startH) {
            Toast.makeText(this, "Giờ kết thúc phải sau giờ bắt đầu!", Toast.LENGTH_SHORT).show(); return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show(); return;
        }

        double total = (endH - startH) * court.getPricePerHour();
        String note  = edtNote.getText() != null ? edtNote.getText().toString().trim() : "";

        // Lấy ownerId từ court (nếu có), nếu không để trống
        String ownerId = ""; // Court model hiện chưa có ownerId field

        Booking booking = new Booking(uid, court.getCourtId(), court.getCourtName(),
                ownerId, selectedDate, selectedStartTime, selectedEndTime, total, note);

        btnConfirmBooking.setEnabled(false);
        btnConfirmBooking.setText("Đang xử lý...");

        FirebaseFirestore.getInstance().collection("Bookings")
                .add(booking)
                .addOnSuccessListener(ref -> {
                    // Cập nhật bookingId vào document
                    ref.update("bookingId", ref.getId());
                    Toast.makeText(this, "Đặt sân thành công! Chờ chủ sân xác nhận.", Toast.LENGTH_LONG).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnConfirmBooking.setEnabled(true);
                    btnConfirmBooking.setText("Xác nhận đặt sân");
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String formatPrice(double price) {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        return fmt.format(price) + "đ";
    }
}
