package com.example.wifi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wifi.models.Payment;
import com.example.wifi.services.FirebaseAuthService;
import com.example.wifi.services.FirestoreService;
import com.google.android.material.card.MaterialCardView;

import java.util.Date;
import java.util.List;

public class PaymentHistoryActivity extends AppCompatActivity {

    private ImageButton backButton;
    private LinearLayout paymentsContainer;
    private FirebaseAuthService authService;
    private FirestoreService firestoreService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        authService = FirebaseAuthService.getInstance();
        firestoreService = FirestoreService.getInstance();

        // Check if user is logged in
        if (authService.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }

        backButton = findViewById(R.id.backButton);
        paymentsContainer = findViewById(R.id.paymentsContainer);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadPayments();
    }

    private void loadPayments() {
        String userId = authService.getCurrentUser().getUid();
        if (userId == null || userId.isEmpty()) {
            Toast.makeText(PaymentHistoryActivity.this, 
                    "Error: User not found. Please log in again.", Toast.LENGTH_LONG).show();
            return;
        }
        
        firestoreService.getPaymentsByUser(userId, new FirestoreService.PaymentsCallback() {
            @Override
            public void onSuccess(List<Payment> payments) {
                if (payments == null) {
                    Toast.makeText(PaymentHistoryActivity.this, 
                            "No payments found", Toast.LENGTH_SHORT).show();
                    displayPayments(new java.util.ArrayList<Payment>());
                } else {
                    displayPayments(payments);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                android.util.Log.e("PaymentHistory", "Failed to load payments: " + errorMessage);
                Toast.makeText(PaymentHistoryActivity.this, 
                        "Failed to load payments: " + errorMessage, Toast.LENGTH_LONG).show();
                // Still show empty state
                displayPayments(new java.util.ArrayList<Payment>());
            }
        });
    }

    private void displayPayments(List<Payment> payments) {
        paymentsContainer.removeAllViews();

        if (payments.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No payment history found");
            emptyText.setTextSize(14);
            emptyText.setPadding(32, 32, 32, 32);
            paymentsContainer.addView(emptyText);
            return;
        }

        for (Payment payment : payments) {
            MaterialCardView cardView = createPaymentCard(payment);
            paymentsContainer.addView(cardView);
        }
    }

    private MaterialCardView createPaymentCard(Payment payment) {
        MaterialCardView cardView = new MaterialCardView(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 12, 0, 0);
        cardView.setLayoutParams(cardParams);
        cardView.setCardBackgroundColor(getResources().getColor(R.color.surface_dark));
        cardView.setRadius(16);
        cardView.setCardElevation(4);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        // Date and Amount row
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        TextView dateText = new TextView(this);
        dateText.setText(formatDate(payment.getPaymentDate()));
        dateText.setTextSize(12);
        dateText.setTextColor(getResources().getColor(R.color.text_primary));
        dateText.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.0f
        ));

        TextView amountText = new TextView(this);
        amountText.setText("Ksh " + String.format("%.0f", payment.getAmount()));
        amountText.setTextSize(15);
        amountText.setTextColor(getResources().getColor(
                payment.getStatus().equals("completed") ? R.color.accent_green : R.color.accent_orange
        ));
        amountText.setTypeface(null, android.graphics.Typeface.BOLD);

        row1.addView(dateText);
        row1.addView(amountText);
        layout.addView(row1);

        // Payment type
        TextView typeText = new TextView(this);
        typeText.setText(getPaymentTypeText(payment));
        typeText.setTextSize(12);
        typeText.setTextColor(getResources().getColor(R.color.text_secondary));
        typeText.setPadding(0, 4, 0, 0);
        layout.addView(typeText);

        cardView.addView(layout);
        return cardView;
    }

    private String formatDate(Date date) {
        if (date == null) return "";
        return DateFormat.format("dd MMM yyyy", date).toString();
    }

    private String getPaymentTypeText(Payment payment) {
        String paymentType;
        if (payment.isFirstTimePayment()) {
            paymentType = "First Time Payment";
        } else if (payment.isMonthlyRenewal()) {
            paymentType = "Monthly Renewal";
        } else {
            paymentType = "Payment";
        }
        return paymentType + " - " + payment.getPaymentMethod();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(PaymentHistoryActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


