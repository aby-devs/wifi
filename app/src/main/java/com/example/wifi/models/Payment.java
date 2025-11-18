package com.example.wifi.models;

import java.util.Date;

public class Payment {
    private String paymentId;
    private String userId;
    private String subscriptionId;
    private double amount;
    private String paymentMethod; // "M-PESA" or "APP"
    private String phoneNumber;
    private String status; // "pending", "completed", "failed"
    private String transactionReference;
    private Date paymentDate;
    private Date createdAt;
    private boolean firstTimePayment; // true for new client payments
    private boolean monthlyRenewal; // true for renewal payments

    // Default constructor required for Firestore
    public Payment() {
    }

    public Payment(String userId, double amount, String phoneNumber, boolean isFirstTimePayment, boolean isMonthlyRenewal) {
        this.userId = userId;
        this.subscriptionId = null; // No longer using subscriptionId
        this.amount = amount;
        this.paymentMethod = "M-PESA";
        this.phoneNumber = phoneNumber;
        this.status = "pending";
        this.paymentDate = new Date();
        this.createdAt = new Date();
        this.firstTimePayment = isFirstTimePayment;
        this.monthlyRenewal = isMonthlyRenewal;
        // Generate a simple transaction reference
        this.transactionReference = "TXN" + System.currentTimeMillis();
    }

    // Getters and Setters
    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isFirstTimePayment() {
        return firstTimePayment;
    }

    public void setFirstTimePayment(boolean firstTimePayment) {
        this.firstTimePayment = firstTimePayment;
    }

    public boolean isMonthlyRenewal() {
        return monthlyRenewal;
    }

    public void setMonthlyRenewal(boolean monthlyRenewal) {
        this.monthlyRenewal = monthlyRenewal;
    }
}




