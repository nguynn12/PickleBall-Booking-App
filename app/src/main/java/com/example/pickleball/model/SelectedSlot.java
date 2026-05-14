package com.example.pickleball.model;

import java.io.Serializable;

/** Một ô đã chọn trong grid lịch: sân con + slot 30 phút */
public class SelectedSlot implements Serializable {
    private String subCourtId;
    private String subCourtName;  // "Pickleball 1"
    private int slotIndex;        // index của slot (0 = openTime, 1 = openTime+30p...)
    private String startTime;     // "07:00"
    private String endTime;       // "07:30"
    private double price;         // giá của slot này (pricePerHour / 2)

    public SelectedSlot() {}

    public SelectedSlot(String subCourtId, String subCourtName,
                        int slotIndex, String startTime, String endTime, double price) {
        this.subCourtId   = subCourtId;
        this.subCourtName = subCourtName;
        this.slotIndex    = slotIndex;
        this.startTime    = startTime;
        this.endTime      = endTime;
        this.price        = price;
    }

    public String getSubCourtId()           { return subCourtId; }
    public void setSubCourtId(String v)     { this.subCourtId = v; }

    public String getSubCourtName()         { return subCourtName; }
    public void setSubCourtName(String v)   { this.subCourtName = v; }

    public int getSlotIndex()               { return slotIndex; }
    public void setSlotIndex(int v)         { this.slotIndex = v; }

    public String getStartTime()            { return startTime; }
    public void setStartTime(String v)      { this.startTime = v; }

    public String getEndTime()              { return endTime; }
    public void setEndTime(String v)        { this.endTime = v; }

    public double getPrice()                { return price; }
    public void setPrice(double v)          { this.price = v; }

    /** Key duy nhất để identify slot: "subCourtId_slotIndex" */
    public String getKey() { return subCourtId + "_" + slotIndex; }
}
