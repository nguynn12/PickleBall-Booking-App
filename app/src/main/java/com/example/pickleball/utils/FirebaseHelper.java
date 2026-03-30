package com.example.pickleball.utils;

import com.example.pickleball.model.Court;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class FirebaseHelper {
    private FirebaseFirestore db;

    public FirebaseHelper() {
        db = FirebaseFirestore.getInstance();
    }

    // Hàm thêm 1 sân mới vào Firestore
    public void addCourt(Court court, com.google.android.gms.tasks.OnSuccessListener<Void> successListener, com.google.android.gms.tasks.OnFailureListener failureListener) {
        // Tự động tạo ID ngẫu nhiên cho Sân nếu chưa có
        if (court.getCourtId() == null || court.getCourtId().isEmpty()) {
            court.setCourtId(db.collection("Courts").document().getId());
        }
        db.collection("Courts").document(court.getCourtId())
                .set(court)
                .addOnSuccessListener(successListener)
                .addOnFailureListener(failureListener);
    }

    // Hàm lấy danh sách toàn bộ Sân Pickleball
    public void getAllCourts(OnCompleteListener<QuerySnapshot> completeListener) {
        db.collection("Courts").get().addOnCompleteListener(completeListener);
    }
}