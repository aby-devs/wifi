package com.example.wifi;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.wifi.models.User;
import com.example.wifi.services.FirebaseAuthService;
import com.example.wifi.services.FirestoreService;
import com.google.android.material.button.MaterialButton;

import java.net.URLEncoder;

public class SupportActivity extends AppCompatActivity {

    private static final String TAG = "SupportActivity";
    private static final String ADMIN_WHATSAPP_NUMBER = "254701652148"; // Replace with actual admin WhatsApp number (country code + number, no + or spaces)
    
    private FirebaseAuthService authService;
    private FirestoreService firestoreService;
    private MaterialButton whatsappButton;
    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        authService = FirebaseAuthService.getInstance();
        firestoreService = FirestoreService.getInstance();

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        whatsappButton = findViewById(R.id.whatsappButton);
        whatsappButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWhatsApp();
            }
        });

        // Load user data to include in WhatsApp message
        loadUserData();
    }

    private void loadUserData() {
        if (authService.getCurrentUser() == null) {
            // User not logged in, can still use WhatsApp but without user context
            return;
        }

        String userId = authService.getCurrentUser().getUid();
        firestoreService.getUser(userId, new FirestoreService.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "Failed to load user data: " + errorMessage);
                // Continue without user data
            }
        });
    }

    private void openWhatsApp() {
        try {
            // Format the WhatsApp message with user context
            String message = formatWhatsAppMessage();
            
            // Encode the message for URL
            String encodedMessage = URLEncoder.encode(message, "UTF-8");
            
            // Create WhatsApp deep link URL
            String whatsappUrl = "https://wa.me/" + ADMIN_WHATSAPP_NUMBER + "?text=" + encodedMessage;
            
            Log.d(TAG, "Attempting to open WhatsApp with URL: " + whatsappUrl);
            
            // Create intent to open WhatsApp
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(whatsappUrl));
            
            // First, try to resolve the intent without specifying package
            // This will work if any app (WhatsApp, WhatsApp Business, etc.) can handle it
            PackageManager packageManager = getPackageManager();
            
            if (intent.resolveActivity(packageManager) != null) {
                // At least one app can handle this intent, try to open it
                try {
                    startActivity(intent);
                    Log.d(TAG, "Successfully opened WhatsApp using generic intent");
                    return;
                } catch (android.content.ActivityNotFoundException e) {
                    Log.e(TAG, "ActivityNotFoundException even though resolveActivity returned non-null", e);
                }
            }
            
            // If generic intent doesn't work, try with specific packages
            String[] whatsappPackages = {"com.whatsapp", "com.whatsapp.w4b"};
            boolean found = false;
            
            for (String pkg : whatsappPackages) {
                try {
                    packageManager.getPackageInfo(pkg, PackageManager.GET_ACTIVITIES);
                    // Package exists, try to open with this package
                    Intent packageIntent = new Intent(Intent.ACTION_VIEW);
                    packageIntent.setData(Uri.parse(whatsappUrl));
                    packageIntent.setPackage(pkg);
                    
                    if (packageIntent.resolveActivity(packageManager) != null) {
                        try {
                            startActivity(packageIntent);
                            Log.d(TAG, "Successfully opened WhatsApp with package: " + pkg);
                            found = true;
                            break;
                        } catch (android.content.ActivityNotFoundException e) {
                            Log.w(TAG, "Failed to open with package " + pkg, e);
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.d(TAG, "Package not found: " + pkg);
                }
            }
            
            if (!found) {
                // Last resort: try opening without any package specification
                // This should work if WhatsApp is installed and registered to handle wa.me URLs
                try {
                    Intent finalIntent = new Intent(Intent.ACTION_VIEW);
                    finalIntent.setData(Uri.parse(whatsappUrl));
                    startActivity(finalIntent);
                    Log.d(TAG, "Opened WhatsApp using final fallback");
                } catch (android.content.ActivityNotFoundException e) {
                    Log.e(TAG, "All methods failed - showing dialog", e);
                    showWhatsAppNotInstalledDialog();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error opening WhatsApp: " + e.getMessage(), e);
            Toast.makeText(this, "Unable to open WhatsApp. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatWhatsAppMessage() {
        StringBuilder message = new StringBuilder();
        
        message.append("Hello! I need support with my WiFi subscription.\n\n");
        
        // Add user information if available
        if (currentUser != null) {
            message.append("User Details:\n");
            
            if (currentUser.getName() != null && !currentUser.getName().isEmpty()) {
                message.append("Name: ").append(currentUser.getName()).append("\n");
            }
            
            if (currentUser.getEmail() != null && !currentUser.getEmail().isEmpty()) {
                message.append("Email: ").append(currentUser.getEmail()).append("\n");
            }
            
            if (currentUser.getPhone() != null && !currentUser.getPhone().isEmpty()) {
                message.append("Phone: ").append(currentUser.getPhone()).append("\n");
            }
            
            message.append("\n");
            
            // Add subscription status
            if (currentUser.isActive()) {
                message.append("Status: Active Subscription\n");
            } else {
                message.append("Status: Inactive/Expired\n");
            }
            
            if (currentUser.getSubscriptionExpiryDate() != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault());
                message.append("Expiry Date: ").append(sdf.format(currentUser.getSubscriptionExpiryDate())).append("\n");
            }
            
            message.append("\n");
        }
        
        message.append("How can you help me?");
        
        return message.toString();
    }

    private void showWhatsAppNotInstalledDialog() {
        new AlertDialog.Builder(this)
                .setTitle("WhatsApp Not Installed")
                .setMessage("WhatsApp is not installed on your device. Would you like to install it from the Play Store?")
                .setPositiveButton("Install WhatsApp", (dialog, which) -> {
                    // Open Play Store to install WhatsApp
                    try {
                        Intent playStoreIntent = new Intent(Intent.ACTION_VIEW);
                        playStoreIntent.setData(Uri.parse("market://details?id=com.whatsapp"));
                        startActivity(playStoreIntent);
                    } catch (android.content.ActivityNotFoundException e) {
                        // Play Store not available, open in browser
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                        browserIntent.setData(Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp"));
                        startActivity(browserIntent);
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Copy Number", (dialog, which) -> {
                    // Copy admin phone number to clipboard
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("WhatsApp Number", "+" + ADMIN_WHATSAPP_NUMBER);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Phone number copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}







