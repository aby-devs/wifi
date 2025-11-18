package com.example.wifi.services;

import android.util.Log;

import com.example.wifi.models.ChatMessage;
import com.example.wifi.models.Payment;
import com.example.wifi.models.Subscription;
import com.example.wifi.models.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;

public class FirestoreService {
    private static final String TAG = "FirestoreService";
    private FirebaseFirestore db;
    private static FirestoreService instance;

    private FirestoreService() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreService getInstance() {
        if (instance == null) {
            instance = new FirestoreService();
        }
        return instance;
    }

    // User Operations
    public void createUser(User user, FirestoreCallback callback) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("name", user.getName());
        userMap.put("email", user.getEmail());
        userMap.put("phone", user.getPhone());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", user.getUpdatedAt());
        userMap.put("isActive", user.isActive());
        userMap.put("subscriptionExpiryDate", user.getSubscriptionExpiryDate());
        userMap.put("dataUsedGB", user.getDataUsedGB());
        userMap.put("hasPaidBefore", false); // Track if user has ever paid

        // New users are stored in the "users" collection
        String collectionName = "users";
        
        db.collection(collectionName).document(user.getUserId())
                .set(userMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User document created successfully in " + collectionName);
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error creating user document", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void getUser(String userId, UserCallback callback) {
        // Try active_users first, then expired_users, then users
        db.collection("active_users").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                processUserDocument(document, callback);
                            } else {
                                // Not in active_users, try expired_users
                                db.collection("expired_users").document(userId)
                                        .get()
                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                if (task.isSuccessful()) {
                                                    DocumentSnapshot doc = task.getResult();
                                                    if (doc != null && doc.exists()) {
                                                        processUserDocument(doc, callback);
                                                    } else {
                                                        // Not in expired_users, try users
                                                        db.collection("users").document(userId)
                                                                .get()
                                                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                                    @Override
                                                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                        if (task.isSuccessful()) {
                                                                            DocumentSnapshot newDoc = task.getResult();
                                                                            if (newDoc != null && newDoc.exists()) {
                                                                                processUserDocument(newDoc, callback);
                                                                            } else {
                                                                                callback.onFailure("User not found");
                                                                            }
                                                                        } else {
                                                                            Log.w(TAG, "Error getting user from users", task.getException());
                                                                            callback.onFailure(task.getException() != null ? 
                                                                                    task.getException().getMessage() : "Failed to get user");
                                                                        }
                                                                    }
                                                                });
                                                    }
                                                } else {
                                                    Log.w(TAG, "Error getting user from expired_users", task.getException());
                                                    callback.onFailure(task.getException() != null ? 
                                                            task.getException().getMessage() : "Failed to get user");
                                                }
                                            }
                                        });
                            }
                        } else {
                            Log.w(TAG, "Error getting user from active_users", task.getException());
                            callback.onFailure(task.getException() != null ? 
                                    task.getException().getMessage() : "Failed to get user");
                        }
                    }
                });
    }
    
    private void processUserDocument(DocumentSnapshot document, UserCallback callback) {
        User user = document.toObject(User.class);
        
        // Manually handle Firestore Timestamp conversion for dates
        if (user != null) {
            // Convert Firestore Timestamp to Date for subscriptionExpiryDate
            if (document.getTimestamp("subscriptionExpiryDate") != null) {
                com.google.firebase.Timestamp timestamp = document.getTimestamp("subscriptionExpiryDate");
                if (timestamp != null) {
                    user.setSubscriptionExpiryDate(timestamp.toDate());
                }
            }
            
            // Convert other date fields if needed
            if (document.getTimestamp("createdAt") != null) {
                com.google.firebase.Timestamp timestamp = document.getTimestamp("createdAt");
                if (timestamp != null) {
                    user.setCreatedAt(timestamp.toDate());
                }
            }
            
            if (document.getTimestamp("updatedAt") != null) {
                com.google.firebase.Timestamp timestamp = document.getTimestamp("updatedAt");
                if (timestamp != null) {
                    user.setUpdatedAt(timestamp.toDate());
                }
            }
            
            // Handle hasPaidBefore field (may not exist in old documents)
            if (document.contains("hasPaidBefore")) {
                Boolean hasPaid = document.getBoolean("hasPaidBefore");
                user.setHasPaidBefore(hasPaid != null && hasPaid);
            } else {
                user.setHasPaidBefore(false); // Default to false if not set
            }
            
            // Log for debugging
            Log.d(TAG, "User loaded - isActive: " + user.isActive() + 
                    ", hasPaidBefore: " + user.isHasPaidBefore() +
                    ", expiryDate: " + (user.getSubscriptionExpiryDate() != null ? 
                    user.getSubscriptionExpiryDate().toString() : "null"));
        }
        
        callback.onSuccess(user);
    }

    public void updateUser(String userId, Map<String, Object> updates, FirestoreCallback callback) {
        updates.put("updatedAt", new Date());
        
        // Check if isActive status is being updated - need to move between collections
        boolean isActiveUpdate = updates.containsKey("isActive");
        Boolean newIsActive = isActiveUpdate ? (Boolean) updates.get("isActive") : null;
        
        // Find which collection the user is currently in and get their current data
        findUserCollection(userId, new CollectionCallback() {
            @Override
            public void onFound(String currentCollection) {
                // Get current user data to check hasPaidBefore
                db.collection(currentCollection).document(userId)
                        .get()
                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                boolean currentHasPaidBefore = false;
                                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                    Boolean hasPaid = task.getResult().getBoolean("hasPaidBefore");
                                    currentHasPaidBefore = hasPaid != null && hasPaid;
                                }
                                
                                // Check if hasPaidBefore is being updated
                                boolean hasPaidBeforeUpdate = updates.containsKey("hasPaidBefore");
                                boolean finalHasPaidBefore = hasPaidBeforeUpdate ? 
                                        (Boolean) updates.get("hasPaidBefore") : currentHasPaidBefore;
                                
                                // Determine target collection based on status
                                String targetCollection;
                                if (newIsActive != null && newIsActive) {
                                    targetCollection = "active_users"; // Active subscription
                                } else if (finalHasPaidBefore) {
                                    targetCollection = "expired_users"; // Has paid before but expired
                                } else {
                                    targetCollection = "users"; // Never paid - new unactivated users
                                }
                                
                                Log.d(TAG, "User found in " + currentCollection + ", target: " + targetCollection + 
                                        " (isActive: " + newIsActive + ", hasPaidBefore: " + finalHasPaidBefore + ")");
                                
                                // If collections are different, move user
                                if (!currentCollection.equals(targetCollection)) {
                                    Log.d(TAG, "Moving user from " + currentCollection + " to " + targetCollection);
                                    moveUserBetweenCollections(userId, currentCollection, targetCollection, updates, callback);
                                } else {
                                    // Just update in current collection
                                    db.collection(currentCollection).document(userId)
                                            .update(updates)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Log.d(TAG, "User document updated successfully in " + currentCollection);
                                                    callback.onSuccess(null);
                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.w(TAG, "Error updating user document", e);
                                                    callback.onFailure(e.getMessage());
                                                }
                                            });
                                }
                            }
                        });
            }
            
            @Override
            public void onNotFound() {
                // User not found - determine target collection from updates
                boolean hasPaidBefore = updates.containsKey("hasPaidBefore") && 
                                       (Boolean) updates.get("hasPaidBefore");
                String targetCollection;
                if (newIsActive != null && newIsActive) {
                    targetCollection = "active_users";
                } else if (hasPaidBefore) {
                    targetCollection = "expired_users";
                } else {
                    targetCollection = "users"; // Never paid - new unactivated users
                }
                
                Log.d(TAG, "User not found in any collection, creating in " + targetCollection);
                Map<String, Object> userData = new HashMap<>();
                userData.putAll(updates);
                userData.put("userId", userId);
                
                db.collection(targetCollection).document(userId)
                        .set(userData)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG, "User document created in " + targetCollection);
                                callback.onSuccess(null);
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error creating user document", e);
                                callback.onFailure(e.getMessage());
                            }
                        });
            }
        });
    }
    
    private interface CollectionCallback {
        void onFound(String collectionName);
        void onNotFound();
    }
    
    private void findUserCollection(String userId, CollectionCallback callback) {
        // Check active_users first
        db.collection("active_users").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            callback.onFound("active_users");
                        } else {
                            // Check expired_users
                            db.collection("expired_users").document(userId)
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                                callback.onFound("expired_users");
                                            } else {
                                                // Check users (for new unactivated users)
                                                db.collection("users").document(userId)
                                                        .get()
                                                        .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                                                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                                                                    callback.onFound("users");
                                                                } else {
                                                                    callback.onNotFound();
                                                                }
                                                            }
                                                        });
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
    
    private void moveUserBetweenCollections(String userId, String fromCollection, String toCollection, 
                                           Map<String, Object> updates, FirestoreCallback callback) {
        // Get user data from source collection
        db.collection(fromCollection).document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                            Map<String, Object> userData = task.getResult().getData();
                            if (userData != null) {
                                // Apply updates
                                userData.putAll(updates);
                                
                                // Write to target collection
                                db.collection(toCollection).document(userId)
                                        .set(userData)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                // Delete from source collection
                                                db.collection(fromCollection).document(userId)
                                                        .delete()
                                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                            @Override
                                                            public void onSuccess(Void aVoid) {
                                                                Log.d(TAG, "User moved from " + fromCollection + " to " + toCollection);
                                                                callback.onSuccess(null);
                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {
                                                                Log.w(TAG, "Error deleting from source collection", e);
                                                                // Still consider it success since user is in target collection
                                                                callback.onSuccess(null);
                                                            }
                                                        });
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Log.w(TAG, "Error moving user to target collection", e);
                                                callback.onFailure(e.getMessage());
                                            }
                                        });
                            } else {
                                callback.onFailure("User data is null");
                            }
                        } else {
                            callback.onFailure("User not found in source collection");
                        }
                    }
                });
    }

    // Subscription Operations
    public void createSubscription(Subscription subscription, SubscriptionCallback callback) {
        Map<String, Object> subMap = new HashMap<>();
        subMap.put("userId", subscription.getUserId());
        subMap.put("planType", subscription.getPlanType());
        subMap.put("amount", subscription.getAmount());
        subMap.put("setupFee", subscription.getSetupFee());
        subMap.put("totalAmount", subscription.getTotalAmount());
        subMap.put("status", subscription.getStatus());
        subMap.put("startDate", subscription.getStartDate());
        subMap.put("expiryDate", subscription.getExpiryDate());
        subMap.put("createdAt", subscription.getCreatedAt());

        db.collection("subscriptions")
                .add(subMap)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Subscription created with ID: " + documentReference.getId());
                        subscription.setSubscriptionId(documentReference.getId());
                        callback.onSuccess(subscription);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error creating subscription", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void activateSubscription(String subscriptionId, FirestoreCallback callback) {
        Calendar calendar = Calendar.getInstance();
        Date startDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, 30);
        Date expiryDate = calendar.getTime();

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "active");
        updates.put("startDate", startDate);
        updates.put("expiryDate", expiryDate);

        db.collection("subscriptions").document(subscriptionId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Subscription activated successfully");
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error activating subscription", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void extendSubscription(String userId, FirestoreCallback callback) {
        // Get the latest subscription for the user
        db.collection("subscriptions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "active")
                .orderBy("expiryDate", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            QueryDocumentSnapshot document = (QueryDocumentSnapshot) task.getResult().getDocuments().get(0);
                            Date currentExpiry = document.getDate("expiryDate");
                            
                            Calendar calendar = Calendar.getInstance();
                            if (currentExpiry != null && currentExpiry.after(new Date())) {
                                calendar.setTime(currentExpiry);
                            } else {
                                calendar.setTime(new Date());
                            }
                            calendar.add(Calendar.DAY_OF_MONTH, 30);
                            Date newExpiryDate = calendar.getTime();

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("expiryDate", newExpiryDate);

                            document.getReference().update(updates)
                                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            Log.d(TAG, "Subscription extended successfully");
                                            callback.onSuccess(null);
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Log.w(TAG, "Error extending subscription", e);
                                            callback.onFailure(e.getMessage());
                                        }
                                    });
                        } else {
                            // No active subscription found, create a new one
                            callback.onFailure("No active subscription found");
                        }
                    }
                });
    }

    // Payment Operations
    public void createPayment(Payment payment, PaymentCallback callback) {
        Map<String, Object> paymentMap = new HashMap<>();
        paymentMap.put("userId", payment.getUserId());
        paymentMap.put("amount", payment.getAmount());
        paymentMap.put("paymentMethod", payment.getPaymentMethod());
        paymentMap.put("phoneNumber", payment.getPhoneNumber());
        paymentMap.put("status", payment.getStatus());
        paymentMap.put("transactionReference", payment.getTransactionReference());
        paymentMap.put("paymentDate", payment.getPaymentDate());
        paymentMap.put("createdAt", payment.getCreatedAt());
        paymentMap.put("firstTimePayment", payment.isFirstTimePayment());
        paymentMap.put("monthlyRenewal", payment.isMonthlyRenewal());

        db.collection("payments")
                .add(paymentMap)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d(TAG, "Payment created with ID: " + documentReference.getId());
                        payment.setPaymentId(documentReference.getId());
                        callback.onSuccess(payment);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error creating payment", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void updatePaymentStatus(String paymentId, String status, FirestoreCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);

        db.collection("payments").document(paymentId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Payment status updated successfully");
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating payment status", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }
    
    public void updatePayment(String paymentId, Map<String, Object> updates, FirestoreCallback callback) {
        db.collection("payments").document(paymentId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Payment updated successfully");
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating payment", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void getPaymentsByUser(String userId, PaymentsCallback callback) {
        // First try with orderBy (requires index)
        db.collection("payments")
                .whereEqualTo("userId", userId)
                .orderBy("paymentDate", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<Payment> payments = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    Payment payment = processPaymentDocument(document);
                                    payments.add(payment);
                                } catch (Exception e) {
                                    Log.e(TAG, "Error processing payment document: " + document.getId(), e);
                                }
                            }
                            // Sort payments by date in descending order (newest first)
                            payments.sort((p1, p2) -> {
                                Date date1 = p1.getPaymentDate();
                                Date date2 = p2.getPaymentDate();
                                if (date1 == null && date2 == null) return 0;
                                if (date1 == null) return 1;
                                if (date2 == null) return -1;
                                return date2.compareTo(date1); // Descending order
                            });
                            Log.d(TAG, "Successfully loaded " + payments.size() + " payments for user: " + userId);
                            callback.onSuccess(payments);
                        } else {
                            Exception exception = task.getException();
                            Log.w(TAG, "Query with orderBy failed, trying without orderBy. Error: " + 
                                    (exception != null ? exception.getMessage() : "Unknown error"));
                            
                            // Fallback: Query without orderBy (no index needed)
                            db.collection("payments")
                                    .whereEqualTo("userId", userId)
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                List<Payment> payments = new ArrayList<>();
                                                for (QueryDocumentSnapshot document : task.getResult()) {
                                                    try {
                                                        Payment payment = processPaymentDocument(document);
                                                        payments.add(payment);
                                                    } catch (Exception e) {
                                                        Log.e(TAG, "Error processing payment document: " + document.getId(), e);
                                                    }
                                                }
                                                // Sort payments by date in descending order (newest first)
                                                payments.sort((p1, p2) -> {
                                                    Date date1 = p1.getPaymentDate();
                                                    Date date2 = p2.getPaymentDate();
                                                    if (date1 == null && date2 == null) return 0;
                                                    if (date1 == null) return 1;
                                                    if (date2 == null) return -1;
                                                    return date2.compareTo(date1); // Descending order
                                                });
                                                Log.d(TAG, "Successfully loaded " + payments.size() + " payments for user: " + userId + " (without index)");
                                                callback.onSuccess(payments);
                                            } else {
                                                Exception fallbackException = task.getException();
                                                Log.e(TAG, "Error getting payments for user: " + userId, fallbackException);
                                                String errorMessage = "Failed to load payments";
                                                if (fallbackException != null) {
                                                    errorMessage = fallbackException.getMessage();
                                                }
                                                callback.onFailure(errorMessage);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }
    
    private Payment processPaymentDocument(QueryDocumentSnapshot document) {
        Payment payment = new Payment();
        payment.setPaymentId(document.getId());
        payment.setUserId(document.getString("userId"));
        payment.setSubscriptionId(document.getString("subscriptionId"));
        payment.setAmount(document.getDouble("amount") != null ? document.getDouble("amount") : 0.0);
        payment.setPaymentMethod(document.getString("paymentMethod"));
        payment.setPhoneNumber(document.getString("phoneNumber"));
        payment.setStatus(document.getString("status"));
        payment.setTransactionReference(document.getString("transactionReference"));
        
        // Handle Firestore Timestamp conversion for dates
        if (document.get("paymentDate") != null) {
            if (document.get("paymentDate") instanceof com.google.firebase.Timestamp) {
                payment.setPaymentDate(((com.google.firebase.Timestamp) document.get("paymentDate")).toDate());
            } else if (document.get("paymentDate") instanceof Date) {
                payment.setPaymentDate((Date) document.get("paymentDate"));
            }
        }
        
        if (document.get("createdAt") != null) {
            if (document.get("createdAt") instanceof com.google.firebase.Timestamp) {
                payment.setCreatedAt(((com.google.firebase.Timestamp) document.get("createdAt")).toDate());
            } else if (document.get("createdAt") instanceof Date) {
                payment.setCreatedAt((Date) document.get("createdAt"));
            }
        }
        
        // Handle boolean fields
        Boolean firstTimePayment = document.getBoolean("firstTimePayment");
        payment.setFirstTimePayment(firstTimePayment != null && firstTimePayment);
        
        Boolean monthlyRenewal = document.getBoolean("monthlyRenewal");
        payment.setMonthlyRenewal(monthlyRenewal != null && monthlyRenewal);
        
        return payment;
    }

    // Update user subscription status
    public void updateUserSubscriptionStatus(String userId, boolean isActive, Date expiryDate, FirestoreCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isActive", isActive);
        updates.put("subscriptionExpiryDate", expiryDate);
        updates.put("updatedAt", new Date());

        db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "User subscription status updated successfully");
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating user subscription status", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }


    // Callback interfaces
    public interface FirestoreCallback {
        void onSuccess(Object result);
        void onFailure(String errorMessage);
    }

    public interface UserCallback {
        void onSuccess(User user);
        void onFailure(String errorMessage);
    }

    public interface SubscriptionCallback {
        void onSuccess(Subscription subscription);
        void onFailure(String errorMessage);
    }

    public interface PaymentCallback {
        void onSuccess(Payment payment);
        void onFailure(String errorMessage);
    }

    public interface PaymentsCallback {
        void onSuccess(List<Payment> payments);
        void onFailure(String errorMessage);
    }

    // Chat Message Operations
    public void sendChatMessage(String userId, String userName, String message, FirestoreCallback callback) {
        String messageId = db.collection("chatMessages").document().getId();
        Date timestamp = new Date();
        
        ChatMessage chatMessage = new ChatMessage(messageId, userId, userName, message, timestamp);
        
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("messageId", chatMessage.getMessageId());
        messageMap.put("userId", chatMessage.getUserId());
        messageMap.put("userName", chatMessage.getUserName());
        messageMap.put("message", chatMessage.getMessage());
        messageMap.put("timestamp", new Timestamp(chatMessage.getTimestamp()));
        messageMap.put("isRead", chatMessage.isRead());
        
        db.collection("chatMessages").document(messageId)
                .set(messageMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Chat message sent successfully");
                        callback.onSuccess(chatMessage);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error sending chat message", e);
                        callback.onFailure(e.getMessage());
                    }
                });
    }

    public void getChatMessages(ChatMessagesCallback callback) {
        db.collection("chatMessages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            List<ChatMessage> messages = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                ChatMessage message = document.toObject(ChatMessage.class);
                                // Ensure messageId is set from document ID
                                if (message.getMessageId() == null || message.getMessageId().isEmpty()) {
                                    message.setMessageId(document.getId());
                                }
                                // Convert Firestore Timestamp to Date
                                if (document.getTimestamp("timestamp") != null) {
                                    message.setTimestamp(document.getTimestamp("timestamp").toDate());
                                }
                                messages.add(message);
                            }
                            // Sort by timestamp in case orderBy didn't work
                            messages.sort((m1, m2) -> {
                                Date d1 = m1.getTimestamp();
                                Date d2 = m2.getTimestamp();
                                if (d1 == null && d2 == null) return 0;
                                if (d1 == null) return 1;
                                if (d2 == null) return -1;
                                return d1.compareTo(d2);
                            });
                            callback.onSuccess(messages);
                        } else {
                            // If orderBy fails (e.g., missing index), try without orderBy
                            Log.w(TAG, "Error getting chat messages with orderBy, trying without", task.getException());
                            db.collection("chatMessages")
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task2) {
                                            if (task2.isSuccessful()) {
                                                List<ChatMessage> messages = new ArrayList<>();
                                                for (QueryDocumentSnapshot document : task2.getResult()) {
                                                    ChatMessage message = document.toObject(ChatMessage.class);
                                                    if (message.getMessageId() == null || message.getMessageId().isEmpty()) {
                                                        message.setMessageId(document.getId());
                                                    }
                                                    if (document.getTimestamp("timestamp") != null) {
                                                        message.setTimestamp(document.getTimestamp("timestamp").toDate());
                                                    }
                                                    messages.add(message);
                                                }
                                                // Sort by timestamp manually
                                                messages.sort((m1, m2) -> {
                                                    Date d1 = m1.getTimestamp();
                                                    Date d2 = m2.getTimestamp();
                                                    if (d1 == null && d2 == null) return 0;
                                                    if (d1 == null) return 1;
                                                    if (d2 == null) return -1;
                                                    return d1.compareTo(d2);
                                                });
                                                callback.onSuccess(messages);
                                            } else {
                                                Log.w(TAG, "Error getting chat messages", task2.getException());
                                                callback.onFailure(task2.getException() != null ? 
                                                        task2.getException().getMessage() : "Failed to load messages");
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    public ListenerRegistration listenToChatMessages(final ChatMessagesCallback callback) {
        return db.collection("chatMessages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot snapshots, FirebaseFirestoreException e) {
                        if (e != null) {
                            Log.w(TAG, "Listen failed", e);
                            callback.onFailure(e.getMessage());
                            return;
                        }

                        List<ChatMessage> messages = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : snapshots) {
                            ChatMessage message = doc.toObject(ChatMessage.class);
                            // Ensure messageId is set from document ID
                            if (message.getMessageId() == null || message.getMessageId().isEmpty()) {
                                message.setMessageId(doc.getId());
                            }
                            // Convert Firestore Timestamp to Date
                            if (doc.getTimestamp("timestamp") != null) {
                                message.setTimestamp(doc.getTimestamp("timestamp").toDate());
                            }
                            messages.add(message);
                        }
                        callback.onSuccess(messages);
                    }
                });
    }

    public interface ChatMessagesCallback {
        void onSuccess(List<ChatMessage> messages);
        void onFailure(String errorMessage);
    }
}


