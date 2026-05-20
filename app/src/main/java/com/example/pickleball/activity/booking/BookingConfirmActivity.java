package com.example.pickleball.activity.booking;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.SelectedSlotAdapter;
import com.example.pickleball.model.Booking;
import com.example.pickleball.model.Court;
import com.example.pickleball.model.SelectedSlot;
import com.example.pickleball.utils.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BookingConfirmActivity extends AppCompatActivity {

    public static final String EXTRA_COURT  = "EXTRA_COURT";
    public static final String EXTRA_DATE   = "EXTRA_DATE";
    public static final String EXTRA_SLOTS  = "EXTRA_SLOTS";

    private Court court;
    private String selectedDate;
    private List<SelectedSlot> selectedSlots;

    private TextInputEditText edtName, edtPhone, edtNote;
    private MaterialButton btnConfirm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_confirm);

        court        = (Court) getIntent().getSerializableExtra(EXTRA_COURT);
        selectedDate = getIntent().getStringExtra(EXTRA_DATE);
        //noinspection unchecked
        selectedSlots = (List<SelectedSlot>) getIntent().getSerializableExtra(EXTRA_SLOTS);

        if (court == null || selectedSlots == null || selectedSlots.isEmpty()) {
            finish(); return;
        }

        initViews();
        bindData();

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnConfirm.setOnClickListener(v -> confirmBooking());
    }

    private void initViews() {
        edtName    = findViewById(R.id.edtConfirmName);
        edtPhone   = findViewById(R.id.edtConfirmPhone);
        edtNote    = findViewById(R.id.edtConfirmNote);
        btnConfirm = findViewById(R.id.btnConfirmBooking);
    }

    private void bindData() {
        // Thông tin sân
        TextView tvCourtName = findViewById(R.id.tvConfirmCourtName);
        TextView tvAddress   = findViewById(R.id.tvConfirmAddress);
        tvCourtName.setText("Tên CLB: " + (court.getCourtName() != null ? court.getCourtName() : ""));
        tvAddress.setText("Địa chỉ: " + (court.getAddress() != null ? court.getAddress() : ""));

        // Ngày
        ((TextView) findViewById(R.id.tvConfirmDate)).setText("Ngày: " + selectedDate);

        // Loại sân
        ((TextView) findViewById(R.id.tvConfirmType))
                .setText("Đối tượng: " + (court.getType() != null ? court.getType() : "Pickleball"));

        // Danh sách slot
        RecyclerView rv = findViewById(R.id.rvConfirmSlots);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setNestedScrollingEnabled(false);
        rv.setAdapter(new SelectedSlotAdapter(selectedSlots));

        // Tổng giờ + tiền
        double totalMoney = 0;
        for (SelectedSlot s : selectedSlots) totalMoney += s.getPrice();
        double totalMinutes = selectedSlots.size() * 30.0;
        double totalHours   = totalMinutes / 60.0;

        String hoursStr;
        if (totalHours == Math.floor(totalHours)) {
            hoursStr = String.format(Locale.getDefault(), "%.0fh", totalHours);
        } else {
            int h = (int) totalHours;
            int m = (int) ((totalHours - h) * 60);
            hoursStr = h + "h" + m;
        }

        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        ((TextView) findViewById(R.id.tvConfirmTotalHours)).setText("Tổng giờ: " + hoursStr);
        ((TextView) findViewById(R.id.tvConfirmTotalMoney)).setText("Tổng tiền: " + fmt.format(totalMoney) + " đ");

        // Prefill tên + SĐT từ profile
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null) {
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
    }

    private void confirmBooking() {
        String name  = edtName.getText()  != null ? edtName.getText().toString().trim()  : "";
        String phone = edtPhone.getText() != null ? edtPhone.getText().toString().trim() : "";
        String note  = edtNote.getText()  != null ? edtNote.getText().toString().trim()  : "";

        if (name.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên!", Toast.LENGTH_SHORT).show(); return;
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại!", Toast.LENGTH_SHORT).show(); return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show(); return;
        }

        btnConfirm.setEnabled(false);
        btnConfirm.setText("Đang xử lý...");

        double totalMoney   = 0;
        for (SelectedSlot s : selectedSlots) totalMoney += s.getPrice();
        double depositMoney = Math.round(totalMoney * Constants.DEPOSIT_RATE);
        double remaining    = totalMoney - depositMoney;

        List<Booking> bookings = mergeSlots(uid, note, name, depositMoney, remaining);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final int[] count = {0};
        final int total   = bookings.size();
        final String firstName = name;
        final double deposit   = depositMoney;
        final double total2    = totalMoney;

        for (Booking b : bookings) {
            db.collection("Bookings").add(b)
                    .addOnSuccessListener(ref -> {
                        ref.update("bookingId", ref.getId());
                        count[0]++;
                        if (count[0] == total) {
                            // Mở màn hình thanh toán
                            Intent intent = new Intent(this, PaymentActivity.class);
                            intent.putExtra(PaymentActivity.EXTRA_BOOKING_ID,   ref.getId());
                            intent.putExtra(PaymentActivity.EXTRA_DEPOSIT,       deposit);
                            intent.putExtra(PaymentActivity.EXTRA_TOTAL,         total2);
                            intent.putExtra(PaymentActivity.EXTRA_COURT_NAME,    court.getCourtName());
                            intent.putExtra(PaymentActivity.EXTRA_DATE,          selectedDate);
                            intent.putExtra(PaymentActivity.EXTRA_CUSTOMER_NAME, firstName);
                            startActivity(intent);
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnConfirm.setEnabled(true);
                        btnConfirm.setText("XÁC NHẬN & THANH TOÁN");
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    /** Gộp các slot liên tiếp của cùng sân con thành 1 booking */
    private List<Booking> mergeSlots(String uid, String note, String customerName,
                                      double depositPerBooking, double remainingPerBooking) {
        List<Booking> result = new ArrayList<>();
        if (selectedSlots.isEmpty()) return result;

        // Sắp xếp theo sân con + slot index
        List<SelectedSlot> sorted = new ArrayList<>(selectedSlots);
        sorted.sort((a, b) -> {
            int c = a.getSubCourtId().compareTo(b.getSubCourtId());
            return c != 0 ? c : Integer.compare(a.getSlotIndex(), b.getSlotIndex());
        });

        String curSubId   = null;
        String curStart   = null;
        String curEnd     = null;
        String curName    = null;
        double curPrice   = 0;

        for (SelectedSlot s : sorted) {
            if (!s.getSubCourtId().equals(curSubId)) {
                // Sân con mới → lưu booking cũ
                if (curSubId != null) {
                    result.add(makeBooking(uid, curSubId, curName, curStart, curEnd, curPrice,
                            note, customerName, depositPerBooking, remainingPerBooking));
                }
                curSubId = s.getSubCourtId();
                curName  = s.getSubCourtName();
                curStart = s.getStartTime();
                curEnd   = s.getEndTime();
                curPrice = s.getPrice();
            } else {
                // Cùng sân con → kiểm tra liên tiếp
                if (s.getStartTime().equals(curEnd)) {
                    // Liên tiếp → mở rộng
                    curEnd   = s.getEndTime();
                    curPrice += s.getPrice();
                } else {
                    // Không liên tiếp → lưu booking cũ, bắt đầu mới
                    result.add(makeBooking(uid, curSubId, curName, curStart, curEnd, curPrice,
                            note, customerName, depositPerBooking, remainingPerBooking));
                    curStart = s.getStartTime();
                    curEnd   = s.getEndTime();
                    curPrice = s.getPrice();
                }
            }
        }
        // Lưu booking cuối
        if (curSubId != null) {
            result.add(makeBooking(uid, curSubId, curName, curStart, curEnd, curPrice,
                    note, customerName, depositPerBooking, remainingPerBooking));
        }
        return result;
    }

    private Booking makeBooking(String uid, String subCourtId, String subCourtName,
                                 String start, String end, double price, String note,
                                 String customerName, double deposit, double remaining) {
        String ownerId = court.getOwnerId() != null ? court.getOwnerId() : "";
        Booking b = new Booking(uid, court.getCourtId(), court.getCourtName(),
                ownerId, selectedDate, start, end, price, note);
        b.setSubCourtId(subCourtId);
        b.setStatus(Constants.BOOKING_STATUS_AWAITING_PAYMENT);
        b.setCustomerName(customerName);
        b.setDepositAmount(deposit);
        b.setRemainingAmount(remaining);
        b.setPaymentStatus(Constants.PAYMENT_STATUS_PENDING);
        b.setPaymentExpiredAt(System.currentTimeMillis() + Constants.PAYMENT_TIMEOUT_MS);
        return b;
    }
}
