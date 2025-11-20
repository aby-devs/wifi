package com.example.wifi;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wifi.models.Payment;
import com.example.wifi.models.User;
import com.example.wifi.services.FirebaseAuthService;
import com.example.wifi.services.FirestoreService;
import com.example.wifi.services.PaymentSimulator;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MonthlyRenewActivity extends AppCompatActivity {

    private ImageButton backButton;
    private MaterialButton renewButton;
    private FirebaseAuthService authService;
    private FirestoreService firestoreService;
    private PaymentSimulator paymentSimulator;
    private static final double RENEWAL_AMOUNT = 1500.0; // Monthly renewal amount

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monthly_renew);

        authService = FirebaseAuthService.getInstance();
        firestoreService = FirestoreService.getInstance();
        paymentSimulator = PaymentSimulator.getInstance();

        // Check if user is logged in
        if (authService.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }

        backButton = findViewById(R.id.backButton);
        renewButton = findViewById(R.id.renewButton);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        renewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPaymentDialog();
            }
        });
    }

    private void showPaymentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_mpesa_payment, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextInputEditText phoneInput = dialogView.findViewById(R.id.phoneInput);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);
        MaterialButton payButton = dialogView.findViewById(R.id.payButton);

        // Set payment amount
        TextView amountText = dialogView.findViewById(R.id.amountText);
        if (amountText != null) {
            amountText.setText("Ksh " + String.format("%.0f", RENEWAL_AMOUNT));
        }

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        payButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phoneNumber = phoneInput.getText().toString().trim();
                if (TextUtils.isEmpty(phoneNumber)) {
                    Toast.makeText(MonthlyRenewActivity.this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (phoneNumber.length() < 10) {
                    Toast.makeText(MonthlyRenewActivity.this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                processRenewal(phoneNumber);
            }
        });

        dialog.show();
    }

    private void processRenewal(String phoneNumber) {
        renewButton.setEnabled(false);
        renewButton.setText("Processing...");

        String userId = authService.getCurrentUser().getUid();
        if (userId == null || userId.isEmpty()) {
            renewButton.setEnabled(true);
            renewButton.setText("Renew Now");
            Toast.makeText(this, "Error: User not found. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d("MonthlyRenew", "Starting renewal process for user: " + userId);

        // Create payment with monthly_renewal flag
        Payment payment = new Payment(userId, RENEWAL_AMOUNT, phoneNumber, false, true);
        firestoreService.createPayment(payment, new FirestoreService.PaymentCallback() {
            @Override
            public void onSuccess(Payment createdPayment) {
                Log.d("MonthlyRenew", "Payment created: " + createdPayment.getPaymentId());
                // Show processing dialog
                renewButton.setText("Processing Payment...");
                
                // Simulate payment
                paymentSimulator.simulatePayment(phoneNumber, RENEWAL_AMOUNT, 
                        new PaymentSimulator.PaymentCallback() {
                            @Override
                            public void onSuccess(String transactionId) {
                                Log.d("MonthlyRenew", "Payment simulated successfully: " + transactionId);
                                // Update payment with transaction ID
                                Map<String, Object> paymentUpdates = new HashMap<>();
                                paymentUpdates.put("transactionReference", transactionId);
                                paymentUpdates.put("status", "completed");
                                
                                firestoreService.updatePayment(createdPayment.getPaymentId(), paymentUpdates,
                                        new FirestoreService.FirestoreCallback() {
                                    @Override
                                    public void onSuccess(Object result) {
                                        Log.d("MonthlyRenew", "Payment updated successfully");
                                        // Update user subscription status - this will move user from expired_users to active_users
                                        Calendar calendar = Calendar.getInstance();
                                        calendar.add(Calendar.MINUTE, 3); // 3 minutes for testing
                                        Date expiryDate = calendar.getTime();
                                        
                                        Map<String, Object> userUpdates = new HashMap<>();
                                        userUpdates.put("isActive", true);
                                        userUpdates.put("subscriptionExpiryDate", expiryDate);
                                        userUpdates.put("hasPaidBefore", true); // Ensure this is set
                                        
                                        Log.d("MonthlyRenew", "Updating user to move from expired_users to active_users");
                                        firestoreService.updateUser(userId, userUpdates, 
                                                new FirestoreService.FirestoreCallback() {
                                            @Override
                                            public void onSuccess(Object result) {
                                                // Success! User moved from expired_users to active_users
                                                Log.d("MonthlyRenew", "User successfully moved to active_users. Status: ACTIVE");
                                                Toast.makeText(MonthlyRenewActivity.this, 
                                                        "Payment successful! Subscription renewed. Status changed to ACTIVE.", 
                                                        Toast.LENGTH_LONG).show();
                                                Intent resultIntent = new Intent();
                                                resultIntent.putExtra("subscriptionActivated", true);
                                                resultIntent.putExtra("expiryDate", expiryDate.getTime());
                                                setResult(RESULT_OK, resultIntent);
                                                finish();
                                            }

                                            @Override
                                            public void onFailure(String errorMessage) {
                                                renewButton.setEnabled(true);
                                                renewButton.setText("Renew Now");
                                                Log.e("MonthlyRenew", "Failed to update user: " + errorMessage);
                                                Toast.makeText(MonthlyRenewActivity.this, 
                                                        "Payment successful but failed to update user status. Please try again.", 
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        renewButton.setEnabled(true);
                                        renewButton.setText("Renew Now");
                                        Log.e("MonthlyRenew", "Failed to update payment: " + errorMessage);
                                        Toast.makeText(MonthlyRenewActivity.this, 
                                                "Failed to update payment: " + errorMessage, 
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                renewButton.setEnabled(true);
                                renewButton.setText("Renew Now");
                                Log.e("MonthlyRenew", "Payment simulation failed: " + errorMessage);
                                Toast.makeText(MonthlyRenewActivity.this, 
                                        "Payment failed: " + errorMessage, 
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onFailure(String errorMessage) {
                renewButton.setEnabled(true);
                renewButton.setText("Renew Now");
                Log.e("MonthlyRenew", "Failed to create payment: " + errorMessage);
                Toast.makeText(MonthlyRenewActivity.this, 
                        "Failed to create payment: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }


    private void navigateToLogin() {
        Intent intent = new Intent(MonthlyRenewActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


