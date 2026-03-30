package com.example.pickleball.model;

import java.io.Serializable;

public class Court implements Serializable {
    private String courtId;
    private String courtName;
    private String address;
    private String type; // "Trong nhà" hoặc "Ngoài trời"
    private double pricePerHour;
    private String imageUrl; // Link ảnh sân

    public Court() {}

    public Court(String courtId, String courtName, String address, String type, double pricePerHour, String imageUrl) {
        this.courtId = courtId;
        this.courtName = courtName;
        this.address = address;
        this.type = type;
        this.pricePerHour = pricePerHour;
        this.imageUrl = imageUrl;
    }

    public String getCourtId() { return courtId; }
    public void setCourtId(String courtId) { this.courtId = courtId; }

    public String getCourtName() { return courtName; }
    public void setCourtName(String courtName) { this.courtName = courtName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getPricePerHour() { return pricePerHour; }
    public void setPricePerHour(double pricePerHour) { this.pricePerHour = pricePerHour; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}