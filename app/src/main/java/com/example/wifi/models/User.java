package com.example.wifi.models;

import java.util.Date;

public class User {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private Date createdAt;
    private Date updatedAt;
    private boolean isActive;
    private Date subscriptionExpiryDate;
    private double dataUsedGB; // Data used in GB for current month
    private boolean hasPaidBefore; // Track if user has ever made a payment

    // Default constructor required for Firestore
    public User() {
    }

    public User(String userId, String name, String email, String phone) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.isActive = false;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public Date getSubscriptionExpiryDate() {
        return subscriptionExpiryDate;
    }

    public void setSubscriptionExpiryDate(Date subscriptionExpiryDate) {
        this.subscriptionExpiryDate = subscriptionExpiryDate;
    }

    public double getDataUsedGB() {
        return dataUsedGB;
    }

    public void setDataUsedGB(double dataUsedGB) {
        this.dataUsedGB = dataUsedGB;
    }

    public boolean isHasPaidBefore() {
        return hasPaidBefore;
    }

    public void setHasPaidBefore(boolean hasPaidBefore) {
        this.hasPaidBefore = hasPaidBefore;
    }
}


