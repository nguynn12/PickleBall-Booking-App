package com.example.pickleball.activity.player;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.pickleball.R;
import com.example.pickleball.model.PlayerSession;
import com.example.pickleball.view.RadarView;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.Locale;

public class RadarMatchActivity extends AppCompatActivity {

    private static final long SESSION_TIMEOUT_MS = 5 * 60 * 1000L; // 5 phút

    private FirebaseFirestore db;
    private String currentUserId;
    private String sessionDocId;
    private ListenerRegistration sessionListener;

    private RadarView radarView;
    private TextView tvTimer, tvStatus;
    private MaterialButton btnCancel;

    private CountDownTimer countUpTimer;
    private long elapsedSeconds = 0;

    private boolean matched = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radar_match);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null) { finish(); return; }

        db = FirebaseFirestore.getInstance();

        radarView = findViewById(R.id.radarView);
        tvTimer   = findViewById(R.id.tvTimer);
        tvStatus  = findViewById(R.id.tvStatus);
        btnCancel = findViewById(R.id.btnCancel);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> cancelSearch());
        btnCancel.setOnClickListener(v -> cancelSearch());

        startTimer();
        createSearchSession();
    }

    private void createSearchSession() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(this);
        try {
            client.getLastLocation().addOnSuccessListener(location -> {
                double lat = location != null ? location.getLatitude() : 0;
                double lng = location != null ? location.getLongitude() : 0;

                db.collection("Users").document(currentUserId).get()
                        .addOnSuccessListener(doc -> {
                            PlayerSession session = new PlayerSession();
                            session.setUserId(currentUserId);
                            session.setUserName(doc.getString("fullName"));
                            session.setAvatarUrl(doc.getString("avatarUrl"));
                            session.setSkillLevel(doc.getString("skillLevel"));
                            session.setLat(lat);
                            session.setLng(lng);
                            session.setStatus("searching");
                            session.setCreatedAt(System.currentTimeMillis());
                            session.setExpiresAt(System.currentTimeMillis() + SESSION_TIMEOUT_MS);

                            db.collection("PlayerSessions").add(session)
                                    .addOnSuccessListener(ref -> {
                                        sessionDocId = ref.getId();
                                        ref.update("sessionId", sessionDocId);
                                        listenForMatch();
                                        tryMatchWithOther();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(this, "Lỗi tạo phiên: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show());
                        });
            });
        } catch (SecurityException e) {
            Toast.makeText(this, "Cần cấp quyền vị trí!", Toast.LENGTH_SHORT).show();
        }
    }

    private void listenForMatch() {
        sessionListener = db.collection("PlayerSessions")
                .document(sessionDocId)
                .addSnapshotListener((snap, err) -> {
                    if (snap == null || !snap.exists()) return;
                    String status      = snap.getString("status");
                    String matchedWith = snap.getString("matchedWith");

                    if ("matched".equals(status) && matchedWith != null && !matched) {
                        matched = true;
                        openMatchFound(matchedWith);
                    }
                });
    }

    private void tryMatchWithOther() {
        db.collection("PlayerSessions")
                .whereEqualTo("status", "searching")
                .whereGreaterThan("expiresAt", System.currentTimeMillis())
                .get()
                .addOnSuccessListener(snap -> {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        String uid = doc.getString("userId");
                        if (uid == null || uid.equals(currentUserId)) continue;
                        if (doc.getId().equals(sessionDocId)) continue;
                        matchPlayers(doc.getId(), uid);
                        break;
                    }
                });
    }

    private void matchPlayers(String otherSessionId, String otherUserId) {
        if (sessionDocId == null) return;
        WriteBatch batch = db.batch();
        DocumentReference myRef    = db.collection("PlayerSessions").document(sessionDocId);
        DocumentReference otherRef = db.collection("PlayerSessions").document(otherSessionId);
        batch.update(myRef,    "status", "matched", "matchedWith", otherUserId);
        batch.update(otherRef, "status", "matched", "matchedWith", currentUserId);
        batch.commit();
    }

    private void openMatchFound(String matchedUserId) {
        if (sessionListener != null) { sessionListener.remove(); sessionListener = null; }
        if (countUpTimer != null) countUpTimer.cancel();
        Intent intent = new Intent(this, MatchFoundActivity.class);
        intent.putExtra(MatchFoundActivity.EXTRA_MATCHED_USER_ID, matchedUserId);
        startActivity(intent);
        finish();
    }

    private void cancelSearch() {
        if (sessionDocId != null) {
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
                if (sessionDocId != null) {
                    db.collection("PlayerSessions").document(sessionDocId)
                            .update("status", "expired");
                }
            }
        };
        countUpTimer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sessionListener != null) sessionListener.remove();
        if (countUpTimer != null) countUpTimer.cancel();
        if (!matched && sessionDocId != null) {
            db.collection("PlayerSessions").document(sessionDocId)
                    .update("status", "cancelled");
        }
        if (radarView != null) radarView.stopAnimation();
    }
}
