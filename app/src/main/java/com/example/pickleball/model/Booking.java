package com.example.pickleball.model;

import java.io.Serializable;

public class Booking implements Serializable {
    private String bookingId;
    private String userId;
    private String courtId;
    private String courtName;   // lưu tên sân để hiển thị nhanh
    private String ownerId;     // để owner query đơn của mình
    private String date;        // "dd/MM/yyyy"
    private String startTime;   // "08:00"
    private String endTime;     // "10:00"
    private String timeSlotId;  // legacy
    private String subCourtId;   // sân con được đặt
    private int rentedPaddles;
    private int boughtBalls;
    private double totalPrice;
    private String status;      // "pending" | "confirmed" | "rejected" | "cancelled"
    private String note;
    private long createdAt;

    // --- Thanh toán ---
    private double depositAmount;       // Số tiền cọc (totalPrice * 30%)
    private double remainingAmount;     // Số tiền còn lại (totalPrice * 70%)
    private String paymentStatus;       // "pending" | "paid" | "refunded" | "expired"
    private long   paidAt;              // timestamp khi thanh toán xong
    private long   paymentExpiredAt;    // timestamp hết hạn QR (createdAt + 15 phút)
    private String customerName;        // tên khách (lưu để hiển thị cho owner)

    // --- PayOS ---
    private long   payosOrderCode;      // mã đơn hàng PayOS (số nguyên)
    private String payosPaymentLinkId;  // ID link thanh toán PayOS
    private String payosTransactionId;  // ID giao dịch sau khi thanh toán

    // --- Hủy & hoàn tiền ---
    private String cancelReason;        // lý do hủy
    private String cancelledBy;         // "user" | "owner" | "system"
    private long   cancelledAt;         // timestamp hủy
    private String refundStatus;        // "not_applicable" | "refund_pending" | "refund_done"
    private double refundAmount;        // số tiền được hoàn

    public Booking() {}

    // Constructor đầy đủ cho tạo booking mới
    public Booking(String userId, String courtId, String courtName, String ownerId,
                   String date, String startTime, String endTime,
                   double totalPrice, String note) {
        this.userId = userId;
        this.courtId = courtId;
        this.courtName = courtName;
        this.ownerId = ownerId;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalPrice = totalPrice;
        this.note = note;
        this.status = "pending";
        this.createdAt = System.currentTimeMillis();
        this.rentedPaddles = 0;
        this.boughtBalls = 0;
    }

    // Getters & Setters
    public String getBookingId() { return bookingId; }
    public void setBookingId(String bookingId) { this.bookingId = bookingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(String timeSlotId) { this.timeSlotId = timeSlotId; }

    public String getSubCourtId() { return subCourtId; }
    public void setSubCourtId(String subCourtId) { this.subCourtId = subCourtId; }

    // --- Thanh toán ---
    public double getDepositAmount() { return depositAmount; }
    public void setDepositAmount(double v) { this.depositAmount = v; }

    public double getRemainingAmount() { return remainingAmount; }
    public void setRemainingAmount(double v) { this.remainingAmount = v; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String v) { this.paymentStatus = v; }

    public long getPaidAt() { return paidAt; }
    public void setPaidAt(long v) { this.paidAt = v; }

    public long getPaymentExpiredAt() { return paymentExpiredAt; }
    public void setPaymentExpiredAt(long v) { this.paymentExpiredAt = v; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { this.customerName = v; }

    // --- PayOS ---
    public long getPayosOrderCode() { return payosOrderCode; }
    public void setPayosOrderCode(long v) { this.payosOrderCode = v; }

    public String getPayosPaymentLinkId() { return payosPaymentLinkId; }
    public void setPayosPaymentLinkId(String v) { this.payosPaymentLinkId = v; }

    public String getPayosTransactionId() { return payosTransactionId; }
    public void setPayosTransactionId(String v) { this.payosTransactionId = v; }

    // --- Hủy & hoàn tiền ---
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String v) { this.cancelReason = v; }

    public String getCancelledBy() { return cancelledBy; }
    public void setCancelledBy(String v) { this.cancelledBy = v; }

    public long getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(long v) { this.cancelledAt = v; }

    public String getRefundStatus() { return refundStatus; }
    public void setRefundStatus(String v) { this.refundStatus = v; }

    public double getRefundAmount() { return refundAmount; }
    public void setRefundAmount(double v) { this.refundAmount = v; }

    public int getRentedPaddles() { return rentedPaddles; }
    public void setRentedPaddles(int rentedPaddles) { this.rentedPaddles = rentedPaddles; }

    public int getBoughtBalls() { return boughtBalls; }
    public void setBoughtBalls(int boughtBalls) { this.boughtBalls = boughtBalls; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
