package com.example.pickleball.model;

import java.io.Serializable;

public class Notification implements Serializable {
    private String notifId;
    private String userId;      // người nhận
    private String title;
    private String message;
    private boolean isRead;
    private String type;        // "booking_confirmed" | "booking_rejected" | "booking_cancelled"
    private String bookingId;   // liên kết đến booking
    private long createdAt;

    public Notification() {}

    public Notification(String userId, String title, String message, String type, String bookingId) {
        this.userId    = userId;
        this.title     = title;
        this.message   = message;
        this.type      = type;
        this.bookingId = bookingId;
        this.isRead    = false;
        this.createdAt = System.currentTimeMillis();
    }

    public String getNotifId()              { return notifId; }
    public void setNotifId(String v)        { this.notifId = v; }

    public String getUserId()               { return userId; }
    public void setUserId(String v)         { this.userId = v; }

    public String getTitle()                { return title; }
    public void setTitle(String v)          { this.title = v; }

    public String getMessage()              { return message; }
    public void setMessage(String v)        { this.message = v; }

    public boolean isRead()                 { return isRead; }
    public void setRead(boolean v)          { this.isRead = v; }

    public String getType()                 { return type; }
    public void setType(String v)           { this.type = v; }

    public String getBookingId()            { return bookingId; }
    public void setBookingId(String v)      { this.bookingId = v; }

    public long getCreatedAt()              { return createdAt; }
    public void setCreatedAt(long v)        { this.createdAt = v; }
}
