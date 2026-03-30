package com.example.pickleball.model;

import java.io.Serializable;

public class Booking implements Serializable {
    private String bookingId;
    private String userId;
    private String courtId;
    private String date; // Ngày đặt, ví dụ: "25/10/2023"
    private String timeSlotId;
    private int rentedPaddles; // Số lượng vợt thuê thêm
    private int boughtBalls; // Số lượng bóng mua thêm
    private double totalPrice;
    private String status; // "Chờ xác nhận", "Đã hoàn thành", "Đã hủy"

    public Booking() {}

    public Booking(String bookingId, String userId, String courtId, String date, String timeSlotId, int rentedPaddles, int boughtBalls, double totalPrice, String status) {
        this.bookingId = bookingId;
        this.userId = userId;
        this.courtId = courtId;
        this.date = date;
        this.timeSlotId = timeSlotId;
        this.rentedPaddles = rentedPaddles;
        this.boughtBalls = boughtBalls;
        this.totalPrice = totalPrice;
        this.status = status;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCourtId() {
        return courtId;
    }

    public void setCourtId(String courtId) {
        this.courtId = courtId;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTimeSlotId() {
        return timeSlotId;
    }

    public void setTimeSlotId(String timeSlotId) {
        this.timeSlotId = timeSlotId;
    }

    public int getRentedPaddles() {
        return rentedPaddles;
    }

    public void setRentedPaddles(int rentedPaddles) {
        this.rentedPaddles = rentedPaddles;
    }

    public int getBoughtBalls() {
        return boughtBalls;
    }

    public void setBoughtBalls(int boughtBalls) {
        this.boughtBalls = boughtBalls;
    }

    public double getTotalPrice() {
        return totalPrice;
    }

    public void setTotalPrice(double totalPrice) {
        this.totalPrice = totalPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}