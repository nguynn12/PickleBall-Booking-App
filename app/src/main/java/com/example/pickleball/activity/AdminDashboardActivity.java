package com.example.pickleball.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.activity.court.AddCourtActivity;
import com.example.pickleball.adapter.BookingAdapter;
import com.example.pickleball.model.Booking;
import com.example.pickleball.utils.FirebaseHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {
    private RecyclerView rvBookings;
    private BookingAdapter adapter;
    private List<Booking> bookingList;
    private FirebaseFirestore db;
    private FirebaseHelper firebaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        rvBookings = findViewById(R.id.rvBookings);
        rvBookings.setLayoutManager(new LinearLayoutManager(this));

        bookingList = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        firebaseHelper = new FirebaseHelper();

        // Initialize adapter with empty list
        adapter = new BookingAdapter(bookingList);
        rvBookings.setAdapter(adapter);

        loadBookingsToday();

        findViewById(R.id.fabAddCourt).setOnClickListener(v -> {
            startActivity(new Intent(this, AddCourtActivity.class));
        });

        // Click on the title to add sample data for testing

    }


    private void loadBookingsToday() {
        db.collection("Bookings")
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Lỗi tải dữ liệu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        bookingList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            Booking b = doc.toObject(Booking.class);
                            if (b != null) {
                                bookingList.add(b);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }
}
