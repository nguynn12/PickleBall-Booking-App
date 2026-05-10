package com.example.pickleball.activity.booking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.pickleball.R;
import com.example.pickleball.model.Booking;
import com.example.pickleball.model.Court;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class BookingScheduleActivity extends AppCompatActivity {

    public static final String EXTRA_COURT = "EXTRA_COURT";

    // Cấu hình bảng lịch
    private static final int SLOT_MINUTES = 30;   // mỗi ô = 30 phút
    private static final int DEFAULT_COURTS = 5;  // số sân mặc định

    private Court court;
    private String selectedDate;
    private int courtCount = DEFAULT_COURTS;       // số sân hiển thị (slider điều chỉnh)

    // Dữ liệu slot đã đặt từ Firestore: key = "courtIndex_slotIndex"
    private final Set<String> bookedSlots = new HashSet<>();
    // Slot người dùng đang chọn: key = "courtIndex_slotIndex"
    private final Set<String> selectedSlots = new HashSet<>();

    // Views
    private TextView tvSelectedDate, tvTotalHours, tvTotalMoney, tvToggleArrow;
    private LinearLayout llCourtHeaders, llGrid, panelSummary, layoutSelectedDetails;
    private HorizontalScrollView hsvHeader, hsvBody;
    private ScrollView svBody;
    private MaterialButton btnNext;
    private boolean summaryExpanded = false;

    // Slot config
    private int openHour, closeHour;
    private int totalSlots;
    private int slotWidthDp = 56; // width mỗi cột sân (dp)
    private int timeColWidthDp = 60;
    private int rowHeightDp = 40;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_schedule);

        court = (Court) getIntent().getSerializableExtra(EXTRA_COURT);
        if (court == null) { finish(); return; }

        // Kiểm tra sân có đang mở không
        if (!isCourtOpen()) {
            Toast.makeText(this, "Sân hiện đang đóng cửa. Vui lòng quay lại trong giờ mở cửa!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Parse giờ mở/đóng
        openHour  = parseHour(court.getOpenTime(),  6);
        closeHour = parseHour(court.getCloseTime(), 22);
        totalSlots = (closeHour - openHour) * (60 / SLOT_MINUTES);

        // Ngày mặc định = hôm nay
        selectedDate = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN")).format(new Date());

        initViews();
        buildGrid();
        loadBookedSlots();
    }

    private boolean isCourtOpen() {
        int open  = parseHour(court.getOpenTime(),  6);
        int close = parseHour(court.getCloseTime(), 22);
        Calendar now = Calendar.getInstance();
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        return currentHour >= open && currentHour < close;
    }

    private int parseHour(String time, int defaultVal) {
        if (time == null || time.isEmpty()) return defaultVal;
        try { return Integer.parseInt(time.split(":")[0]); }
        catch (Exception e) { return defaultVal; }
    }

    private void initViews() {
        tvSelectedDate      = findViewById(R.id.tvSelectedDate);
        tvTotalHours        = findViewById(R.id.tvTotalHours);
        tvTotalMoney        = findViewById(R.id.tvTotalMoney);
        tvToggleArrow       = findViewById(R.id.tvToggleArrow);
        llCourtHeaders      = findViewById(R.id.llCourtHeaders);
        llGrid              = findViewById(R.id.llGrid);
        panelSummary        = findViewById(R.id.panelSummary);
        layoutSelectedDetails = findViewById(R.id.layoutSelectedDetails);
        hsvHeader           = findViewById(R.id.hsvHeader);
        hsvBody             = findViewById(R.id.hsvBody);
        svBody              = findViewById(R.id.svBody);
        btnNext             = findViewById(R.id.btnNext);

        tvSelectedDate.setText(selectedDate);

        // Sync scroll ngang header ↔ body
        hsvBody.setOnScrollChangeListener((v, scrollX, scrollY, oldX, oldY) ->
                hsvHeader.scrollTo(scrollX, 0));

        // Date picker
        findViewById(R.id.btnPickDate).setOnClickListener(v -> showDatePicker());

        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Toggle summary
        findViewById(R.id.btnToggleSummary).setOnClickListener(v -> toggleSummary());

        // Slider zoom
        ((SeekBar) findViewById(R.id.seekBarZoom)).setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                        // progress 0-4 → courtCount 3-7
                        courtCount = 3 + progress;
                        rebuildGrid();
                    }
                    @Override public void onStartTrackingTouch(SeekBar sb) {}
                    @Override public void onStopTrackingTouch(SeekBar sb) {}
                });

        // Tiếp theo
        btnNext.setOnClickListener(v -> goToConfirm());
    }

    // ─── BUILD GRID ──────────────────────────────────────────────────────────

    private void buildGrid() {
        llCourtHeaders.removeAllViews();
        llGrid.removeAllViews();

        int slotW = dp(slotWidthDp);
        int timeW = dp(timeColWidthDp);
        int rowH  = dp(rowHeightDp);

        // ── Header hàng: ô góc + tên sân ──
        // Ô góc (freeze placeholder)
        View corner = new View(this);
        corner.setLayoutParams(new LinearLayout.LayoutParams(timeW, rowH));
        corner.setBackgroundColor(getColor(R.color.schedule_header_bg));
        llCourtHeaders.addView(corner);

        for (int c = 0; c < courtCount; c++) {
            TextView tvCourt = new TextView(this);
            tvCourt.setLayoutParams(new LinearLayout.LayoutParams(slotW, rowH));
            tvCourt.setText("Sân " + (c + 1));
            tvCourt.setTextSize(13f);
            tvCourt.setTypeface(null, Typeface.BOLD);
            tvCourt.setTextColor(getColor(R.color.booking_dark_green));
            tvCourt.setGravity(Gravity.CENTER);
            tvCourt.setBackgroundColor(getColor(R.color.schedule_header_bg));
            // Đường kẻ phải
            tvCourt.setPadding(0, 0, 1, 0);
            llCourtHeaders.addView(tvCourt);
        }

        // ── Body: mỗi hàng = 1 slot thời gian ──
        for (int s = 0; s < totalSlots; s++) {
            LinearLayout row = new LinearLayout(this);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, rowH));
            row.setOrientation(LinearLayout.HORIZONTAL);

            // Cột thời gian (freeze)
            int totalMinutes = openHour * 60 + s * SLOT_MINUTES;
            int h = totalMinutes / 60;
            int m = totalMinutes % 60;
            String timeLabel = String.format(Locale.getDefault(), "%d:%02d", h, m);

            TextView tvTime = new TextView(this);
            tvTime.setLayoutParams(new LinearLayout.LayoutParams(timeW, rowH));
            tvTime.setText(timeLabel);
            tvTime.setTextSize(11f);
            tvTime.setTextColor(getColor(R.color.text_secondary));
            tvTime.setGravity(Gravity.CENTER);
            tvTime.setBackgroundColor(getColor(R.color.schedule_time_col_bg));
            row.addView(tvTime);

            // Ô sân
            for (int c = 0; c < courtCount; c++) {
                final int slotIdx  = s;
                final int courtIdx = c;
                String key = courtIdx + "_" + slotIdx;

                View cell = new View(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(slotW - 1, rowH - 1);
                lp.setMargins(1, 1, 0, 0);
                cell.setLayoutParams(lp);
                cell.setTag(key);
                updateCellColor(cell, key);

                cell.setOnClickListener(v -> onCellClick(v, courtIdx, slotIdx));
                row.addView(cell);
            }

            llGrid.addView(row);
        }
    }

    private void rebuildGrid() {
        selectedSlots.clear();
        buildGrid();
        reapplyBookedColors();
        updateSummary();
    }

    private void updateCellColor(View cell, String key) {
        if (bookedSlots.contains(key)) {
            cell.setBackgroundColor(getColor(R.color.slot_booked));
        } else if (selectedSlots.contains(key)) {
            cell.setBackgroundColor(getColor(R.color.slot_selected));
        } else {
            cell.setBackgroundColor(getColor(R.color.slot_empty));
        }
    }

    private void reapplyBookedColors() {
        for (int s = 0; s < totalSlots; s++) {
            for (int c = 0; c < courtCount; c++) {
                String key = c + "_" + s;
                View cell = llGrid.findViewWithTag(key);
                if (cell != null) updateCellColor(cell, key);
            }
        }
    }

    // ─── CELL CLICK ──────────────────────────────────────────────────────────

    private void onCellClick(View cell, int courtIdx, int slotIdx) {
        String key = courtIdx + "_" + slotIdx;
        if (bookedSlots.contains(key)) {
            Toast.makeText(this, "Slot này đã được đặt!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedSlots.contains(key)) {
            selectedSlots.remove(key);
        } else {
            selectedSlots.add(key);
        }
        updateCellColor(cell, key);
        updateSummary();
    }

    // ─── SUMMARY ─────────────────────────────────────────────────────────────

    private void updateSummary() {
        if (selectedSlots.isEmpty()) {
            panelSummary.setVisibility(View.GONE);
            return;
        }
        panelSummary.setVisibility(View.VISIBLE);

        // Tính tổng: mỗi slot = SLOT_MINUTES phút
        int totalMinutes = selectedSlots.size() * SLOT_MINUTES;
        int hours = totalMinutes / 60;
        int mins  = totalMinutes % 60;
        double pricePerSlot = court.getPricePerHour() * SLOT_MINUTES / 60.0;
        double totalMoney = selectedSlots.size() * pricePerSlot;

        tvTotalHours.setText(String.format(Locale.getDefault(),
                "Tổng giờ: %dh%02d", hours, mins));
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        tvTotalMoney.setText("Tổng tiền: " + fmt.format(totalMoney) + " đ");

        // Chi tiết từng sân
        layoutSelectedDetails.removeAllViews();
        // Group by court
        Map<Integer, List<Integer>> courtSlots = new HashMap<>();
        for (String key : selectedSlots) {
            String[] parts = key.split("_");
            int c = Integer.parseInt(parts[0]);
            int s = Integer.parseInt(parts[1]);
            courtSlots.computeIfAbsent(c, k -> new ArrayList<>()).add(s);
        }
        for (Map.Entry<Integer, List<Integer>> entry : courtSlots.entrySet()) {
            int c = entry.getKey();
            List<Integer> slots = entry.getValue();
            slots.sort(Integer::compareTo);
            int firstSlot = slots.get(0);
            int lastSlot  = slots.get(slots.size() - 1);
            String startT = slotToTime(firstSlot);
            String endT   = slotToTime(lastSlot + 1);
            double price  = slots.size() * pricePerSlot;

            TextView tv = new TextView(this);
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));
            tv.setText(String.format("Sân %d: %s - %s  |  %s đ",
                    c + 1, startT, endT, fmt.format(price)));
            tv.setTextSize(14f);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextColor(getColor(R.color.white));
            tv.setPadding(dp(16), dp(4), dp(16), dp(4));
            layoutSelectedDetails.addView(tv);
        }
    }

    private String slotToTime(int slotIdx) {
        int totalMin = openHour * 60 + slotIdx * SLOT_MINUTES;
        return String.format(Locale.getDefault(), "%dh%02d", totalMin / 60, totalMin % 60);
    }

    private void toggleSummary() {
        summaryExpanded = !summaryExpanded;
        layoutSelectedDetails.setVisibility(summaryExpanded ? View.VISIBLE : View.GONE);
        tvToggleArrow.setText(summaryExpanded ? "∨" : "∧");
    }

    // ─── LOAD BOOKED SLOTS ───────────────────────────────────────────────────

    private void loadBookedSlots() {
        if (court.getCourtId() == null) return;
        FirebaseFirestore.getInstance().collection("Bookings")
                .whereEqualTo("courtId", court.getCourtId())
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(snap -> {
                    bookedSlots.clear();
                    for (var doc : snap.getDocuments()) {
                        Booking b = doc.toObject(Booking.class);
                        if (b == null) continue;
                        // Map startTime-endTime → slot indices
                        int startSlot = timeToSlot(b.getStartTime());
                        int endSlot   = timeToSlot(b.getEndTime());
                        // courtIndex từ courtName (đơn giản: dùng 0 nếu không có sub-court)
                        int cIdx = 0;
                        for (int s = startSlot; s < endSlot && s < totalSlots; s++) {
                            bookedSlots.add(cIdx + "_" + s);
                        }
                    }
                    reapplyBookedColors();
                });
    }

    private int timeToSlot(String time) {
        if (time == null) return 0;
        try {
            String[] parts = time.split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return (h - openHour) * (60 / SLOT_MINUTES) + m / SLOT_MINUTES;
        } catch (Exception e) { return 0; }
    }

    // ─── DATE PICKER ─────────────────────────────────────────────────────────

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            tvSelectedDate.setText(selectedDate);
            selectedSlots.clear();
            updateSummary();
            loadBookedSlots();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ─── TIẾP THEO ───────────────────────────────────────────────────────────

    private void goToConfirm() {
        if (selectedSlots.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn ít nhất 1 slot!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build danh sách booking items
        Map<Integer, List<Integer>> courtSlots = new HashMap<>();
        for (String key : selectedSlots) {
            String[] parts = key.split("_");
            int c = Integer.parseInt(parts[0]);
            int s = Integer.parseInt(parts[1]);
            courtSlots.computeIfAbsent(c, k -> new ArrayList<>()).add(s);
        }

        ArrayList<String> courtNames = new ArrayList<>();
        ArrayList<String> startTimes = new ArrayList<>();
        ArrayList<String> endTimes   = new ArrayList<>();
        ArrayList<Double> prices     = new ArrayList<>();
        double pricePerSlot = court.getPricePerHour() * SLOT_MINUTES / 60.0;

        for (Map.Entry<Integer, List<Integer>> entry : courtSlots.entrySet()) {
            int c = entry.getKey();
            List<Integer> slots = entry.getValue();
            slots.sort(Integer::compareTo);
            courtNames.add("Sân " + (c + 1));
            startTimes.add(slotToTime(slots.get(0)));
            endTimes.add(slotToTime(slots.get(slots.size() - 1) + 1));
            prices.add(slots.size() * pricePerSlot);
        }

        Intent intent = new Intent(this, BookingConfirmActivity.class);
        intent.putExtra(BookingConfirmActivity.EXTRA_COURT, court);
        intent.putExtra(BookingConfirmActivity.EXTRA_DATE, selectedDate);
        intent.putStringArrayListExtra(BookingConfirmActivity.EXTRA_COURT_NAMES, courtNames);
        intent.putStringArrayListExtra(BookingConfirmActivity.EXTRA_START_TIMES, startTimes);
        intent.putStringArrayListExtra(BookingConfirmActivity.EXTRA_END_TIMES, endTimes);
        double[] pricesArr = new double[prices.size()];
        for (int i = 0; i < prices.size(); i++) pricesArr[i] = prices.get(i);
        intent.putExtra(BookingConfirmActivity.EXTRA_PRICES, pricesArr);
        startActivity(intent);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}
