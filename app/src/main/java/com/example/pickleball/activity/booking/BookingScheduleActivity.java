package com.example.pickleball.activity.booking;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.SelectedSlotAdapter;
import com.example.pickleball.model.Court;
import com.example.pickleball.model.SelectedSlot;
import com.example.pickleball.model.SubCourt;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookingScheduleActivity extends AppCompatActivity {

    public static final String EXTRA_COURT        = "EXTRA_COURT";
    public static final String EXTRA_SELECTED_DATE = "EXTRA_SELECTED_DATE";
    public static final String EXTRA_SELECTED_SLOTS = "EXTRA_SELECTED_SLOTS";

    // Kích thước ô grid (dp)
    private static final int CELL_W_DP = 52;
    private static final int CELL_H_DP = 44;
    private static final int NAME_W_DP = 80;

    private Court court;
    private String selectedDate = "";
    private List<SubCourt> subCourts = new ArrayList<>();

    // Map key="subCourtId_slotIndex" → SelectedSlot (các ô đã chọn)
    private final Map<String, SelectedSlot> selectedMap = new HashMap<>();
    // Map key="subCourtId_slotIndex" → true nếu đã bị đặt
    private final Map<String, Boolean> bookedMap = new HashMap<>();
    // Map key="subCourtId_slotIndex" → View ô grid
    private final Map<String, View> cellViewMap = new HashMap<>();

    private List<String> timeSlots = new ArrayList<>(); // ["07:00","07:30","08:00"...]
    private int openH, closeH;
    private double pricePerSlot; // pricePerHour / 2

    private TextView tvScheduleDate, tvTotalHours, tvTotalMoney;
    private LinearLayout colSubCourtNames, rowTimeHeader, rowsContainer;
    private LinearLayout bottomPanel, layoutSlotDetails, btnTogglePanel;
    private ImageView ivToggleArrow;
    private RecyclerView rvSelectedSlots;
    private SelectedSlotAdapter slotAdapter;
    private final List<SelectedSlot> selectedSlotList = new ArrayList<>();
    private boolean isPanelExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_schedule);

        court = (Court) getIntent().getSerializableExtra(EXTRA_COURT);
        if (court == null) { finish(); return; }

        openH  = parseHour(court.getOpenTime(),  6);
        closeH = parseHour(court.getCloseTime(), 22);
        pricePerSlot = court.getPricePerHour() / 2.0; // 30 phút = nửa giờ

        // Build time slots (30 phút / slot)
        for (int h = openH; h < closeH; h++) {
            timeSlots.add(String.format(Locale.getDefault(), "%02d:00", h));
            timeSlots.add(String.format(Locale.getDefault(), "%02d:30", h));
        }

        initViews();
        setupListeners();

        // Ngày mặc định = hôm nay
        Calendar today = Calendar.getInstance();
        selectedDate = new SimpleDateFormat("dd/MM/yyyy", new Locale("vi", "VN")).format(today.getTime());
        tvScheduleDate.setText(selectedDate);

        loadSubCourtsAndSchedule();
    }

    private void initViews() {
        tvScheduleDate    = findViewById(R.id.tvScheduleDate);
        tvTotalHours      = findViewById(R.id.tvTotalHours);
        tvTotalMoney      = findViewById(R.id.tvTotalMoney);
        colSubCourtNames  = findViewById(R.id.colSubCourtNames);
        rowTimeHeader     = findViewById(R.id.rowTimeHeader);
        rowsContainer     = findViewById(R.id.rowsContainer);
        bottomPanel       = findViewById(R.id.bottomPanel);
        layoutSlotDetails = findViewById(R.id.layoutSlotDetails);
        btnTogglePanel    = findViewById(R.id.btnTogglePanel);
        ivToggleArrow     = findViewById(R.id.ivToggleArrow);
        rvSelectedSlots   = findViewById(R.id.rvSelectedSlots);

        rvSelectedSlots.setLayoutManager(new LinearLayoutManager(this));
        slotAdapter = new SelectedSlotAdapter(selectedSlotList);
        rvSelectedSlots.setAdapter(slotAdapter);

        ((ImageView) findViewById(R.id.btnBack)).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        // Chọn ngày
        findViewById(R.id.btnPickScheduleDate).setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            DatePickerDialog dlg = new DatePickerDialog(this, (view, year, month, day) -> {
                selectedDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
                tvScheduleDate.setText(selectedDate);
                selectedMap.clear();
                loadSubCourtsAndSchedule();
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
            dlg.getDatePicker().setMinDate(cal.getTimeInMillis());
            dlg.show();
        });

        // Toggle bottom panel
        btnTogglePanel.setOnClickListener(v -> {
            isPanelExpanded = !isPanelExpanded;
            layoutSlotDetails.setVisibility(isPanelExpanded ? View.VISIBLE : View.GONE);
            ivToggleArrow.setImageResource(isPanelExpanded
                    ? android.R.drawable.arrow_down_float
                    : android.R.drawable.arrow_up_float);
        });

        // Nút Tiếp theo
        ((MaterialButton) findViewById(R.id.btnNext)).setOnClickListener(v -> {
            if (selectedMap.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất 1 ô!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, BookingConfirmActivity.class);
            intent.putExtra(BookingConfirmActivity.EXTRA_COURT, court);
            intent.putExtra(BookingConfirmActivity.EXTRA_DATE, selectedDate);
            intent.putExtra(BookingConfirmActivity.EXTRA_SLOTS,
                    (Serializable) new ArrayList<>(selectedMap.values()));
            startActivity(intent);
        });
    }

    // ─── LOAD DỮ LIỆU ────────────────────────────────────────────────────────

    private void loadSubCourtsAndSchedule() {
        if (court.getCourtId() == null) {
            // Không có courtId → tạo sân con mặc định
            subCourts.clear();
            subCourts.add(new SubCourt(court.getCourtId(), court.getCourtName() != null ? court.getCourtName() : "Sân 1", 0));
            loadBookedSlots();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("Courts").document(court.getCourtId())
                .collection("SubCourts")
                .orderBy("sortOrder")
                .get()
                .addOnSuccessListener(snap -> {
                    subCourts.clear();
                    for (var doc : snap.getDocuments()) {
                        SubCourt sc = doc.toObject(SubCourt.class);
                        if (sc != null) {
                            if (sc.getSubCourtId() == null) sc.setSubCourtId(doc.getId());
                            subCourts.add(sc);
                        }
                    }
                    // Nếu chưa có sân con → tạo mặc định 1 sân
                    if (subCourts.isEmpty()) {
                        SubCourt def = new SubCourt(court.getCourtId(),
                                court.getCourtName() != null ? court.getCourtName() : "Sân 1", 0);
                        def.setSubCourtId("default");
                        subCourts.add(def);
                    }
                    loadBookedSlots();
                })
                .addOnFailureListener(e -> {
                    // Fallback: 1 sân con mặc định
                    subCourts.clear();
                    SubCourt def = new SubCourt(court.getCourtId(),
                            court.getCourtName() != null ? court.getCourtName() : "Sân 1", 0);
                    def.setSubCourtId("default");
                    subCourts.add(def);
                    loadBookedSlots();
                });
    }

    private void loadBookedSlots() {
        bookedMap.clear();
        if (court.getCourtId() == null) {
            buildGrid();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection("Bookings")
                .whereEqualTo("courtId", court.getCourtId())
                .whereEqualTo("date", selectedDate)
                .get()
                .addOnSuccessListener(snap -> {
                    for (var doc : snap.getDocuments()) {
                        String status = doc.getString("status");
                        // Chỉ block khi đã thanh toán — "awaiting_payment" không block
                        if (!"confirmed".equals(status)) continue;

                        String subCourtId = doc.getString("subCourtId");
                        String startTime  = doc.getString("startTime");
                        String endTime    = doc.getString("endTime");
                        if (startTime == null || endTime == null) continue;

                        // Đánh dấu tất cả slot trong khoảng bị đặt
                        int startMin = toMinutes(startTime);
                        int endMin   = toMinutes(endTime);
                        for (int i = 0; i < timeSlots.size(); i++) {
                            int slotMin = toMinutes(timeSlots.get(i));
                            if (slotMin >= startMin && slotMin < endMin) {
                                String key = (subCourtId != null ? subCourtId : "default") + "_" + i;
                                bookedMap.put(key, true);
                            }
                        }
                    }
                    buildGrid();
                })
                .addOnFailureListener(e -> buildGrid());
    }

    // ─── BUILD GRID ──────────────────────────────────────────────────────────

    private void buildGrid() {
        colSubCourtNames.removeAllViews();
        rowTimeHeader.removeAllViews();
        rowsContainer.removeAllViews();
        cellViewMap.clear();

        int cellW = dp(CELL_W_DP);
        int cellH = dp(CELL_H_DP);
        int nameW = dp(NAME_W_DP);

        // ── Header giờ ──
        // Placeholder đầu (align với cột tên)
        View placeholder = new View(this);
        placeholder.setLayoutParams(new LinearLayout.LayoutParams(0, cellH));
        rowTimeHeader.addView(placeholder);

        for (int i = 0; i < timeSlots.size(); i++) {
            // Chỉ hiển thị giờ chẵn (xx:00), slot 30 phút hiển thị nhỏ hơn
            String slot = timeSlots.get(i);
            boolean isHour = slot.endsWith(":00");

            TextView tv = new TextView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cellW, cellH);
            tv.setLayoutParams(lp);
            tv.setText(isHour ? slot : "");
            tv.setTextSize(10f);
            tv.setTextColor(Color.parseColor("#555555"));
            tv.setGravity(Gravity.CENTER);
            tv.setBackgroundColor(Color.parseColor("#E0F2E9"));

            // Đường kẻ dọc
            tv.setPadding(0, 0, 1, 0);
            rowTimeHeader.addView(tv);
        }

        // ── Cột tên sân con + rows ──
        // Header rỗng đầu cột tên
        View headerEmpty = new View(this);
        headerEmpty.setLayoutParams(new LinearLayout.LayoutParams(nameW, dp(32)));
        headerEmpty.setBackgroundColor(Color.parseColor("#E0F2E9"));
        colSubCourtNames.addView(headerEmpty);

        for (int r = 0; r < subCourts.size(); r++) {
            SubCourt sc = subCourts.get(r);

            // Tên sân con (cột trái)
            TextView tvName = new TextView(this);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(nameW, cellH);
            tvName.setLayoutParams(nameLp);
            tvName.setText(sc.getName());
            tvName.setTextSize(11f);
            tvName.setTextColor(Color.parseColor("#333333"));
            tvName.setGravity(Gravity.CENTER);
            tvName.setTypeface(null, Typeface.BOLD);
            tvName.setBackgroundColor(r % 2 == 0 ? Color.WHITE : Color.parseColor("#F5FFF8"));
            tvName.setPadding(4, 0, 4, 0);
            colSubCourtNames.addView(tvName);

            // Row các ô slot
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, cellH));

            for (int c = 0; c < timeSlots.size(); c++) {
                String key = sc.getSubCourtId() + "_" + c;
                boolean isBooked   = Boolean.TRUE.equals(bookedMap.get(key));
                boolean isSelected = selectedMap.containsKey(key);

                View cell = makeCellView(sc, c, key, isBooked, isSelected, cellW, cellH);
                row.addView(cell);
                cellViewMap.put(key, cell);
            }
            rowsContainer.addView(row);
        }

        updateBottomPanel();
    }

    private View makeCellView(SubCourt sc, int slotIdx, String key,
                               boolean isBooked, boolean isSelected, int cellW, int cellH) {
        View cell = new View(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(cellW - 1, cellH - 1);
        lp.setMargins(0, 0, 1, 1);
        cell.setLayoutParams(lp);

        if (isBooked) {
            cell.setBackgroundColor(Color.parseColor("#FF6B6B")); // đỏ = đã đặt
            cell.setClickable(false);
        } else if (isSelected) {
            cell.setBackgroundColor(Color.parseColor("#A8E6CF")); // xanh nhạt = đã chọn
            cell.setClickable(true);
        } else {
            cell.setBackgroundColor(Color.WHITE); // trắng = trống
            cell.setClickable(true);
        }

        if (!isBooked) {
            cell.setOnClickListener(v -> toggleCell(sc, slotIdx, key, cell));
        }
        return cell;
    }

    private void toggleCell(SubCourt sc, int slotIdx, String key, View cell) {
        if (selectedMap.containsKey(key)) {
            // Bỏ chọn
            selectedMap.remove(key);
            cell.setBackgroundColor(Color.WHITE);
        } else {
            // Chọn
            String startTime = timeSlots.get(slotIdx);
            String endTime;
            if (slotIdx + 1 < timeSlots.size()) {
                endTime = timeSlots.get(slotIdx + 1);
            } else {
                endTime = String.format(Locale.getDefault(), "%02d:00", closeH);
            }
            SelectedSlot slot = new SelectedSlot(
                    sc.getSubCourtId(), sc.getName(),
                    slotIdx, startTime, endTime, pricePerSlot);
            selectedMap.put(key, slot);
            cell.setBackgroundColor(Color.parseColor("#A8E6CF"));
        }
        updateBottomPanel();
    }

    private void updateBottomPanel() {
        // Tổng số slot × 30 phút
        int totalSlots = selectedMap.size();
        double totalMinutes = totalSlots * 30.0;
        double totalHours   = totalMinutes / 60.0;
        double totalMoney   = totalSlots * pricePerSlot;

        String hoursStr;
        if (totalHours == Math.floor(totalHours)) {
            hoursStr = String.format(Locale.getDefault(), "%.0fh", totalHours);
        } else {
            int h = (int) totalHours;
            int m = (int) ((totalHours - h) * 60);
            hoursStr = h + "h" + m;
        }

        tvTotalHours.setText("Tổng giờ: " + hoursStr);
        NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
        tvTotalMoney.setText("Tổng tiền: " + fmt.format(totalMoney) + " đ");

        // Cập nhật danh sách slot đã chọn (sắp xếp theo sân con + giờ)
        selectedSlotList.clear();
        selectedSlotList.addAll(selectedMap.values());
        selectedSlotList.sort((a, b) -> {
            int cmp = a.getSubCourtName().compareTo(b.getSubCourtName());
            return cmp != 0 ? cmp : Integer.compare(a.getSlotIndex(), b.getSlotIndex());
        });
        slotAdapter.notifyDataSetChanged();
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private int parseHour(String t, int def) {
        if (t == null || t.isEmpty()) return def;
        try { return Integer.parseInt(t.split(":")[0]); }
        catch (Exception e) { return def; }
    }

    private int toMinutes(String time) {
        if (time == null || !time.contains(":")) return 0;
        try {
            String[] p = time.split(":");
            return Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]);
        } catch (Exception e) { return 0; }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
