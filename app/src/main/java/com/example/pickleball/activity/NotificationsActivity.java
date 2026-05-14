package com.example.pickleball.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.pickleball.R;
import com.example.pickleball.adapter.NotificationAdapter;
import com.example.pickleball.model.Notification;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private final List<Notification> notifList = new ArrayList<>();
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        rvNotifications = findViewById(R.id.rvNotifications);
        tvEmpty         = findViewById(R.id.tvEmpty);

        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(notifList, notif -> markAsRead(notif));
        rvNotifications.setAdapter(adapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMarkAllRead).setOnClickListener(v -> markAllRead());

        loadNotifications();
    }

    private void loadNotifications() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore.getInstance()
                .collection("Notifications")
                .whereEqualTo("userId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (snap == null) return;
                    notifList.clear();
                    for (var doc : snap.getDocuments()) {
                        Notification n = doc.toObject(Notification.class);
                        if (n != null) {
                            if (n.getNotifId() == null) n.setNotifId(doc.getId());
                            notifList.add(n);
                        }
                    }
                    adapter.notifyDataSetChanged();
                    boolean empty = notifList.isEmpty();
                    tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                    rvNotifications.setVisibility(empty ? View.GONE : View.VISIBLE);
                });
    }

    private void markAsRead(Notification notif) {
        if (notif.isRead() || notif.getNotifId() == null) return;
        FirebaseFirestore.getInstance()
                .collection("Notifications")
                .document(notif.getNotifId())
                .update("read", true)
                .addOnSuccessListener(v -> {
                    notif.setRead(true);
                    adapter.notifyDataSetChanged();
                });
    }

    private void markAllRead() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        for (Notification n : notifList) {
            if (!n.isRead() && n.getNotifId() != null) {
                db.collection("Notifications").document(n.getNotifId())
                        .update("read", true);
                n.setRead(true);
            }
        }
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
    }
}
