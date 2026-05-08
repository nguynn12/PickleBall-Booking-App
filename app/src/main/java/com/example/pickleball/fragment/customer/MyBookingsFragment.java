package com.example.pickleball.fragment.customer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.BookingAdapter;
import com.example.pickleball.model.Booking;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MyBookingsFragment extends Fragment {

    private RecyclerView rvBookings;
    private BookingAdapter adapter;
    private final List<Booking> bookingList = new ArrayList<>();
    private TextView tvEmpty;

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

        rvBookings = view.findViewById(R.id.rvBookings);
        tvEmpty    = view.findViewById(R.id.tvEmpty);

        rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BookingAdapter(bookingList);
        rvBookings.setAdapter(adapter);

        loadMyBookings();
    }

    private void loadMyBookings() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("Bookings")
                .whereEqualTo("userId", uid)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    bookingList.clear();
                    for (var doc : value.getDocuments()) {
                        Booking b = doc.toObject(Booking.class);
                        if (b != null) bookingList.add(b);
                    }
                    adapter.notifyDataSetChanged();
                    tvEmpty.setVisibility(bookingList.isEmpty() ? View.VISIBLE : View.GONE);
                    rvBookings.setVisibility(bookingList.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }
}
