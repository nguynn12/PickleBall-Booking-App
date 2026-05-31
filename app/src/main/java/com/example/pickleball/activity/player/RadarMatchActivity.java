package com.example.pickleball.activity.player;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import com.example.pickleball.R;
import com.example.pickleball.model.PlayerSession;
import com.example.pickleball.view.RadarView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class RadarMatchActivity extends AppCompatActivity {

    private static final long SESSION_TIMEOUT_MS = 5 * 60 * 1000L;

    private FirebaseFirestore db;
    private String currentUserId;
    private String sessionDocId;
    private ListenerRegistration sessionListener;
    private ListenerRegistration queueListener;

    private RadarView radarView;
    private TextView tvTimer, tvStatus;
    private MaterialButton btnCancel;

    private CountDownTimer countUpTimer;
    private long elapsedSeconds = 0;
    private boolean matched = false;
    private boolean matchInProgress = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar_match);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();

        radarView = findViewById(R.id.radarView);
        tvTimer = findViewById(R.id.tvTimer);
        tvStatus = findViewById(R.id.tvStatus);
        btnCancel = findViewById(R.id.btnCancel);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> cancelSearch());
        btnCancel.setOnClickListener(v -> cancelSearch());

        startTimer();
        createSearchSession();
    }

    private void createSearchSession() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            createSearchSessionWithLocation(0, 0);
            return;
        }

        client.getLastLocation()
                .addOnSuccessListener(location -> {
                    double lat = location != null ? location.getLatitude() : 0;
                    double lng = location != null ? location.getLongitude() : 0;
                    createSearchSessionWithLocation(lat, lng);
                })
                .addOnFailureListener(e -> createSearchSessionWithLocation(0, 0));
    }

    private void createSearchSessionWithLocation(double lat, double lng) {
        db.collection("Users").document(currentUserId).get()
                .addOnSuccessListener(doc -> {
                    long now = System.currentTimeMillis();
                    PlayerSession session = new PlayerSession();
                    session.setUserId(currentUserId);
                    session.setUserName(doc.getString("fullName"));
                    session.setAvatarUrl(doc.getString("avatarUrl"));
                    session.setSkillLevel(doc.getString("skillLevel"));
                    session.setLat(lat);
                    session.setLng(lng);
                    session.setStatus("searching");
                    session.setCreatedAt(now);
                    session.setExpiresAt(now + SESSION_TIMEOUT_MS);

                    db.collection("PlayerSessions").add(session)
                            .addOnSuccessListener(ref -> {
                                sessionDocId = ref.getId();
                                ref.update("sessionId", sessionDocId);
                                listenForMatch();
                                listenForQueue();
                                tryMatchWithOther();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lỗi tạo phiên: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Không tải được thông tin người dùng", Toast.LENGTH_SHORT).show());
    }

    private void listenForMatch() {
        sessionListener = db.collection("PlayerSessions")
                .document(sessionDocId)
                .addSnapshotListener((snap, err) -> {
                    if (snap == null || !snap.exists()) return;
                    String status = snap.getString("status");
                    String matchedWith = snap.getString("matchedWith");

                    if ("matched".equals(status) && matchedWith != null && !matched) {
                        matched = true;
                        openMatchFound(matchedWith);
                    }
                });
    }

    private void listenForQueue() {
        queueListener = db.collection("PlayerSessions")
                .whereEqualTo("status", "searching")
                .addSnapshotListener((snap, err) -> {
                    if (err != null) return;
                    if (snap != null && !matched && !matchInProgress) {
                        tryMatchWithOther();
                    }
                });
    }

    private void tryMatchWithOther() {
        if (sessionDocId == null || matched || matchInProgress) return;

        db.collection("PlayerSessions")
                .whereEqualTo("status", "searching")
                .get()
                .addOnSuccessListener(snap -> {
                    if (matched || matchInProgress) return;

                    long now = System.currentTimeMillis();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String uid = doc.getString("userId");
                        Long expiresAt = doc.getLong("expiresAt");

                        if (uid == null || uid.equals(currentUserId)) continue;
                        if (doc.getId().equals(sessionDocId)) continue;
                        if (expiresAt != null && expiresAt <= now) continue;

                        matchPlayers(doc.getId());
                        break;
                    }
                });
    }

    private void matchPlayers(String otherSessionId) {
        if (sessionDocId == null || matched || matchInProgress) return;
        matchInProgress = true;

        DocumentReference myRef = db.collection("PlayerSessions").document(sessionDocId);
        DocumentReference otherRef = db.collection("PlayerSessions").document(otherSessionId);

        db.runTransaction(transaction -> {
            DocumentSnapshot mySnap = transaction.get(myRef);
            DocumentSnapshot otherSnap = transaction.get(otherRef);

            if (!mySnap.exists() || !otherSnap.exists()) return null;
            if (!"searching".equals(mySnap.getString("status"))) return null;
            if (!"searching".equals(otherSnap.getString("status"))) return null;

            Long myExpiresAt = mySnap.getLong("expiresAt");
            Long otherExpiresAt = otherSnap.getLong("expiresAt");
            long now = System.currentTimeMillis();
            if ((myExpiresAt != null && myExpiresAt <= now) || (otherExpiresAt != null && otherExpiresAt <= now)) {
                return null;
            }

            String otherUserId = otherSnap.getString("userId");
            if (otherUserId == null || otherUserId.equals(currentUserId)) return null;

            transaction.update(myRef, "status", "matched", "matchedWith", otherUserId);
            transaction.update(otherRef, "status", "matched", "matchedWith", currentUserId);
            return null;
        }).addOnCompleteListener(task -> {
            matchInProgress = false;
            if (!task.isSuccessful()) {
                tryMatchWithOther();
            }
        });
    }

    private void openMatchFound(String matchedUserId) {
        removeListeners();
        if (countUpTimer != null) countUpTimer.cancel();
        Intent intent = new Intent(this, MatchFoundActivity.class);
        intent.putExtra(MatchFoundActivity.EXTRA_MATCHED_USER_ID, matchedUserId);
        startActivity(intent);
        finish();
    }

    private void cancelSearch() {
        if (sessionDocId != null && !matched) {
            db.collection("PlayerSessions").document(sessionDocId)
                    .update("status", "cancelled");
        }
        finish();
    }

    private void startTimer() {
        countUpTimer = new CountDownTimer(SESSION_TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                elapsedSeconds++;
                long min = elapsedSeconds / 60;
                long sec = elapsedSeconds % 60;
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
            }

            @Override
            public void onFinish() {
                tvStatus.setText("Không tìm thấy đối thủ. Hãy thử lại!");
                btnCancel.setText("Đóng");
                if (sessionDocId != null && !matched) {
                    db.collection("PlayerSessions").document(sessionDocId)
                            .update("status", "expired");
                }
            }
        };
        countUpTimer.start();
    }

    private void removeListeners() {
        if (sessionListener != null) {
            sessionListener.remove();
            sessionListener = null;
        }
        if (queueListener != null) {
            queueListener.remove();
            queueListener = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
        if (countUpTimer != null) countUpTimer.cancel();
        if (!matched && sessionDocId != null) {
            db.collection("PlayerSessions").document(sessionDocId)
                    .update("status", "cancelled");
        }
        if (radarView != null) radarView.stopAnimation();
    }
}
