package com.example.wifi.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

public class PaymentSimulator {
    private static final String TAG = "PaymentSimulator";
    private static PaymentSimulator instance;
    private Handler mainHandler;

    public interface PaymentCallback {
        void onSuccess(String transactionId);
        void onFailure(String errorMessage);
    }

    private PaymentSimulator() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static PaymentSimulator getInstance() {
        if (instance == null) {
            synchronized (PaymentSimulator.class) {
                if (instance == null) {
                    instance = new PaymentSimulator();
                }
            }
        }
        return instance;
    }

    /**
     * Simulate a payment process
     * @param phoneNumber Phone number (for display purposes)
     * @param amount Payment amount
     * @param callback Callback for payment result
     */
    public void simulatePayment(String phoneNumber, double amount, final PaymentCallback callback) {
        Log.d(TAG, "Simulating payment: " + amount + " for " + phoneNumber);
        
        // Simulate payment processing delay (3 seconds)
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Generate a fake transaction ID
                String transactionId = "SIM_" + System.currentTimeMillis();
                Log.d(TAG, "Payment simulation successful: " + transactionId);
                
                if (callback != null) {
                    callback.onSuccess(transactionId);
                }
            }
        }, 3000); // 3 second delay to simulate processing
    }

    /**
     * Simulate a failed payment (for testing error handling)
     */
    public void simulatePaymentFailure(String phoneNumber, double amount, final PaymentCallback callback) {
        Log.d(TAG, "Simulating payment failure: " + amount + " for " + phoneNumber);
        
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (callback != null) {
                    callback.onFailure("Payment simulation failed. Please try again.");
                }
            }
        }, 2000);
    }
}





