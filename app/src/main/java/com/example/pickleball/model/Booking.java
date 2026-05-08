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
    private int rentedPaddles;
    private int boughtBalls;
    private double totalPrice;
    private String status;      // "pending" | "confirmed" | "rejected" | "cancelled"
    private String note;
    private long createdAt;

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
