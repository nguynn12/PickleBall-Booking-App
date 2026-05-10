package com.example.pickleball.model;

import java.io.Serializable;

public class Review implements Serializable {
    private String reviewId;
    private String courtId;
    private String userId;
    private String userName;
    private float rating;       // 1.0 - 5.0
    private String comment;
    private long createdAt;

    public Review() {}

    public Review(String courtId, String userId, String userName, float rating, String comment) {
        this.courtId = courtId;
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = System.currentTimeMillis();
    }

    public String getReviewId()             { return reviewId; }
    public void setReviewId(String v)       { this.reviewId = v; }

    public String getCourtId()              { return courtId; }
    public void setCourtId(String v)        { this.courtId = v; }

    public String getUserId()               { return userId; }
    public void setUserId(String v)         { this.userId = v; }

    public String getUserName()             { return userName; }
    public void setUserName(String v)       { this.userName = v; }

    public float getRating()                { return rating; }
    public void setRating(float v)          { this.rating = v; }

    public String getComment()              { return comment; }
    public void setComment(String v)        { this.comment = v; }

    public long getCreatedAt()              { return createdAt; }
    public void setCreatedAt(long v)        { this.createdAt = v; }
}
