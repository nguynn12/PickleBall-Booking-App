package com.example.pickleball.activity.booking;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.BookingManageAdapter;
import com.example.pickleball.model.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class OwnerBookingManageActivity extends AppCompatActivity {

    private RecyclerView rvBookings;
    private BookingManageAdapter adapter;
    private final List<Booking> allBookings = new ArrayList<>();
    private final List<Booking> displayList = new ArrayList<>();
    private TextView tvEmpty, tabAll, tabPending, tabConfirmed;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_owner_booking_manage);

        rvBookings   = findViewById(R.id.rvBookings);
        tvEmpty      = findViewById(R.id.tvEmpty);
        tabAll       = findViewById(R.id.tabAll);
        tabPending   = findViewById(R.id.tabPending);
        tabConfirmed = findViewById(R.id.tabConfirmed);

        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingManageAdapter(displayList, new BookingManageAdapter.OnActionListener() {
            @Override
            public void onConfirm(Booking booking) { updateStatus(booking, "confirmed"); }
            @Override
            public void onReject(Booking booking)  { updateStatus(booking, "rejected"); }
        });
        rvBookings.setAdapter(adapter);

        tabAll.setOnClickListener(v -> applyFilter("all", tabAll, tabPending, tabConfirmed));
        tabPending.setOnClickListener(v -> applyFilter("pending", tabPending, tabAll, tabConfirmed));
        tabConfirmed.setOnClickListener(v -> applyFilter("confirmed", tabConfirmed, tabAll, tabPending));

        loadBookings();
    }

    private void loadBookings() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("Bookings")
                .whereEqualTo("ownerId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    allBookings.clear();
                    for (var doc : value.getDocuments()) {
                        Booking b = doc.toObject(Booking.class);
                        if (b != null) {
                            if (b.getBookingId() == null) b.setBookingId(doc.getId());
                            allBookings.add(b);
                        }
                    }
                    filterAndShow();
                });
    }

    private void applyFilter(String filter, TextView selected, TextView u1, TextView u2) {
        currentFilter = filter;
        selected.setBackgroundResource(R.drawable.bg_button_primary);
        selected.setTextColor(ContextCompat.getColor(this, android.R.color.white));
        u1.setBackgroundResource(R.drawable.bg_search_bar);
        u1.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        u2.setBackgroundResource(R.drawable.bg_search_bar);
        u2.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
        filterAndShow();
    }

    private void filterAndShow() {
        displayList.clear();
        for (Booking b : allBookings) {
            if ("all".equals(currentFilter) || currentFilter.equals(b.getStatus())) {
                displayList.add(b);
            }
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);
        rvBookings.setVisibility(displayList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void updateStatus(Booking booking, String newStatus) {
        // Ưu tiên lấy bookingId từ object, nếu không có thì không thể update
        String bId = booking.getBookingId();

        if (bId == null || bId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID đơn hàng!", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseFirestore.getInstance().collection("Bookings")
                .document(bId)
                .update("status", newStatus)
                .addOnSuccessListener(v -> {
                    String msg = "confirmed".equals(newStatus) ? "✅ Đã xác nhận đơn!" : "❌ Đã từ chối đơn!";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    // UI sẽ tự động cập nhật thông qua addSnapshotListener trong loadBookings()
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
