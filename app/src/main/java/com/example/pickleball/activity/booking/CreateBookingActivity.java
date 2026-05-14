package com.example.pickleball.activity.booking;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.example.pickleball.R;
import com.example.pickleball.model.Booking;
import com.example.pickleball.model.Court;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateBookingActivity extends AppCompatActivity {

    public static final String EXTRA_COURT = "EXTRA_COURT";

    private TextView tvCourtName, tvCourtAddress, tvCourtPrice;
    private TextView tvSelectedDate, tvStartTime, tvEndTime, tvTotalPrice;
    private TextView tvOutsideHoursMsg;
    private LinearLayout btnPickDate, btnPickStartTime, btnPickEndTime;
    private MaterialCardView cardOutsideHours;
    private TextInputEditText edtNote;
    private MaterialButton btnConfirmBooking;

    private Court court;
    private String selectedDate = "";
    private String selectedStartTime = "";
    private String selectedEndTime = "";

    /** true = đang trong giờ hoạt động của sân */
    private boolean isWithinOpenHours = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_booking);

        court = (Court) getIntent().getSerializableExtra(EXTRA_COURT);
        if (court == null) { finish(); return; }

        initViews();
        bindCourtInfo();
        checkOpenHours();

        // Nhận preset từ BookingScheduleActivity (nếu có)
        String presetDate = getIntent().getStringExtra("PRESET_DATE");
        int presetHour    = getIntent().getIntExtra("PRESET_START_HOUR", -1);
        if (presetDate != null && !presetDate.isEmpty()) {
            selectedDate = presetDate;
            tvSelectedDate.setText(selectedDate);
        }
        if (presetHour >= 0) {
            selectedStartTime = String.format(java.util.Locale.getDefault(), "%02d:00", presetHour);
            tvStartTime.setText(selectedStartTime);
            int endH = Math.min(presetHour + 1, parseHour(court.getCloseTime(), 22));
            selectedEndTime = String.format(java.util.Locale.getDefault(), "%02d:00", endH);
            tvEndTime.setText(selectedEndTime);
            updateTotalPrice();
        }

        setupListeners();
    }

    // ─── INIT ────────────────────────────────────────────────────────────────

    private void initViews() {
        tvCourtName       = findViewById(R.id.tvCourtNameBooking);
        tvCourtAddress    = findViewById(R.id.tvCourtAddressBooking);
        tvCourtPrice      = findViewById(R.id.tvCourtPriceBooking);
        tvSelectedDate    = findViewById(R.id.tvSelectedDate);
        tvStartTime       = findViewById(R.id.tvStartTime);
        tvEndTime         = findViewById(R.id.tvEndTime);
        tvTotalPrice      = findViewById(R.id.tvTotalPrice);
        tvOutsideHoursMsg = findViewById(R.id.tvOutsideHoursMsg);
        btnPickDate       = findViewById(R.id.btnPickDate);
        btnPickStartTime  = findViewById(R.id.btnPickStartTime);
        btnPickEndTime    = findViewById(R.id.btnPickEndTime);
        cardOutsideHours  = findViewById(R.id.cardOutsideHours);
        edtNote           = findViewById(R.id.edtNote);
        btnConfirmBooking = findViewById(R.id.btnConfirmBooking);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void bindCourtInfo() {
        tvCourtName.setText(court.getCourtName() != null ? court.getCourtName() : "Sân Pickleball");
        tvCourtAddress.setText("📍 " + (court.getAddress() != null ? court.getAddress() : "Chưa có địa chỉ"));
        tvCourtPrice.setText("💰 " + formatPrice(court.getPricePerHour()) + "/giờ");
    }

    // ─── KIỂM TRA GIỜ MỞ CỬA ────────────────────────────────────────────────

    /**
     * So sánh giờ hiện tại với giờ mở cửa của sân.
     * Nếu ngoài giờ → hiện banner cảnh báo, disable tất cả input.
     * Nếu trong giờ → ẩn banner, enable bình thường.
     */
    private void checkOpenHours() {
        // Parse giờ mở/đóng từ Court model (mặc định 06:00 - 22:00)
        int openH  = parseHour(court.getOpenTime(),  6);
        int closeH = parseHour(court.getCloseTime(), 22);

        Calendar now = Calendar.getInstance();
        int currentH = now.get(Calendar.HOUR_OF_DAY);
        int currentM = now.get(Calendar.MINUTE);
        // Tính phút từ 0h để so sánh chính xác
        int nowMinutes   = currentH * 60 + currentM;
        int openMinutes  = openH * 60;
        int closeMinutes = closeH * 60;

        isWithinOpenHours = nowMinutes >= openMinutes && nowMinutes < closeMinutes;

        if (!isWithinOpenHours) {
            // Hiện banner cảnh báo
            cardOutsideHours.setVisibility(View.VISIBLE);
            tvOutsideHoursMsg.setText(
                    "Sân hoạt động từ " + formatHour(openH) + " - " + formatHour(closeH)
                    + ". Bạn có thể xem trước nhưng chưa thể đặt lịch."
            );
            setReadonlyMode(true);
        } else {
            cardOutsideHours.setVisibility(View.GONE);
            setReadonlyMode(false);
        }
    }

    /**
     * Bật/tắt chế độ readonly cho toàn bộ form.
     * readonly = true  → disable click, làm mờ, nút xác nhận bị khoá
     * readonly = false → enable bình thường
     */
    private void setReadonlyMode(boolean readonly) {
        // Màu nền của các row khi bị khoá
        int lockedBg = ContextCompat.getColor(this, R.color.input_bg);
        int normalBg = android.graphics.Color.TRANSPARENT;

        // Các row chọn ngày/giờ
        setRowLocked(btnPickDate,      readonly, lockedBg, normalBg);
        setRowLocked(btnPickStartTime, readonly, lockedBg, normalBg);
        setRowLocked(btnPickEndTime,   readonly, lockedBg, normalBg);

        // Ghi chú
        edtNote.setEnabled(!readonly);
        edtNote.setAlpha(readonly ? 0.5f : 1f);

        // Nút xác nhận
        btnConfirmBooking.setEnabled(!readonly);
        btnConfirmBooking.setAlpha(readonly ? 0.5f : 1f);
        if (readonly) {
            btnConfirmBooking.setText("Ngoài giờ hoạt động");
        } else {
            btnConfirmBooking.setText("Xác nhận đặt sân");
        }
    }

    private void setRowLocked(LinearLayout row, boolean locked, int lockedBg, int normalBg) {
        row.setClickable(!locked);
        row.setFocusable(!locked);
        row.setAlpha(locked ? 0.5f : 1f);
        // Xoá ripple khi locked
        if (locked) {
            row.setBackground(null);
            row.setBackgroundColor(lockedBg);
        } else {
            row.setBackgroundResource(android.R.attr.selectableItemBackground);
            // Dùng TypedValue để lấy đúng ripple drawable
            android.util.TypedValue tv = new android.util.TypedValue();
            getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true);
            row.setBackgroundResource(tv.resourceId);
        }
    }

    // ─── LISTENERS ───────────────────────────────────────────────────────────

    private void setupListeners() {
        btnPickDate.setOnClickListener(v -> {
            if (!isWithinOpenHours) { showLockedToast(); return; }
            showDatePicker();
        });
        btnPickStartTime.setOnClickListener(v -> {
            if (!isWithinOpenHours) { showLockedToast(); return; }
            showTimePicker(true);
        });
        btnPickEndTime.setOnClickListener(v -> {
            if (!isWithinOpenHours) { showLockedToast(); return; }
            showTimePicker(false);
        });
        btnConfirmBooking.setOnClickListener(v -> {
            if (!isWithinOpenHours) { showLockedToast(); return; }
            confirmBooking();
        });
    }

    private void showLockedToast() {
        int openH  = parseHour(court.getOpenTime(),  6);
        int closeH = parseHour(court.getCloseTime(), 22);
        Toast.makeText(this,
                "Sân chỉ nhận đặt lịch từ " + formatHour(openH) + " - " + formatHour(closeH),
                Toast.LENGTH_SHORT).show();
    }

    // ─── DATE / TIME PICKERS ─────────────────────────────────────────────────

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            tvSelectedDate.setText(selectedDate);
            updateTotalPrice();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        // Không cho chọn ngày trong quá khứ
        dialog.getDatePicker().setMinDate(cal.getTimeInMillis());
        dialog.show();
    }

    private void showTimePicker(boolean isStart) {
        int openH  = parseHour(court.getOpenTime(),  6);
        int closeH = parseHour(court.getCloseTime(), 22);

        // Giờ mặc định: nếu start → giờ mở cửa, nếu end → giờ mở + 1
        int defaultHour = isStart ? openH : Math.min(openH + 1, closeH);

        new TimePickerDialog(this, (view, hour, minute) -> {
            // Validate trong giờ mở cửa
            if (hour < openH || hour >= closeH) {
                Toast.makeText(this,
                        "Vui lòng chọn giờ trong khung " + formatHour(openH) + " - " + formatHour(closeH),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            String time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            if (isStart) {
                selectedStartTime = time;
                tvStartTime.setText(time);
            } else {
                selectedEndTime = time;
                tvEndTime.setText(time);
            }
            updateTotalPrice();
        }, defaultHour, 0, true).show();
    }

    private void updateTotalPrice() {
        if (selectedStartTime.isEmpty() || selectedEndTime.isEmpty()) return;
        try {
            int startH = Integer.parseInt(selectedStartTime.split(":")[0]);
            int startM = Integer.parseInt(selectedStartTime.split(":")[1]);
            int endH   = Integer.parseInt(selectedEndTime.split(":")[0]);
            int endM   = Integer.parseInt(selectedEndTime.split(":")[1]);

            double hours = (endH * 60 + endM - startH * 60 - startM) / 60.0;
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

    // ─── CONFIRM BOOKING ─────────────────────────────────────────────────────

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
        int startM = Integer.parseInt(selectedStartTime.split(":")[1]);
        int endH   = Integer.parseInt(selectedEndTime.split(":")[0]);
        int endM   = Integer.parseInt(selectedEndTime.split(":")[1]);

        if (endH * 60 + endM <= startH * 60 + startM) {
            Toast.makeText(this, "Giờ kết thúc phải sau giờ bắt đầu!", Toast.LENGTH_SHORT).show(); return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show(); return;
        }

        double hours = (endH * 60 + endM - startH * 60 - startM) / 60.0;
        double total = hours * court.getPricePerHour();
        String note  = edtNote.getText() != null ? edtNote.getText().toString().trim() : "";
        String ownerId = court.getOwnerId() != null ? court.getOwnerId() : "";

        btnConfirmBooking.setEnabled(false);
        btnConfirmBooking.setText("Đang xử lý...");

        // Kiểm tra slot trùng trước khi lưu
        checkConflictThenSave(uid, ownerId, total, note);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    /** Parse "HH:mm" hoặc "HH:00" → giờ nguyên. Trả về defaultVal nếu lỗi. */
    private int parseHour(String timeStr, int defaultVal) {
        if (timeStr == null || timeStr.isEmpty()) return defaultVal;
        try {
            return Integer.parseInt(timeStr.split(":")[0]);
        } catch (Exception e) {
            return defaultVal;
        }
    }

    private String formatHour(int hour) {
        return String.format(Locale.getDefault(), "%02d:00", hour);
    }

    private String formatPrice(double price) {
        if (price <= 0) return "Liên hệ";
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        return fmt.format(price) + "đ";
    }

    // ─── KIỂM TRA TRÙNG SLOT ─────────────────────────────────────────────────

    private void checkConflictThenSave(String uid, String ownerId, double total, String note) {
        int newStartMin = toMinutes(selectedStartTime);
        int newEndMin   = toMinutes(selectedEndTime);

        FirebaseFirestore.getInstance().collection("Bookings")
                .whereEqualTo("courtId", court.getCourtId())
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(snap -> {
                    boolean conflict = false;
                    for (var doc : snap.getDocuments()) {
                        String status = doc.getString("status");
                        // Chỉ kiểm tra pending và confirmed
                        if (!"pending".equals(status) && !"confirmed".equals(status)) continue;

                        String existStart = doc.getString("startTime");
                        String existEnd   = doc.getString("endTime");
                        if (existStart == null || existEnd == null) continue;

                        int exStartMin = toMinutes(existStart);
                        int exEndMin   = toMinutes(existEnd);

                        // Overlap: newStart < exEnd && newEnd > exStart
                        if (newStartMin < exEndMin && newEndMin > exStartMin) {
                            conflict = true;
                            break;
                        }
                    }

                    if (conflict) {
                        btnConfirmBooking.setEnabled(true);
                        btnConfirmBooking.setText("Xác nhận đặt sân");
                        Toast.makeText(this,
                                "⚠️ Khung giờ này đã có người đặt!\nVui lòng chọn giờ khác.",
                                Toast.LENGTH_LONG).show();
                    } else {
                        // Không trùng → lưu booking
                        Booking booking = new Booking(uid, court.getCourtId(), court.getCourtName(),
                                ownerId, selectedDate, selectedStartTime, selectedEndTime, total, note);
                        FirebaseFirestore.getInstance().collection("Bookings")
                                .add(booking)
                                .addOnSuccessListener(ref -> {
                                    ref.update("bookingId", ref.getId());
                                    Toast.makeText(this,
                                            "Đặt sân thành công! Chờ chủ sân xác nhận.",
                                            Toast.LENGTH_LONG).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    btnConfirmBooking.setEnabled(true);
                                    btnConfirmBooking.setText("Xác nhận đặt sân");
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    btnConfirmBooking.setEnabled(true);
                    btnConfirmBooking.setText("Xác nhận đặt sân");
                    Toast.makeText(this, "Lỗi kiểm tra lịch: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /** Chuyển "HH:mm" → phút từ 0h */
    private int toMinutes(String time) {
        if (time == null || !time.contains(":")) return 0;
        try {
            String[] parts = time.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) { return 0; }
    }
}
