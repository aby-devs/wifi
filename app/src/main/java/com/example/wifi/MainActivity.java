package com.example.wifi;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wifi.models.ChatMessage;
import com.example.wifi.models.User;
import com.example.wifi.services.FirebaseAuthService;
import com.example.wifi.services.FirestoreService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_DARK_MODE = "dark_mode";

    private ImageButton logoutButton;
    private MaterialCardView newClientCard;
    private MaterialCardView monthlyRenewCard;
    private MaterialCardView supportCard;
    private MaterialCardView historyCard;
    private MaterialCardView settingsCard;
    private MaterialButton mpesaButton;
    private TextView welcomeText;
    private TextView phoneText;
    private TextView avatarText;
    private TextView activeLabel;
    private TextView activeBadge;
    private TextView expiryDateText;
    private TextView expiryLabel;
    private TextView daysRemainingText;
    
    private FirebaseAuthService authService;
    private FirestoreService firestoreService;
    
    // Flag to track if we just activated a subscription (to prevent overwriting)
    private boolean justActivatedSubscription = false;
    private Date pendingExpiryDate = null;
    
    // Chat UI elements
    private EditText chatMessageInput;
    private MaterialButton chatSendButton;
    private LinearLayout chatMessagesContainer;
    private ScrollView chatMessagesScrollView;
    private ListenerRegistration chatListener;
    
    // Handler for checking subscription expiry
    private Handler expiryCheckHandler;
    private Runnable expiryCheckRunnable;
    
    // Handler for countdown timer
    private Handler countdownHandler;
    private Runnable countdownRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? 
            AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        authService = FirebaseAuthService.getInstance();
        firestoreService = FirestoreService.getInstance();

        // Check if user is logged in
        if (authService.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }

        logoutButton = findViewById(R.id.logoutButton);
        newClientCard = findViewById(R.id.newClientCard);
        monthlyRenewCard = findViewById(R.id.monthlyRenewCard);
        supportCard = findViewById(R.id.supportCard);
        historyCard = findViewById(R.id.historyCard);
        settingsCard = findViewById(R.id.settingsCard);
        mpesaButton = findViewById(R.id.mpesaButton);
        welcomeText = findViewById(R.id.welcomeText);
        phoneText = findViewById(R.id.phoneText);
        avatarText = findViewById(R.id.avatarText);
        activeLabel = findViewById(R.id.activeLabel);
        activeBadge = findViewById(R.id.activeBadge);
        expiryDateText = findViewById(R.id.expiryDateText);
        expiryLabel = findViewById(R.id.expiryLabel);
        daysRemainingText = findViewById(R.id.daysRemainingText);

        // Load user data
        loadUserData();
        
        // Initialize chat UI
        initializeChat();
        
        // Load and listen to chat messages
        loadChatMessages();
        setupChatListener();

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });

        newClientCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, NewClientActivity.class);
                startActivityForResult(intent, 100); // Request code for new client subscription
            }
        });

        monthlyRenewCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MonthlyRenewActivity.class);
                startActivityForResult(intent, 101); // Request code for monthly renewal
            }
        });

        supportCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SupportActivity.class);
                startActivity(intent);
            }
        });

        historyCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, PaymentHistoryActivity.class);
                startActivity(intent);
            }
        });

        settingsCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(intent);
            }
        });

        mpesaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMpesaPaymentDialog();
            }
        });
    }

    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_logout, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);
        MaterialButton logoutButton = dialogView.findViewById(R.id.logoutButton);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                authService.signOut();
                navigateToLogin();
            }
        });

        dialog.show();
    }

    private void showMpesaPaymentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_mpesa_payment, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextInputEditText phoneInput = dialogView.findViewById(R.id.phoneInput);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancelButton);
        MaterialButton payButton = dialogView.findViewById(R.id.payButton);

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
                    Toast.makeText(MainActivity.this, "Please enter a phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (phoneNumber.length() < 10) {
                    Toast.makeText(MainActivity.this, "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Process payment
                Toast.makeText(MainActivity.this, "Payment request sent to " + phoneNumber, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void loadUserData() {
        if (authService.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }
        String userId = authService.getCurrentUser().getUid();
        firestoreService.getUser(userId, new FirestoreService.UserCallback() {
            @Override
            public void onSuccess(User user) {
                // Run UI updates on main thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (user != null) {
                            // If we just activated subscription, don't overwrite with potentially stale Firestore data
                            if (justActivatedSubscription && pendingExpiryDate != null) {
                                // Only update name/phone/avatar, but keep subscription status from pending data
                                if (welcomeText != null) {
                                    welcomeText.setText("Welcome, " + user.getName());
                                }
                                if (phoneText != null) {
                                    phoneText.setText(user.getPhone());
                                }
                                if (avatarText != null) {
                                    String initials = getInitials(user.getName());
                                    avatarText.setText(initials);
                                }
                                // Keep subscription status from pending activation
                                updateSubscriptionStatus(user); // This will use pendingExpiryDate due to the flag
                                return;
                            }
                            
                            // Normal case - update everything from Firestore
                            if (welcomeText != null) {
                                welcomeText.setText("Welcome, " + user.getName());
                            }
                            if (phoneText != null) {
                                phoneText.setText(user.getPhone());
                            }
                            
                            // Set avatar initials
                            if (avatarText != null) {
                                String initials = getInitials(user.getName());
                                avatarText.setText(initials);
                            }
                            
                            // Update subscription status and card visibility
                            updateSubscriptionStatus(user);
                            updateCardVisibility(user);
                        } else {
                            Toast.makeText(MainActivity.this, "User data is null", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // If we just activated, don't show error - keep the local update
                        if (!justActivatedSubscription) {
                            Toast.makeText(MainActivity.this, "Failed to load user data: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }
    

    private void updateSubscriptionStatus(User user) {
        // Ensure all views are not null
        if (activeLabel == null || expiryDateText == null || daysRemainingText == null || expiryLabel == null) {
            Toast.makeText(this, "Some views are null. Please check layout.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Date currentDate = new Date();
        Date expiryDate = null;
        boolean isCurrentlyActive = false;
        
        // If we just activated and have pending data, use that instead of potentially stale Firestore data
        if (justActivatedSubscription && pendingExpiryDate != null) {
            expiryDate = pendingExpiryDate;
            isCurrentlyActive = true;
        } else if (user.getSubscriptionExpiryDate() != null) {
            expiryDate = user.getSubscriptionExpiryDate();
            isCurrentlyActive = user.isActive() && expiryDate.after(currentDate);
            
            // Check if subscription has expired and update Firestore if needed
            if (user.isActive() && !expiryDate.after(currentDate)) {
                // Subscription expired - update Firestore
                Map<String, Object> updates = new HashMap<>();
                updates.put("isActive", false);
                firestoreService.updateUser(user.getUserId(), updates, new FirestoreService.FirestoreCallback() {
                    @Override
                    public void onSuccess(Object result) {
                        Log.d("MainActivity", "Subscription status updated to expired");
                    }
                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e("MainActivity", "Failed to update expired status: " + errorMessage);
                    }
                });
                isCurrentlyActive = false;
            }
        }
        
        // Update UI based on current status
        if (isCurrentlyActive && expiryDate != null) {
            // Subscription is active - start countdown timer
            activeLabel.setText("ACTIVE");
            // Set green color for ACTIVE status
            activeLabel.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
            if (activeBadge != null) {
                activeBadge.setText("ACTIVE");
                activeBadge.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
                activeBadge.setVisibility(View.VISIBLE);
            }
            
            expiryLabel.setText("Expires In");
            
            // Start real-time countdown that updates every second
            startCountdownTimer(expiryDate);
        } else {
            // Check if user has never paid (NOT ACTIVATED) or has expired (EXPIRED)
            boolean hasPaidBefore = user.isHasPaidBefore();
            
            if (!hasPaidBefore) {
                // New user who hasn't paid yet
                activeLabel.setText("NOT ACTIVATED");
                activeLabel.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
                if (activeBadge != null) {
                    activeBadge.setText("NOT ACTIVATED");
                    activeBadge.setTextColor(getResources().getColor(android.R.color.darker_gray, getTheme()));
                    activeBadge.setVisibility(View.VISIBLE);
                }
                expiryDateText.setText("No Subscription");
                expiryLabel.setText("");
                daysRemainingText.setText("0");
            } else {
                // User has paid before but subscription expired
                activeLabel.setText("EXPIRED");
                // Set red color for EXPIRED status
                activeLabel.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                if (activeBadge != null) {
                    activeBadge.setText("EXPIRED");
                    activeBadge.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                    activeBadge.setVisibility(View.VISIBLE);
                }
                expiryDateText.setText("Subscription Expired");
                expiryLabel.setText("");
                daysRemainingText.setText("0");
            }
        }
        
        // Start/restart expiry checking
        startExpiryChecking(user);
    }
    
    private void startCountdownTimer(Date expiryDate) {
        // Stop any existing countdown
        stopCountdownTimer();
        
        countdownHandler = new Handler(Looper.getMainLooper());
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                Date currentDate = new Date();
                long diffInMillis = expiryDate.getTime() - currentDate.getTime();
                
                if (diffInMillis > 0) {
                    // Still counting down
                    long seconds = diffInMillis / 1000;
                    
                    // Update UI
                    if (expiryDateText != null) {
                        expiryDateText.setText(seconds + " seconds");
                    }
                    if (daysRemainingText != null) {
                        daysRemainingText.setText(seconds + "s");
                    }
                    
                    // Continue countdown - update every second
                    countdownHandler.postDelayed(this, 1000);
                } else {
                    // Countdown reached zero - show expired in red
                    if (expiryDateText != null) {
                        expiryDateText.setText("0 seconds");
                    }
                    if (daysRemainingText != null) {
                        daysRemainingText.setText("0s");
                    }
                    
                    // Update status to EXPIRED in red immediately
                    if (activeLabel != null) {
                        activeLabel.setText("EXPIRED");
                        activeLabel.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                    }
                    if (activeBadge != null) {
                        activeBadge.setText("EXPIRED");
                        activeBadge.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                        activeBadge.setVisibility(View.VISIBLE);
                    }
                    if (expiryDateText != null) {
                        expiryDateText.setText("Subscription Expired");
                    }
                    if (expiryLabel != null) {
                        expiryLabel.setText("");
                    }
                    
                    // Trigger expiry check to update Firestore and move user to expired_users
                    String userId = authService.getCurrentUser() != null ? authService.getCurrentUser().getUid() : null;
                    if (userId != null) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("isActive", false);
                        Log.d("MainActivity", "Countdown reached zero - moving user from active_users to expired_users");
                        firestoreService.updateUser(userId, updates, new FirestoreService.FirestoreCallback() {
                            @Override
                            public void onSuccess(Object result) {
                                Log.d("MainActivity", "Subscription expired - user moved to expired_users collection");
                                // Reload user data to update UI (this will ensure consistency)
                                loadUserData();
                            }
                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e("MainActivity", "Failed to move user to expired_users: " + errorMessage);
                            }
                        });
                    }
                }
            }
        };
        
        // Start countdown immediately
        countdownHandler.post(countdownRunnable);
    }
    
    private void stopCountdownTimer() {
        if (countdownHandler != null && countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
        }
    }
    
    private void updateCardVisibility(User user) {
        // Keep both cards visible at all times - don't hide anything
        if (newClientCard != null) {
            newClientCard.setVisibility(View.VISIBLE);
        }
        
        if (monthlyRenewCard != null) {
            monthlyRenewCard.setVisibility(View.VISIBLE);
        }
    }
    
    private void startExpiryChecking(User user) {
        // Stop any existing checking
        stopExpiryChecking();
        
        if (user.getSubscriptionExpiryDate() == null || !user.isActive()) {
            return; // No expiry to check
        }
        
        String userId = user.getUserId();
        expiryCheckHandler = new Handler(Looper.getMainLooper());
        expiryCheckRunnable = new Runnable() {
            @Override
            public void run() {
                // Reload user data to check current status
                firestoreService.getUser(userId, new FirestoreService.UserCallback() {
                    @Override
                    public void onSuccess(User currentUser) {
                        if (currentUser == null) {
                            return;
                        }
                        
                        Date currentDate = new Date();
                        Date expiryDate = currentUser.getSubscriptionExpiryDate();
                        
                        // Check if subscription expired
                        if (expiryDate != null && 
                            !expiryDate.after(currentDate) && 
                            currentUser.isActive()) {
                            // Subscription expired - update Firestore
                            Map<String, Object> updates = new HashMap<>();
                            updates.put("isActive", false);
                            firestoreService.updateUser(userId, updates, new FirestoreService.FirestoreCallback() {
                                @Override
                                public void onSuccess(Object result) {
                                    // Reload user data to update UI
                                    loadUserData();
                                }
                                @Override
                                public void onFailure(String errorMessage) {
                                    Log.e("MainActivity", "Failed to update expired status: " + errorMessage);
                                    // Continue checking
                                    expiryCheckHandler.postDelayed(expiryCheckRunnable, 5000);
                                }
                            });
                        } else if (expiryDate != null && expiryDate.after(currentDate) && currentUser.isActive()) {
                            // Still active - check again in 5 seconds
                            expiryCheckHandler.postDelayed(expiryCheckRunnable, 5000);
                        }
                        // If not active or no expiry date, stop checking
                    }
                    
                    @Override
                    public void onFailure(String errorMessage) {
                        // Continue checking even on error
                        expiryCheckHandler.postDelayed(expiryCheckRunnable, 5000);
                    }
                });
            }
        };
        
        // Start checking every 5 seconds
        expiryCheckHandler.postDelayed(expiryCheckRunnable, 5000);
    }
    
    private void stopExpiryChecking() {
        if (expiryCheckHandler != null && expiryCheckRunnable != null) {
            expiryCheckHandler.removeCallbacks(expiryCheckRunnable);
        }
    }

    private String getInitials(String name) {
        if (name == null || name.isEmpty()) {
            return "U";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else {
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data when returning to this activity
        // But don't overwrite if we just activated a subscription
        if (authService.getCurrentUser() != null) {
            if (justActivatedSubscription) {
                // Use protected load to avoid overwriting
                loadUserDataWithProtection();
            } else {
                loadUserData();
            }
            // Restart expiry checking
            firestoreService.getUser(authService.getCurrentUser().getUid(), new FirestoreService.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    if (user != null) {
                        startExpiryChecking(user);
                    }
                }
                @Override
                public void onFailure(String errorMessage) {
                    // Ignore
                }
            });
        }
    }
    
    @Override
    protected void onStart() {
        super.onStart();
        // Also refresh when activity starts (in case user came from payment screen)
        if (authService.getCurrentUser() != null) {
            if (justActivatedSubscription) {
                // Use protected load to avoid overwriting
                loadUserDataWithProtection();
            } else {
                loadUserData();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Refresh data when returning from payment activities
        if ((requestCode == 100 || requestCode == 101) && resultCode == RESULT_OK) {
            // Payment was successful
            if (data != null && data.getBooleanExtra("subscriptionActivated", false)) {
                // Immediately update UI with subscription data (simulation mode)
                long expiryTimestamp = data.getLongExtra("expiryDate", 0);
                if (expiryTimestamp > 0) {
                    Date expiryDate = new Date(expiryTimestamp);
                    pendingExpiryDate = expiryDate;
                    justActivatedSubscription = true;
                    
                    // Update status to ACTIVE immediately
                    if (activeLabel != null) {
                        activeLabel.setText("ACTIVE");
                        activeLabel.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
                    }
                    if (activeBadge != null) {
                        activeBadge.setText("ACTIVE");
                        activeBadge.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
                        activeBadge.setVisibility(View.VISIBLE);
                    }
                    
                    updateCardsWithSubscriptionData(expiryDate);
                    
                    // Show success message
                    Toast.makeText(this, "Subscription renewed! Status changed to ACTIVE.", Toast.LENGTH_LONG).show();
                }
            }
            
            // Also refresh from Firestore, but with longer delay and only if data is valid
            if (authService.getCurrentUser() != null) {
                // Try multiple times to get the data from Firestore
                // First attempt after 2 seconds
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadUserDataWithProtection();
                    }
                }, 2000);
                
                // Second attempt after 4 seconds
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (justActivatedSubscription) {
                            loadUserDataWithProtection();
                        }
                    }
                }, 4000);
                
                // Third attempt after 6 seconds
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (justActivatedSubscription) {
                            loadUserDataWithProtection();
                        }
                    }
                }, 6000);
                
                // Only clear protection after 10 seconds if Firestore still doesn't have it
                // This gives Firestore plenty of time to sync
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (justActivatedSubscription) {
                            // One final attempt
                            loadUserDataWithProtection();
                            // If still protected after 10 seconds, keep the local data
                            // The protection will clear when Firestore finally syncs
                        }
                    }
                }, 10000);
            }
        }
    }
    
    // Load user data but protect against overwriting just-activated subscription
    private void loadUserDataWithProtection() {
        String userId = authService.getCurrentUser().getUid();
        firestoreService.getUser(userId, new FirestoreService.UserCallback() {
            @Override
            public void onSuccess(User user) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (user != null) {
                            // If we just activated and Firestore data shows active, use Firestore
                            // If we just activated but Firestore hasn't updated yet, keep our local update
                            if (justActivatedSubscription && pendingExpiryDate != null) {
                                // Check if Firestore has valid subscription data
                                boolean firestoreHasValidData = user.isActive() && 
                                        user.getSubscriptionExpiryDate() != null &&
                                        user.getSubscriptionExpiryDate().after(new Date());
                                
                                if (firestoreHasValidData) {
                                    // Firestore has the data, use it and clear the flag
                                    justActivatedSubscription = false;
                                    pendingExpiryDate = null;
                                    
                                    // Update all UI elements
                                    if (welcomeText != null) {
                                        welcomeText.setText("Welcome, " + user.getName());
                                    }
                                    if (phoneText != null) {
                                        phoneText.setText(user.getPhone());
                                    }
                                    if (avatarText != null) {
                                        String initials = getInitials(user.getName());
                                        avatarText.setText(initials);
                                    }
                                    updateSubscriptionStatus(user);
                                    updateCardVisibility(user);
                                } else {
                                    // Firestore doesn't have it yet, keep our local update
                                    // Re-apply the pending data to ensure it stays
                                    updateCardsWithSubscriptionData(pendingExpiryDate);
                                    // Don't overwrite with inactive data
                                    return;
                                }
                            } else {
                                // Normal case - just update from Firestore
                                if (welcomeText != null) {
                                    welcomeText.setText("Welcome, " + user.getName());
                                }
                                if (phoneText != null) {
                                    phoneText.setText(user.getPhone());
                                }
                                if (avatarText != null) {
                                    String initials = getInitials(user.getName());
                                    avatarText.setText(initials);
                                }
                                updateSubscriptionStatus(user);
                                updateCardVisibility(user);
                            }
                        }
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // If we just activated, keep the local update even if Firestore fails
                        if (justActivatedSubscription && pendingExpiryDate != null) {
                            // Re-apply the pending data to ensure it stays
                            updateCardsWithSubscriptionData(pendingExpiryDate);
                        } else if (!justActivatedSubscription) {
                            Toast.makeText(MainActivity.this, "Failed to load user data: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
    }
    
    // Helper method to immediately update cards with subscription data (for simulation)
    private void updateCardsWithSubscriptionData(Date expiryDate) {
        // Update Active Status Card
        if (activeLabel != null) {
            activeLabel.setText("ACTIVE");
            activeLabel.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
        }
        if (activeBadge != null) {
            activeBadge.setText("ACTIVE");
            activeBadge.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));
            activeBadge.setVisibility(View.VISIBLE);
        }
        if (expiryLabel != null) {
            expiryLabel.setText("Expires In");
        }
        
        // Start countdown timer for real-time updates
        startCountdownTimer(expiryDate);
        
        // Keep both cards visible - don't hide anything
        if (newClientCard != null) {
            newClientCard.setVisibility(View.VISIBLE);
        }
        if (monthlyRenewCard != null) {
            monthlyRenewCard.setVisibility(View.VISIBLE);
        }
    }

    // Chat Methods
    private void initializeChat() {
        chatMessageInput = findViewById(R.id.chatMessageInput);
        chatSendButton = findViewById(R.id.chatSendButton);
        chatMessagesContainer = findViewById(R.id.chatMessagesContainer);
        chatMessagesScrollView = findViewById(R.id.chatMessagesScrollView);

        // Update input field background based on theme
        boolean isDarkMode = (getResources().getConfiguration().uiMode & 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES;
        chatMessageInput.setBackgroundResource(isDarkMode ? 
                R.drawable.chat_input_background_dark : R.drawable.chat_input_background);

        chatSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendChatMessage();
            }
        });
    }

    private void sendChatMessage() {
        if (authService.getCurrentUser() == null) {
            Toast.makeText(this, "Please login to send messages", Toast.LENGTH_SHORT).show();
            navigateToLogin();
            return;
        }
        
        String message = chatMessageInput.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            return;
        }

        String userId = authService.getCurrentUser().getUid();
        String userName = authService.getCurrentUser().getDisplayName();
        if (userName == null || userName.isEmpty()) {
            // Get user name from Firestore
            firestoreService.getUser(userId, new FirestoreService.UserCallback() {
                @Override
                public void onSuccess(User user) {
                    String name = user != null ? user.getName() : "User";
                    sendMessageToFirestore(userId, name, message);
                }

                @Override
                public void onFailure(String errorMessage) {
                    sendMessageToFirestore(userId, "User", message);
                }
            });
        } else {
            sendMessageToFirestore(userId, userName, message);
        }
    }

    private void sendMessageToFirestore(String userId, String userName, String message) {
        chatSendButton.setEnabled(false);
        firestoreService.sendChatMessage(userId, userName, message, new FirestoreService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chatMessageInput.setText("");
                        chatSendButton.setEnabled(true);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "Failed to send message: " + errorMessage, Toast.LENGTH_SHORT).show();
                        chatSendButton.setEnabled(true);
                    }
                });
            }
        });
    }

    private void loadChatMessages() {
        firestoreService.getChatMessages(new FirestoreService.ChatMessagesCallback() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayMessages(messages);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("MainActivity", "Failed to load chat messages: " + errorMessage);
            }
        });
    }

    private void setupChatListener() {
        chatListener = firestoreService.listenToChatMessages(new FirestoreService.ChatMessagesCallback() {
            @Override
            public void onSuccess(List<ChatMessage> messages) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayMessages(messages);
                    }
                });
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("MainActivity", "Chat listener error: " + errorMessage);
            }
        });
    }

    private void displayMessages(List<ChatMessage> messages) {
        if (chatMessagesContainer == null) {
            Log.e("MainActivity", "chatMessagesContainer is null");
            return;
        }
        
        chatMessagesContainer.removeAllViews();

        if (messages == null || messages.isEmpty()) {
            Log.d("MainActivity", "No messages to display");
            return;
        }

        Log.d("MainActivity", "Displaying " + messages.size() + " messages");
        String currentUserId = authService.getCurrentUser() != null ? authService.getCurrentUser().getUid() : null;
        
        for (ChatMessage message : messages) {
            if (message == null) {
                continue;
            }
            // Log message details for debugging
            Log.d("MainActivity", "Message from userId: " + message.getUserId() + ", userName: " + message.getUserName() + ", currentUserId: " + currentUserId);
            View messageView = createMessageView(message);
            if (messageView != null) {
                chatMessagesContainer.addView(messageView);
            }
        }

        // Scroll to bottom
        if (chatMessagesScrollView != null) {
            chatMessagesScrollView.post(new Runnable() {
                @Override
                public void run() {
                    chatMessagesScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    private View createMessageView(ChatMessage message) {
        if (authService.getCurrentUser() == null || message == null) {
            // Return empty view if user is null or message is null
            return new LinearLayout(this);
        }
        String currentUserId = authService.getCurrentUser().getUid();
        boolean isCurrentUser = message.getUserId() != null && message.getUserId().equals(currentUserId);
        
        // Main container for the message
        LinearLayout messageLayout = new LinearLayout(this);
        messageLayout.setOrientation(LinearLayout.HORIZONTAL);
        messageLayout.setPadding(8, 4, 8, 4);
        messageLayout.setGravity(isCurrentUser ? Gravity.END : Gravity.START);
        
        // Message bubble container
        LinearLayout messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);
        messageContainer.setPadding(12, 10, 12, 10);
        
        // Set bubble background based on user and theme
        boolean isDarkMode = (getResources().getConfiguration().uiMode & 
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) == 
                android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        if (isCurrentUser) {
            // User's own message - green bubble (like WhatsApp)
            messageContainer.setBackgroundResource(isDarkMode ? 
                    R.drawable.chat_bubble_sent_dark : R.drawable.chat_bubble_sent);
        } else {
            // Other user's message - white/gray bubble
            messageContainer.setBackgroundResource(isDarkMode ? 
                    R.drawable.chat_bubble_received_dark : R.drawable.chat_bubble_received);
        }
        
        // User name (only show for other users)
        if (!isCurrentUser) {
            TextView userNameText = new TextView(this);
            userNameText.setText(message.getUserName());
            userNameText.setTextColor(getResources().getColor(R.color.text_secondary));
            userNameText.setTextSize(11);
            userNameText.setTypeface(null, android.graphics.Typeface.BOLD);
            userNameText.setPadding(0, 0, 0, 4);
            messageContainer.addView(userNameText);
        }

        // Message text
        TextView messageText = new TextView(this);
        messageText.setText(message.getMessage());
        messageText.setTextColor(isCurrentUser ? 
                getResources().getColor(android.R.color.black) : 
                getResources().getColor(R.color.text_primary));
        messageText.setTextSize(15);
        messageText.setPadding(0, 0, 0, 4);
        messageText.setLineSpacing(4, 1.0f);
        messageContainer.addView(messageText);

        // Timestamp
        TextView timestampText = new TextView(this);
        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Date messageTimestamp = message.getTimestamp();
        String formattedDate = messageTimestamp != null ? dateFormat.format(messageTimestamp) : "Now";
        timestampText.setText(formattedDate);
        timestampText.setTextColor(getResources().getColor(R.color.text_secondary));
        timestampText.setTextSize(10);
        timestampText.setGravity(Gravity.END);
        messageContainer.addView(timestampText);

        // Set max width for message bubble (80% of screen)
        int maxWidth = (int) (getResources().getDisplayMetrics().widthPixels * 0.75);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        containerParams.width = Math.min(maxWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        messageContainer.setLayoutParams(containerParams);
        
        messageLayout.addView(messageContainer);

        // Set layout params for main container
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        messageLayout.setLayoutParams(params);

        return messageLayout;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopExpiryChecking();
        stopCountdownTimer();
        if (chatListener != null) {
            chatListener.remove();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopExpiryChecking();
        stopCountdownTimer();
    }
    
}