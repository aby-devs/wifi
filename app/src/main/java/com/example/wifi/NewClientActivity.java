package com.example.wifi;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

public class NewClientActivity extends AppCompatActivity {

    private ImageButton backButton;
    private MaterialButton subscribeButton;
    private FirebaseAuthService authService;
    private FirestoreService firestoreService;
    private PaymentSimulator paymentSimulator;
    private static final double NEW_CLIENT_AMOUNT = 4500.0; // New client subscription amount

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_client);

        authService = FirebaseAuthService.getInstance();
        firestoreService = FirestoreService.getInstance();
        paymentSimulator = PaymentSimulator.getInstance();

        // Check if user is logged in
        if (authService.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }

        backButton = findViewById(R.id.backButton);
        subscribeButton = findViewById(R.id.subscribeButton);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        subscribeButton.setOnClickListener(new View.OnClickListener() {
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
            amountText.setText("Ksh " + String.format("%.0f", NEW_CLIENT_AMOUNT));
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
                    Toast.makeText(NewClientActivity.this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (phoneNumber.length() < 10) {
                    Toast.makeText(NewClientActivity.this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                processNewClientSubscription(phoneNumber);
            }
        });

        dialog.show();
    }

    private void processNewClientSubscription(String phoneNumber) {
        subscribeButton.setEnabled(false);
        subscribeButton.setText("Processing...");

        String userId = authService.getCurrentUser().getUid();

        // Create payment with first_time_payment flag
        Payment payment = new Payment(userId, NEW_CLIENT_AMOUNT, phoneNumber, true, false);
        firestoreService.createPayment(payment, new FirestoreService.PaymentCallback() {
            @Override
            public void onSuccess(Payment createdPayment) {
                // Show processing dialog
                subscribeButton.setText("Processing Payment...");
                
                // Simulate payment
                paymentSimulator.simulatePayment(phoneNumber, NEW_CLIENT_AMOUNT, 
                        new PaymentSimulator.PaymentCallback() {
                            @Override
                            public void onSuccess(String transactionId) {
                                // Update payment with transaction ID
                                Map<String, Object> paymentUpdates = new HashMap<>();
                                paymentUpdates.put("transactionReference", transactionId);
                                paymentUpdates.put("status", "completed");
                                
                                firestoreService.updatePayment(createdPayment.getPaymentId(), paymentUpdates,
                                        new FirestoreService.FirestoreCallback() {
                                    @Override
                                    public void onSuccess(Object result) {
                                        // Update user subscription status
                                        // Set expiry to 3 minutes for testing
                                        Calendar calendar = Calendar.getInstance();
                                        calendar.add(Calendar.MINUTE, 3); // 3 minutes for testing
                                        Date expiryDate = calendar.getTime();
                                        
                                        Map<String, Object> userUpdates = new HashMap<>();
                                        userUpdates.put("isActive", true);
                                        userUpdates.put("subscriptionExpiryDate", expiryDate);
                                        userUpdates.put("dataUsedGB", 0.0);
                                        userUpdates.put("hasPaidBefore", true); // Mark that user has paid
                                        
                                        firestoreService.updateUser(userId, userUpdates, 
                                                new FirestoreService.FirestoreCallback() {
                                            @Override
                                            public void onSuccess(Object result) {
                                                Toast.makeText(NewClientActivity.this, 
                                                        "Payment successful! Subscription activated.", 
                                                        Toast.LENGTH_LONG).show();
                                                Intent resultIntent = new Intent();
                                                resultIntent.putExtra("subscriptionActivated", true);
                                                resultIntent.putExtra("expiryDate", expiryDate.getTime());
                                                setResult(RESULT_OK, resultIntent);
                                                finish();
                                            }

                                            @Override
                                            public void onFailure(String errorMessage) {
                                                subscribeButton.setEnabled(true);
                                                subscribeButton.setText("Subscribe Now");
                                                Toast.makeText(NewClientActivity.this, 
                                                        "Payment successful but failed to activate subscription: " + errorMessage, 
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        });
                                    }

                                    @Override
                                    public void onFailure(String errorMessage) {
                                        subscribeButton.setEnabled(true);
                                        subscribeButton.setText("Subscribe Now");
                                        Toast.makeText(NewClientActivity.this, 
                                                "Failed to update payment: " + errorMessage, 
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                subscribeButton.setEnabled(true);
                                subscribeButton.setText("Subscribe Now");
                                Toast.makeText(NewClientActivity.this, 
                                        "Payment failed: " + errorMessage, 
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }

            @Override
            public void onFailure(String errorMessage) {
                subscribeButton.setEnabled(true);
                subscribeButton.setText("Subscribe Now");
                Toast.makeText(NewClientActivity.this, 
                        "Failed to create payment: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }


    private void navigateToLogin() {
        Intent intent = new Intent(NewClientActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


