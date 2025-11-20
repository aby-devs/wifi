package com.example.wifi.models;

import java.util.Date;

public class ChatMessage {
    private String messageId;
    private String userId;
    private String userName;
    private String message;
    private Date timestamp;
    private boolean isRead;

    // Default constructor required for Firestore
    public ChatMessage() {
    }

    public ChatMessage(String messageId, String userId, String userName, String message, Date timestamp) {
        this.messageId = messageId;
        this.userId = userId;
        this.userName = userName;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}





