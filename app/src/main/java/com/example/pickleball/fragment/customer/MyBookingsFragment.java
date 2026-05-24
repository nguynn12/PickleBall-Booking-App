package com.example.pickleball.fragment.customer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.BookingAdapter;
import com.example.pickleball.model.Booking;
import com.example.pickleball.utils.Constants;
import com.example.pickleball.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MyBookingsFragment extends Fragment {

    private RecyclerView rvBookings;
    private BookingAdapter adapter;
    private final List<Booking> allBookings  = new ArrayList<>();
    private final List<Booking> displayList  = new ArrayList<>();
    private TextView tvEmpty, tabAll, tabPending, tabConfirmed, tabCancelled;
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_bookings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvBookings   = view.findViewById(R.id.rvBookings);
        tvEmpty      = view.findViewById(R.id.tvEmpty);
        tabAll       = view.findViewById(R.id.tabAll);
        tabPending   = view.findViewById(R.id.tabPending);
        tabConfirmed = view.findViewById(R.id.tabConfirmed);
        tabCancelled = view.findViewById(R.id.tabCancelled);

        rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BookingAdapter(displayList, booking -> {
            String status = booking.getStatus();
            if (Constants.BOOKING_STATUS_AWAITING_PAYMENT.equals(status)
                    || Constants.BOOKING_STATUS_CONFIRMED.equals(status)) {
                showCancelDialog(booking);
            }
        });
        rvBookings.setAdapter(adapter);

        tabAll.setOnClickListener(v       -> applyFilter("all",
                tabAll, tabPending, tabConfirmed, tabCancelled));
        tabPending.setOnClickListener(v   -> applyFilter(Constants.BOOKING_STATUS_AWAITING_PAYMENT,
                tabPending, tabAll, tabConfirmed, tabCancelled));
        tabConfirmed.setOnClickListener(v -> applyFilter(Constants.BOOKING_STATUS_CONFIRMED,
                tabConfirmed, tabAll, tabPending, tabCancelled));
        tabCancelled.setOnClickListener(v -> applyFilter("cancelled",
                tabCancelled, tabAll, tabPending, tabConfirmed));

        loadMyBookings();
    }

    private void loadMyBookings() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("Bookings")
                .whereEqualTo("userId", uid)
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

    private void applyFilter(String filter, TextView selected, TextView... others) {
        currentFilter = filter;
        selected.setBackgroundResource(R.drawable.bg_button_primary);
        selected.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white));
        for (TextView t : others) {
            t.setBackgroundResource(R.drawable.bg_search_bar);
            t.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary));
        }
        filterAndShow();
    }

    private void filterAndShow() {
        displayList.clear();
        for (Booking b : allBookings) {
            String s = b.getStatus();
            boolean matches = "all".equals(currentFilter)
                    || currentFilter.equals(s)
                    || ("cancelled".equals(currentFilter) && s != null && s.startsWith("cancelled"));
            if (matches) displayList.add(b);
        }
        adapter.notifyDataSetChanged();
        boolean empty = displayList.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvBookings.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void showCancelDialog(Booking booking) {
        long playTimeMs  = parseBookingDateTime(booking.getDate(), booking.getStartTime());
        long msUntilPlay = playTimeMs - System.currentTimeMillis();
        boolean hasPaid   = Constants.PAYMENT_STATUS_PAID.equals(booking.getPaymentStatus());
        boolean canRefund = hasPaid && msUntilPlay >= Constants.FREE_CANCEL_WINDOW_MS;

        String msg;
        if (!hasPaid) {
            msg = "Hủy lịch đặt sân \"" + booking.getCourtName()
                    + "\" vào ngày " + booking.getDate() + "?\n(Chưa thanh toán — không mất phí)";
        } else if (canRefund) {
            msg = "Hủy lịch đặt sân \"" + booking.getCourtName()
                    + "\" vào ngày " + booking.getDate() + "?\n✅ Bạn sẽ được hoàn 100% tiền cọc.";
        } else {
            msg = "Hủy lịch đặt sân \"" + booking.getCourtName()
                    + "\" vào ngày " + booking.getDate()
                    + "?\n⚠️ Còn dưới 2 tiếng trước giờ chơi — bạn sẽ MẤT tiền cọc.";
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Hủy đặt sân")
                .setMessage(msg)
                .setPositiveButton("Xác nhận hủy", (d, w) -> cancelBooking(booking, canRefund && hasPaid))
                .setNegativeButton("Giữ lại", null)
                .show();
    }

    private void cancelBooking(Booking booking, boolean shouldRefund) {
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status",       Constants.BOOKING_STATUS_CANCELLED_BY_USER);
        updates.put("cancelledBy",  "user");
        updates.put("cancelledAt",  System.currentTimeMillis());
        updates.put("refundStatus", shouldRefund ? Constants.REFUND_STATUS_PENDING : Constants.REFUND_STATUS_NA);
        updates.put("refundAmount", shouldRefund ? booking.getDepositAmount() : 0);

        FirebaseFirestore.getInstance()
                .collection("Bookings").document(booking.getBookingId())
                .update(updates)
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Đã hủy lịch đặt sân!", Toast.LENGTH_SHORT).show();
                    if (booking.getOwnerId() != null && !booking.getOwnerId().isEmpty()) {
                        NotificationHelper.sendBookingCancelledToOwner(
                                booking.getOwnerId(),
                                booking.getCourtName() != null ? booking.getCourtName() : "Sân",
                                booking.getDate() != null ? booking.getDate() : "",
                                booking.getBookingId());
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private long parseBookingDateTime(String date, String startTime) {
        try {
            String[] d = date.split("/");   // dd/MM/yyyy
            String[] t = (startTime != null ? startTime : "00:00").split(":");
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(Integer.parseInt(d[2]), Integer.parseInt(d[1]) - 1, Integer.parseInt(d[0]),
                    Integer.parseInt(t[0]), Integer.parseInt(t[1]), 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return 0L;
        }
    }
}
