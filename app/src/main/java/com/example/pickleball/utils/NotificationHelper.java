package com.example.pickleball.utils;

import com.example.pickleball.model.Notification;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Helper gửi thông báo trong app (Firestore-based).
 * Khi owner xác nhận/từ chối → gửi notif cho customer.
 * Khi customer hủy → gửi notif cho owner.
 */
public class NotificationHelper {

    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Gửi thông báo đặt sân được xác nhận */
    public static void sendBookingConfirmed(String customerId, String courtName, String date,
                                            String bookingId) {
        send(new Notification(
                customerId,
                "✅ Đặt sân thành công!",
                "Lịch đặt sân " + courtName + " vào ngày " + date + " đã được xác nhận.",
                "booking_confirmed",
                bookingId
        ));
    }

    /** Gửi thông báo đặt sân bị từ chối */
    public static void sendBookingRejected(String customerId, String courtName, String date,
                                           String bookingId) {
        send(new Notification(
                customerId,
                "❌ Đặt sân bị từ chối",
                "Lịch đặt sân " + courtName + " vào ngày " + date + " đã bị từ chối.",
                "booking_rejected",
                bookingId
        ));
    }

    /** Gửi thông báo khách hủy đơn cho owner */
    public static void sendBookingCancelledToOwner(String ownerId, String courtName, String date,
                                                   String bookingId) {
        send(new Notification(
                ownerId,
                "🚫 Khách hủy đặt sân",
                "Khách hàng đã hủy lịch đặt sân " + courtName + " vào ngày " + date + ".",
                "booking_cancelled",
                bookingId
        ));
    }

    private static void send(Notification notif) {
        db.collection("Notifications")
                .add(notif)
                .addOnSuccessListener(ref -> ref.update("notifId", ref.getId()));
    }
}
