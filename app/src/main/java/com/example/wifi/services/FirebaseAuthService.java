package com.example.wifi.services;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthService {
    private static final String TAG = "FirebaseAuthService";
    private FirebaseAuth firebaseAuth;
    private static FirebaseAuthService instance;

    private FirebaseAuthService() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseAuthService getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthService();
        }
        return instance;
    }

    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public void signUp(String email, String password, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            callback.onSuccess(user);
                        } else {
                            Log.w(TAG, "createUserWithEmail:failure", task.getException());
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Sign up failed");
                        }
                    }
                });
    }

    public void signIn(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            callback.onSuccess(user);
                        } else {
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Sign in failed");
                        }
                    }
                });
    }

    public void signOut() {
        firebaseAuth.signOut();
    }

    public void changePassword(String currentPassword, String newPassword, AuthCallback callback) {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            callback.onFailure("No user is currently signed in");
            return;
        }

        if (user.getEmail() == null) {
            callback.onFailure("User email not found");
            return;
        }

        // Re-authenticate user first (required by Firebase)
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        
        user.reauthenticate(credential)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Re-authentication successful, now update password
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(TAG, "User password updated.");
                                                callback.onSuccess(user);
                                            } else {
                                                Log.w(TAG, "Password update failed", task.getException());
                                                callback.onFailure(task.getException() != null ? 
                                                        task.getException().getMessage() : "Password change failed");
                                            }
                                        }
                                    });
                        } else {
                            Log.w(TAG, "Re-authentication failed", task.getException());
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Current password is incorrect");
                        }
                    }
                });
    }

    public void sendPasswordResetEmail(String email, AuthCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Password reset email sent.");
                            callback.onSuccess(null);
                        } else {
                            Log.w(TAG, "Password reset failed", task.getException());
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Password reset failed");
                        }
                    }
                });
    }

    public interface AuthCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(String errorMessage);
    }
}




