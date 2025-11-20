package com.example.wifi.models;

import java.util.Date;

public class Subscription {
    private String subscriptionId;
    private String userId;
    private String planType; // "new_client" or "monthly_renewal"
    private double amount;
    private double setupFee;
    private double totalAmount;
    private String status; // "pending", "active", "expired"
    private Date startDate;
    private Date expiryDate;
    private Date createdAt;

    // Default constructor required for Firestore
    public Subscription() {
    }

    public Subscription(String userId, String planType, double amount, double setupFee) {
        this.userId = userId;
        this.planType = planType;
        this.amount = amount;
        this.setupFee = setupFee;
        this.totalAmount = amount + setupFee;
        this.status = "pending";
        this.createdAt = new Date();
    }

    // Getters and Setters
    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getSetupFee() {
        return setupFee;
    }

    public void setSetupFee(double setupFee) {
        this.setupFee = setupFee;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}







