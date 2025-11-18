package com.example.wifi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.example.wifi.models.User;
import com.example.wifi.services.FirebaseAuthService;
import com.example.wifi.services.FirestoreService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_DARK_MODE = "dark_mode";

    private TextInputEditText nameInput;
    private TextInputEditText phoneInput;
    private TextInputEditText emailInput;
    private MaterialButton saveButton;
    private FirebaseAuthService authService;
    private FirestoreService firestoreService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize theme
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ?
            AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        authService = FirebaseAuthService.getInstance();
        firestoreService = FirestoreService.getInstance();

        // Check if user is logged in
        if (authService.getCurrentUser() == null) {
            navigateToLogin();
            return;
        }

        ImageButton backButton = findViewById(R.id.backButton);
        nameInput = findViewById(R.id.nameInput);
        phoneInput = findViewById(R.id.phoneInput);
        emailInput = findViewById(R.id.emailInput);
        saveButton = findViewById(R.id.saveButton);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Load current user data
        loadUserData();

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameInput.getText().toString().trim();
                String phone = phoneInput.getText().toString().trim();
                String email = emailInput.getText().toString().trim();

                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(EditProfileActivity.this, "Please enter your name", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(phone)) {
                    Toast.makeText(EditProfileActivity.this, "Please enter your phone number", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(EditProfileActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                    return;
                }

                updateProfile(name, phone, email);
            }
        });
    }

    private void loadUserData() {
        String userId = authService.getCurrentUser().getUid();
        firestoreService.getUser(userId, new FirestoreService.UserCallback() {
            @Override
            public void onSuccess(User user) {
                if (user != null) {
                    nameInput.setText(user.getName());
                    phoneInput.setText(user.getPhone());
                    emailInput.setText(user.getEmail());
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(EditProfileActivity.this, 
                        "Failed to load profile: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateProfile(String name, String phone, String email) {
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        String userId = authService.getCurrentUser().getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("email", email);

        firestoreService.updateUser(userId, updates, new FirestoreService.FirestoreCallback() {
            @Override
            public void onSuccess(Object result) {
                Toast.makeText(EditProfileActivity.this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                saveButton.setEnabled(true);
                saveButton.setText("Save");
                Toast.makeText(EditProfileActivity.this, 
                        "Failed to update profile: " + errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(EditProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


