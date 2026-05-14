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
        // Click vào booking → hỏi hủy nếu còn pending
        adapter = new BookingAdapter(displayList, booking -> {
            if ("pending".equals(booking.getStatus())) {
                showCancelDialog(booking);
            }
        });
        rvBookings.setAdapter(adapter);

        tabAll.setOnClickListener(v       -> applyFilter("all",       tabAll,       tabPending, tabConfirmed, tabCancelled));
        tabPending.setOnClickListener(v   -> applyFilter("pending",   tabPending,   tabAll,     tabConfirmed, tabCancelled));
        tabConfirmed.setOnClickListener(v -> applyFilter("confirmed", tabConfirmed, tabAll,     tabPending,   tabCancelled));
        tabCancelled.setOnClickListener(v -> applyFilter("cancelled", tabCancelled, tabAll,     tabPending,   tabConfirmed));

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
            if ("all".equals(currentFilter) || currentFilter.equals(b.getStatus())) {
                displayList.add(b);
            }
        }
        adapter.notifyDataSetChanged();
        boolean empty = displayList.isEmpty();
        tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvBookings.setVisibility(empty ? View.GONE : View.VISIBLE);

        // Hint khi tab pending: tap để hủy
        if ("pending".equals(currentFilter) && !empty) {
            tvEmpty.setVisibility(View.GONE);
        }
    }

    private void showCancelDialog(Booking booking) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Hủy đặt sân")
                .setMessage("Bạn có chắc muốn hủy lịch đặt sân \"" + booking.getCourtName() + "\" vào ngày " + booking.getDate() + "?")
                .setPositiveButton("Hủy đặt", (d, w) -> cancelBooking(booking))
                .setNegativeButton("Giữ lại", null)
                .show();
    }

    private void cancelBooking(Booking booking) {
        FirebaseFirestore.getInstance()
                .collection("Bookings")
                .document(booking.getBookingId())
                .update("status", "cancelled")
                .addOnSuccessListener(v -> {
                    Toast.makeText(requireContext(), "Đã hủy lịch đặt sân!", Toast.LENGTH_SHORT).show();
                    // Gửi thông báo cho owner
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
}
