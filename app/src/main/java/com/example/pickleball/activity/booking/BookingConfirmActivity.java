package com.example.pickleball.activity.booking;

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
import java.util.ArrayList;
import java.util.Locale;

public class BookingConfirmActivity extends AppCompatActivity {

    public static final String EXTRA_COURT       = "EXTRA_COURT";
    public static final String EXTRA_DATE        = "EXTRA_DATE";
    public static final String EXTRA_COURT_NAMES = "EXTRA_COURT_NAMES";
    public static final String EXTRA_START_TIMES = "EXTRA_START_TIMES";
    public static final String EXTRA_END_TIMES   = "EXTRA_END_TIMES";
    public static final String EXTRA_PRICES      = "EXTRA_PRICES";

    private Court court;
    private String date;
    private ArrayList<String> courtNames, startTimes, endTimes;
    private double[] prices;

    private TextInputEditText edtName, edtPhone, edtNote;
    private MaterialButton btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirm);

        court      = (Court) getIntent().getSerializableExtra(EXTRA_COURT);
        date       = getIntent().getStringExtra(EXTRA_DATE);
        courtNames = getIntent().getStringArrayListExtra(EXTRA_COURT_NAMES);
        startTimes = getIntent().getStringArrayListExtra(EXTRA_START_TIMES);
        endTimes   = getIntent().getStringArrayListExtra(EXTRA_END_TIMES);
        prices     = getIntent().getDoubleArrayExtra(EXTRA_PRICES);

        if (court == null || courtNames == null) { finish(); return; }

        edtName  = findViewById(R.id.edtName);
        edtPhone = findViewById(R.id.edtPhone);
        edtNote  = findViewById(R.id.edtNote);
        btnConfirm = findViewById(R.id.btnConfirm);

        // Nếu đã đăng nhập, tự điền tên + phone
        prefillUserInfo();

        bindSummary();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> submitBookings());
    }

    private void prefillUserInfo() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;
        FirebaseFirestore.getInstance().collection("Users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name  = doc.getString("fullName");
                        String phone = doc.getString("phone");
                        if (name  != null) edtName.setText(name);
                        if (phone != null) edtPhone.setText(phone);
                    }
                });
    }

    private void bindSummary() {
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

        // Thông tin sân
        ((TextView) findViewById(R.id.tvConfirmCourtName))
                .setText("Tên CLB: " + (court.getCourtName() != null ? court.getCourtName() : ""));
        ((TextView) findViewById(R.id.tvConfirmAddress))
                .setText("Địa chỉ: " + (court.getAddress() != null ? court.getAddress() : ""));

        // Ngày
        ((TextView) findViewById(R.id.tvConfirmDate)).setText("Ngày: " + date);

        // Danh sách sân + giờ + giá
        LinearLayout layoutSlots = findViewById(R.id.layoutConfirmSlots);
        layoutSlots.removeAllViews();
        double totalMoney = 0;
        int totalMinutes  = 0;

        for (int i = 0; i < courtNames.size(); i++) {
            double price = prices != null && i < prices.length ? prices[i] : 0;
            totalMoney += price;

            // Tính phút từ startTime → endTime
            totalMinutes += calcMinutes(startTimes.get(i), endTimes.get(i));

            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tv.setText(String.format("- %s: %s - %s  |  %s đ",
                    courtNames.get(i), startTimes.get(i), endTimes.get(i),
                    fmt.format(price)));
            tv.setTextSize(14f);
            tv.setTextColor(getColor(R.color.white));
            tv.setPadding(0, 4, 0, 4);
            layoutSlots.addView(tv);
        }

        // Đối tượng
        ((TextView) findViewById(R.id.tvConfirmTarget))
                .setText("Đối tượng: " + (court.getType() != null ? court.getType() : "Pickleball")
                        + " " + buildCourtNumbers());

        // Tổng giờ
        int h = totalMinutes / 60, m = totalMinutes % 60;
        ((TextView) findViewById(R.id.tvConfirmTotalHours))
                .setText(String.format(Locale.getDefault(), "Tổng giờ: %dh%02d", h, m));

        // Tổng tiền
        ((TextView) findViewById(R.id.tvConfirmTotalMoney))
                .setText("Tổng tiền: " + fmt.format(totalMoney) + " đ");
    }

    private String buildCourtNumbers() {
        StringBuilder sb = new StringBuilder();
        for (String name : courtNames) {
            // "Sân 1" → "1"
            sb.append(name.replace("Sân ", "")).append(" ");
        }
        return sb.toString().trim();
    }

    private int calcMinutes(String start, String end) {
        try {
            int sh = Integer.parseInt(start.replace("h", ":").split(":")[0]);
            int sm = start.contains(":") ? Integer.parseInt(start.split(":")[1]) : 0;
            int eh = Integer.parseInt(end.replace("h", ":").split(":")[0]);
            int em = end.contains(":") ? Integer.parseInt(end.split(":")[1]) : 0;
            return (eh * 60 + em) - (sh * 60 + sm);
        } catch (Exception e) { return 0; }
    }

    private void submitBookings() {
        String name  = edtName.getText() != null ? edtName.getText().toString().trim() : "";
        String phone = edtPhone.getText() != null ? edtPhone.getText().toString().trim() : "";
        String note  = edtNote.getText() != null ? edtNote.getText().toString().trim() : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên!", Toast.LENGTH_SHORT).show(); return;
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại!", Toast.LENGTH_SHORT).show(); return;
        }

        // userId: nếu đã đăng nhập dùng uid, nếu không dùng phone làm id tạm
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "guest_" + phone;

        String ownerId = court.getOwnerId() != null ? court.getOwnerId() : "";

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Đang xử lý...");

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final int[] remaining = {courtNames.size()};

        for (int i = 0; i < courtNames.size(); i++) {
            double price = prices != null && i < prices.length ? prices[i] : 0;
            String subCourtName = court.getCourtName() + " - " + courtNames.get(i);

            // Convert "19h00" → "19:00"
            String start = startTimes.get(i).replace("h", ":");
            String end   = endTimes.get(i).replace("h", ":");

            Booking booking = new Booking(uid, court.getCourtId(), subCourtName,
                    ownerId, date, start, end, price, note);
            // Thêm tên người đặt vào note
            booking.setNote((name + " | " + phone + (note.isEmpty() ? "" : " | " + note)));

            db.collection("Bookings").add(booking)
                    .addOnSuccessListener(ref -> {
                        ref.update("bookingId", ref.getId());
                        remaining[0]--;
                        if (remaining[0] == 0) {
                            Toast.makeText(this,
                                    "Đặt sân thành công! Chờ chủ sân xác nhận.",
                                    Toast.LENGTH_LONG).show();
                            // Quay về 2 màn hình
                            setResult(RESULT_OK);
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("XÁC NHẬN");
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }
}
