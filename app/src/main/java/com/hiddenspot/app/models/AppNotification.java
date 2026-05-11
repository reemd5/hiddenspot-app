package com.hiddenspot.app.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class AppNotification {

    @DocumentId
    private String id;
    private String recipientUserId;
    private String actorUserId;
    private String actorName;
    private String actionType;
    private String gemId;
    private String gemName;
    private String message;

    @ServerTimestamp
    private Timestamp createdAt;

    public AppNotification() {}

    public AppNotification(String recipientUserId, String actorUserId, String actorName,
                           String actionType, String gemId, String gemName, String message) {
        this.recipientUserId = recipientUserId;
        this.actorUserId = actorUserId;
        this.actorName = actorName;
        this.actionType = actionType;
        this.gemId = gemId;
        this.gemName = gemName;
        this.message = message;
    }

    public String getId() { return id; }
    public String getRecipientUserId() { return recipientUserId; }
    public String getActorUserId() { return actorUserId; }
    public String getActorName() { return actorName; }
    public String getActionType() { return actionType; }
    public String getGemId() { return gemId; }
    public String getGemName() { return gemName; }
    public String getMessage() { return message; }
    public Timestamp getCreatedAt() { return createdAt; }

    public void setId(String id) { this.id = id; }
    public void setRecipientUserId(String recipientUserId) { this.recipientUserId = recipientUserId; }
    public void setActorUserId(String actorUserId) { this.actorUserId = actorUserId; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public void setGemId(String gemId) { this.gemId = gemId; }
    public void setGemName(String gemName) { this.gemName = gemName; }
    public void setMessage(String message) { this.message = message; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public String getFormattedTimestamp() {
        if (createdAt == null) return "Just now";
        return new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.getDefault())
                .format(createdAt.toDate());
    }
}
