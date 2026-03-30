package com.example.pickleball.model;

public class TimeSlot {
    private String slotId;
    private String courtId;
    private String startTime; // Ví dụ: "17:00"
    private String endTime;   // Ví dụ: "18:00"
    private boolean isAvailable; // true = còn trống, false = đã có người đặt

    public TimeSlot() {}

    public TimeSlot(String slotId, String courtId, String startTime, String endTime, boolean isAvailable) {
        this.slotId = slotId;
        this.courtId = courtId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isAvailable = isAvailable;
    }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { isAvailable = available; }
}