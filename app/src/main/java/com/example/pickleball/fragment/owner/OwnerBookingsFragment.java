package com.example.pickleball.fragment.owner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.BookingManageAdapter;
import com.example.pickleball.model.Booking;
import com.example.pickleball.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class OwnerBookingsFragment extends Fragment {

    private RecyclerView rvBookings;
    private BookingManageAdapter adapter;
    private final List<Booking> allBookings = new ArrayList<>();
    private final List<Booking> displayList = new ArrayList<>();
    private TextView tvEmpty, tabAll, tabPending, tabConfirmed;
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_owner_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvBookings   = view.findViewById(R.id.rvBookings);
        tvEmpty      = view.findViewById(R.id.tvEmpty);
        tabAll       = view.findViewById(R.id.tabAll);
        tabPending   = view.findViewById(R.id.tabPending);
        tabConfirmed = view.findViewById(R.id.tabConfirmed);

        rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BookingManageAdapter(displayList, new BookingManageAdapter.OnActionListener() {
            @Override public void onConfirm(Booking b) { ownerCancelBooking(b); }
            @Override public void onReject(Booking b)  { ownerCancelBooking(b); }
        });
        rvBookings.setAdapter(adapter);

        tabAll.setOnClickListener(v -> applyFilter("all", tabAll, tabPending, tabConfirmed));
        tabPending.setOnClickListener(v -> applyFilter(com.example.pickleball.utils.Constants.BOOKING_STATUS_AWAITING_PAYMENT,
                tabPending, tabAll, tabConfirmed));
        tabConfirmed.setOnClickListener(v -> applyFilter(com.example.pickleball.utils.Constants.BOOKING_STATUS_CONFIRMED,
                tabConfirmed, tabAll, tabPending));

        loadOwnerBookings();
    }

    private void loadOwnerBookings() {
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
        selected.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        u1.setBackgroundResource(R.drawable.bg_search_bar);
        u1.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        u2.setBackgroundResource(R.drawable.bg_search_bar);
        u2.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
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
        if (tvEmpty != null) tvEmpty.setVisibility(displayList.isEmpty() ? View.VISIBLE : View.GONE);
        if (rvBookings != null) rvBookings.setVisibility(displayList.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void ownerCancelBooking(Booking booking) {
        if (booking.getBookingId() == null) return;
        String customerLabel = booking.getCustomerName() != null ? booking.getCustomerName() : "khách";
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Hủy lịch đặt")
                .setMessage("Hủy lịch của " + customerLabel + "?\nKhách sẽ được hoàn 100% tiền cọc (nếu đã thanh toán).")
                .setPositiveButton("Xác nhận hủy", (d, w) -> {
                    java.util.Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("status",       com.example.pickleball.utils.Constants.BOOKING_STATUS_CANCELLED_BY_OWNER);
                    updates.put("cancelledBy",  "owner");
                    updates.put("cancelledAt",  System.currentTimeMillis());
                    updates.put("refundStatus", com.example.pickleball.utils.Constants.REFUND_STATUS_PENDING);
                    updates.put("refundAmount", booking.getDepositAmount());
                    FirebaseFirestore.getInstance().collection("Bookings")
                            .document(booking.getBookingId())
                            .update(updates)
                            .addOnSuccessListener(v -> {
                                Toast.makeText(requireContext(), "Đã hủy lịch!", Toast.LENGTH_SHORT).show();
                                NotificationHelper.sendBookingCancelledByOwner(
                                        booking.getUserId(),
                                        booking.getCourtName() != null ? booking.getCourtName() : "Sân",
                                        booking.getDate() != null ? booking.getDate() : "",
                                        booking.getBookingId());
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Giữ lại", null)
                .show();
    }
}
