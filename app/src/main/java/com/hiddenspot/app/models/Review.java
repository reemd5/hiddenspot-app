package com.hiddenspot.app.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class Review {

    @DocumentId
    private String id;
    private String gemId;
    private String userId;
    private String userName;
    private String userAvatar;
    private float rating;
    private String comment;
    private String gemName;

    @ServerTimestamp
    private Timestamp createdAt;

    public Review() {}

    public Review(String gemId, String userId, String userName, String userAvatar, float rating, String comment) {
        this.gemId = gemId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.rating = rating;
        this.comment = comment;
    }

    public String getId() { return id; }
    public String getGemId() { return gemId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getUserAvatar() { return userAvatar; }
    public float getRating() { return rating; }
    public String getComment() { return comment; }
    public String getGemName() { return gemName; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setGemId(String gemId) { this.gemId = gemId; }
    public void setUserId(String userId) { this.userId = userId; }
    public void setUserName(String userName) { this.userName = userName; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public void setRating(float rating) { this.rating = rating; }
    public void setComment(String comment) { this.comment = comment; }
    public void setGemName(String gemName) { this.gemName = gemName; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getFormattedDate() {
        if (createdAt == null) return "Recently";
        return new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(createdAt.toDate());
    }
}
