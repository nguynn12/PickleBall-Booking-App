package com.example.pickleball.model;

public class PlayerSession {
    private String sessionId;
    private String userId;
    private String userName;
    private String avatarUrl;
    private String skillLevel;    // "beginner" | "intermediate" | "pro"
    private double lat;
    private double lng;
    private String status;        // "searching" | "matched" | "cancelled" | "expired"
    private long createdAt;
    private long expiresAt;       // createdAt + 5 phút (300000ms)
    private String matchedWith;   // userId của người được ghép

    public PlayerSession() {}

    // Getters & Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public String getMatchedWith() { return matchedWith; }
    public void setMatchedWith(String matchedWith) { this.matchedWith = matchedWith; }
}
