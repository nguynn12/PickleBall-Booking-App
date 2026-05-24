package com.example.pickleball.model;

import java.io.Serializable;

public class User implements Serializable {
    private String userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;        // "admin" | "owner" | "user"
    private String skillLevel;  // "beginner" | "intermediate" | "pro"
    private String avatarUrl;   // URL anh dai dien (Firebase Storage)
    private boolean blocked;
    private Double lat;          // độ vĩ đặt cuối cùng
    private Double lng;          // kinh độ đặt cuối cùng
    private long lastSeen;       // timestamp lần cuối online
    // Firebase yeu cau constructor rong
    public User() {}

    public User(String userId, String fullName, String email, String phone, String role) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.skillLevel = "beginner"; // mac dinh
        this.avatarUrl = "";
        this.blocked = false;
    }

    public User(String userId, String fullName, String email, String phone,
                String role, String skillLevel) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.role = role;
        this.skillLevel = skillLevel;
        this.avatarUrl = "";
    }

    // Getters & Setters
    public String getUserId()   { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail()    { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone()    { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRole()     { return role; }
    public void setRole(String role) { this.role = role; }

    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isBlocked() {
        return blocked;
    }
    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
}
