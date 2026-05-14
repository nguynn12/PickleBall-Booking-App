package com.example.pickleball.activity.booking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.BookingAdapter;
import com.example.pickleball.model.Booking;
import com.example.pickleball.model.Court;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class BookingScheduleActivity extends AppCompatActivity {

    public static final String EXTRA_COURT = "EXTRA_COURT";

    private Court court;
    private String selectedDate = "";

    private TextView tvCourtName, tvDateLabel, tvScheduleDate, tvNoSlots, tvNoDayBookings;
    private ChipGroup chipGroupSlots;
    private RecyclerView rvDayBookings;
    private BookingAdapter dayAdapter;
    private final List<Booking> dayBookings = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_schedule);

        court = (Court) getIntent().getSerializableExtra(EXTRA_COURT);
        if (court == null) { finish(); return; }

        tvCourtName     = findViewById(R.id.tvCourtNameSchedule);
        tvDateLabel     = findViewById(R.id.tvSelectedDateLabel);
        tvScheduleDate  = findViewById(R.id.tvScheduleDate);
        tvNoSlots       = findViewById(R.id.tvNoSlots);
        tvNoDayBookings = findViewById(R.id.tvNoDayBookings);
        chipGroupSlots  = findViewById(R.id.chipGroupSlots);
        rvDayBookings   = findViewById(R.id.rvDayBookings);

        tvCourtName.setText(court.getCourtName() != null ? court.getCourtName() : "Sân Pickleball");

        rvDayBookings.setLayoutManager(new LinearLayoutManager(this));
        dayAdapter = new BookingAdapter(dayBookings);
        rvDayBookings.setAdapter(dayAdapter);

        // Mặc định chọn hôm nay
        Calendar today = Calendar.getInstance();
        selectedDate = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN")).format(today.getTime());
        tvScheduleDate.setText(selectedDate);
        loadSchedule(selectedDate);

        // Chọn ngày
        findViewById(R.id.btnPickScheduleDate).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
                tvScheduleDate.setText(selectedDate);
                tvDateLabel.setText("Lịch ngày " + selectedDate);
                loadSchedule(selectedDate);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Nút đặt sân → mở CreateBookingActivity
        ((MaterialButton) findViewById(R.id.btnGoBooking)).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateBookingActivity.class);
            intent.putExtra(CreateBookingActivity.EXTRA_COURT, court);
            startActivity(intent);
        });

        ((ImageView) findViewById(R.id.btnBack)).setOnClickListener(v -> finish());
    }

    /** Load tất cả booking của sân trong ngày, vẽ slot grid */
    private void loadSchedule(String date) {
        chipGroupSlots.removeAllViews();
        tvNoSlots.setVisibility(View.VISIBLE);

        FirebaseFirestore.getInstance()
                .collection("Bookings")
                .whereEqualTo("courtId", court.getCourtId())
                .whereEqualTo("date", date)
                .whereIn("status", List.of("pending", "confirmed"))
                .get()
                .addOnSuccessListener(snap -> {
                    // Thu thập các giờ đã bị đặt
                    Set<Integer> bookedHours = new HashSet<>();
                    dayBookings.clear();
                    for (var doc : snap.getDocuments()) {
                        Booking b = doc.toObject(Booking.class);
                        if (b == null) continue;
                        if (b.getBookingId() == null) b.setBookingId(doc.getId());
                        dayBookings.add(b);

                        // Đánh dấu các giờ bị chiếm
                        if (b.getStartTime() != null && b.getEndTime() != null) {
                            try {
                                int sh = Integer.parseInt(b.getStartTime().split(":")[0]);
                                int eh = Integer.parseInt(b.getEndTime().split(":")[0]);
                                for (int h = sh; h < eh; h++) bookedHours.add(h);
                            } catch (Exception ignored) {}
                        }
                    }
                    dayAdapter.notifyDataSetChanged();

                    // Hiện/ẩn danh sách booking ngày
                    tvNoDayBookings.setVisibility(dayBookings.isEmpty() ? View.VISIBLE : View.GONE);
                    rvDayBookings.setVisibility(dayBookings.isEmpty() ? View.GONE : View.VISIBLE);

                    // Vẽ slot grid từ giờ mở → giờ đóng
                    int openH  = parseHour(court.getOpenTime(),  6);
                    int closeH = parseHour(court.getCloseTime(), 22);

                    chipGroupSlots.removeAllViews();
                    tvNoSlots.setVisibility(View.GONE);

                    for (int h = openH; h < closeH; h++) {
                        boolean booked = bookedHours.contains(h);
                        Chip chip = new Chip(this);
                        chip.setText(String.format(Locale.getDefault(), "%02d:00", h));
                        chip.setCheckable(false);
                        chip.setClickable(!booked);

                        if (booked) {
                            chip.setChipBackgroundColorResource(R.color.error_red);
                            chip.setTextColor(getColor(android.R.color.white));
                            chip.setAlpha(0.7f);
                        } else {
                            chip.setChipBackgroundColorResource(R.color.green_light);
                            chip.setTextColor(getColor(R.color.green_dark));
                        }

                        // Click slot trống → mở đặt sân với giờ đó
                        if (!booked) {
                            final int slotHour = h;
                            chip.setOnClickListener(v -> {
                                Intent intent = new Intent(this, CreateBookingActivity.class);
                                intent.putExtra(CreateBookingActivity.EXTRA_COURT, court);
                                intent.putExtra("PRESET_DATE", selectedDate);
                                intent.putExtra("PRESET_START_HOUR", slotHour);
                                startActivity(intent);
                            });
                        }
                        chipGroupSlots.addView(chip);
                    }
                });
    }

    private int parseHour(String t, int def) {
        if (t == null || t.isEmpty()) return def;
        try { return Integer.parseInt(t.split(":")[0]); }
        catch (Exception e) { return def; }
    }
}
