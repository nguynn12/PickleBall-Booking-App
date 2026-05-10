package com.example.pickleball.model;

import java.io.Serializable;

public class CourtService implements Serializable {
    private String serviceId;
    private String courtId;
    private String name;        // "Social tháng 5", "Nước Tăng Lực Rockstar"
    private double price;       // 500000, 15000
    private String unit;        // "tháng", "Lon", "giờ"
    private String type;        // "price_table" | "service"
    // Cho bảng giá sân:
    private String dayRange;    // "T2 - T6", "T7 - CN"
    private String timeRange;   // "5h - 17h", "17h - 22h"

    public CourtService() {}

    public CourtService(String courtId, String name, double price, String unit, String type) {
        this.courtId = courtId;
        this.name = name;
        this.price = price;
        this.unit = unit;
        this.type = type;
    }

    public String getServiceId()            { return serviceId; }
    public void setServiceId(String v)      { this.serviceId = v; }

    public String getCourtId()              { return courtId; }
    public void setCourtId(String v)        { this.courtId = v; }

    public String getName()                 { return name; }
    public void setName(String v)           { this.name = v; }

    public double getPrice()                { return price; }
    public void setPrice(double v)          { this.price = v; }

    public String getUnit()                 { return unit; }
    public void setUnit(String v)           { this.unit = v; }

    public String getType()                 { return type; }
    public void setType(String v)           { this.type = v; }

    public String getDayRange()             { return dayRange; }
    public void setDayRange(String v)       { this.dayRange = v; }

    public String getTimeRange()            { return timeRange; }
    public void setTimeRange(String v)      { this.timeRange = v; }
}
