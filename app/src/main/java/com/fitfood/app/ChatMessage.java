package com.fitfood.app;

import com.google.firebase.Timestamp;

public class ChatMessage {
    private String senderId;
    private String message;
    private Timestamp timestamp;
    private boolean sentByAdmin;

    public ChatMessage() { }

    public ChatMessage(String senderId, String message, Timestamp timestamp, boolean sentByAdmin) {
        this.senderId = senderId;
        this.message = message;
        this.timestamp = timestamp;
        this.sentByAdmin = sentByAdmin;
    }

    public String getSenderId() { return senderId; }
    public String getMessage() { return message; }
    public Timestamp getTimestamp() { return timestamp; }
    public boolean isSentByAdmin() { return sentByAdmin; }
}