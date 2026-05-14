package com.example.pickleball.model;

import java.io.Serializable;
import java.util.List;

public class Court implements Serializable {
    private String courtId;
    private String courtName;
    private String address;
    private String type;           // "Pickleball" | "Trong nhà" | "Ngoài trời"
    private Double lat;
    private Double lng;
    private double pricePerHour;
    private String imageUrl;
    private String ownerId;
    private String openTime;       // "05:00"
    private String closeTime;      // "22:00"
    private String phone;          // số điện thoại liên hệ
    private String description;    // mô tả sân
    private String terms;          // điều khoản & quy định
    private String status;         // "active" | "inactive"
    private long createdAt;

    public Court() {}

    public Court(String courtId, String courtName, String address,
                 String type, double pricePerHour, String imageUrl) {
        this.courtId = courtId;
        this.courtName = courtName;
        this.address = address;
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.imageUrl = imageUrl;
        this.status = "active";
    }

    // Getters & Setters
    public String getCourtId()                  { return courtId; }
    public void setCourtId(String v)            { this.courtId = v; }

    public String getCourtName()                { return courtName; }
    public void setCourtName(String v)          { this.courtName = v; }

    public String getAddress()                  { return address; }
    public void setAddress(String v)            { this.address = v; }

    public String getType()                     { return type; }
    public void setType(String v)               { this.type = v; }

    public Double getLat()                      { return lat; }
    public void setLat(Double v)                { this.lat = v; }

    public Double getLng()                      { return lng; }
    public void setLng(Double v)                { this.lng = v; }

    public double getPricePerHour()             { return pricePerHour; }
    public void setPricePerHour(double v)       { this.pricePerHour = v; }

    public String getImageUrl()                 { return imageUrl; }
    public void setImageUrl(String v)           { this.imageUrl = v; }

    public String getOwnerId()                  { return ownerId; }
    public void setOwnerId(String v)            { this.ownerId = v; }

    public String getOpenTime()                 { return openTime; }
    public void setOpenTime(String v)           { this.openTime = v; }

    public String getCloseTime()                { return closeTime; }
    public void setCloseTime(String v)          { this.closeTime = v; }

    public String getPhone()                    { return phone; }
    public void setPhone(String v)              { this.phone = v; }

    public String getDescription()              { return description; }
    public void setDescription(String v)        { this.description = v; }

    public String getTerms()                    { return terms; }
    public void setTerms(String v)              { this.terms = v; }

    public String getStatus()                   { return status; }
    public void setStatus(String v)             { this.status = v; }

    public long getCreatedAt()                  { return createdAt; }
    public void setCreatedAt(long v)            { this.createdAt = v; }

    /** Trả về chuỗi giờ mở cửa dạng "05:00 - 22:00" */
    public String getOpenHours() {
        String open  = openTime  != null && !openTime.isEmpty()  ? openTime  : "06:00";
        String close = closeTime != null && !closeTime.isEmpty() ? closeTime : "22:00";
        return open + " - " + close;
    }
}
