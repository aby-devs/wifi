# Firebase Backend Setup Guide

## Overview
The backend has been fully integrated with Firebase. The app now uses:
- **Firebase Authentication** for user login/signup
- **Cloud Firestore** for data storage (users, subscriptions, payments)
- **Basic payment processing** (simulated, not real M-Pesa integration)

## What Has Been Implemented

### 1. Firebase Dependencies
- Added Firebase BOM and dependencies to `app/build.gradle.kts`
- Added Google Services plugin

### 2. Data Models
Created in `app/src/main/java/com/example/wifi/models/`:
- **User.java** - User profile data
- **Subscription.java** - Subscription plans
- **Payment.java** - Payment records

### 3. Service Classes
Created in `app/src/main/java/com/example/wifi/services/`:
- **FirebaseAuthService.java** - Handles authentication (login, signup, password change)
- **FirestoreService.java** - Handles all database operations

### 4. Updated Activities
All activities now use Firebase:
- **LoginActivity** - Firebase Auth login
- **SignUpActivity** - Firebase Auth signup + creates user profile
- **MainActivity** - Fetches and displays user data from Firestore
- **NewClientActivity** - Creates subscription and processes payment
- **MonthlyRenewActivity** - Processes renewal payment
- **PaymentHistoryActivity** - Fetches and displays payment history
- **EditProfileActivity** - Updates user profile in Firestore
- **ChangePasswordActivity** - Changes password via Firebase Auth

## Required Setup Steps

### 1. Create Firebase Project
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter project name (e.g., "NoorTech WiFi")
4. Follow the setup wizard

### 2. Add Android App to Firebase
1. In Firebase Console, click "Add app" → Android
2. Enter package name: `com.example.wifi`
3. Download `google-services.json`
4. Place it in `app/` directory (same level as `build.gradle.kts`)

### 3. Enable Firebase Services

#### Authentication
1. Go to Firebase Console → Authentication
2. Click "Get started"
3. Enable "Email/Password" sign-in method

#### Firestore Database
1. Go to Firebase Console → Firestore Database
2. Click "Create database"
3. Start in **test mode** (for development)
4. Choose a location (e.g., `us-central1`)

### 4. Set Up Firestore Security Rules (Important!)

Go to Firestore Database → Rules and update with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - users can only read/write their own data
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Messages collection - top-level independent collection
    // Users can only read/write messages where userId matches their own
    match /messages/{messageId} {
      // Allow users to create messages with their own userId
      allow create: if request.auth != null && 
                       request.resource.data.userId == request.auth.uid;
      // Allow users to read messages where userId matches their own
      allow read: if request.auth != null && 
                     resource.data.userId == request.auth.uid;
      // Allow users to update their own messages
      allow update: if request.auth != null && 
                       resource.data.userId == request.auth.uid &&
                       request.resource.data.userId == request.auth.uid;
      // Allow users to delete their own messages
      allow delete: if request.auth != null && 
                       resource.data.userId == request.auth.uid;
    }
    
    // Subscriptions collection
    match /subscriptions/{subscriptionId} {
      // Users can read their own subscriptions
      allow read: if request.auth != null && 
                     resource.data.userId == request.auth.uid;
      // Users can create subscriptions for themselves
      allow create: if request.auth != null && 
                      request.resource.data.userId == request.auth.uid;
      // Users can update their own subscriptions (for activation, status changes)
      allow update: if request.auth != null && 
                       resource.data.userId == request.auth.uid &&
                       request.resource.data.userId == request.auth.uid;
      // Only admins can delete
      allow delete: if false;
    }
    
    // Payments collection
    match /payments/{paymentId} {
      // Users can read their own payments
      allow read: if request.auth != null && 
                     resource.data.userId == request.auth.uid;
      // Users can create payments for themselves
      allow create: if request.auth != null && 
                      request.resource.data.userId == request.auth.uid;
      // Users can update their own payments (for status changes)
      allow update: if request.auth != null && 
                       resource.data.userId == request.auth.uid &&
                       request.resource.data.userId == request.auth.uid;
      // Only admins can delete
      allow delete: if false;
    }
    
    // Deny all other access by default
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

**⚠️ SECURITY WARNING: Your current rules are too permissive!**

Your current rules (`allow read, write: if request.auth != null`) allow **any authenticated user to read/write ANY document** in your database. This is a **major security risk** because:
- Users can access other users' personal data
- Users can modify other users' subscriptions
- Users can see other users' payment information
- Users can read/write messages from other users

**The updated rules above provide:**
✅ Users can only access their own user document  
✅ Users can only read/write messages where `userId` matches their own  
✅ Users can only access their own subscriptions and payments  
✅ All other documents are denied by default  
✅ Prevents unauthorized access to other users' data

**To apply these rules:**
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Firestore Database** → **Rules** tab
4. Replace your current rules with the rules above
5. Click **"Publish"** to save

**After updating, test that:**
- ✅ Users can send and receive their own messages
- ✅ Users can view their own subscriptions
- ✅ Users can view their own payment history
- ✅ Users cannot access other users' data

## Database Structure

### Collections

#### `users`
- Document ID: `userId` (Firebase Auth UID)
- Fields:
  - `userId` (string)
  - `name` (string)
  - `email` (string)
  - `phone` (string)
  - `createdAt` (timestamp)
  - `updatedAt` (timestamp)
  - `isActive` (boolean)
  - `subscriptionExpiryDate` (timestamp)

#### `subscriptions`
- Document ID: Auto-generated
- Fields:
  - `subscriptionId` (string)
  - `userId` (string)
  - `planType` (string) - "new_client" or "monthly_renewal"
  - `amount` (number)
  - `setupFee` (number)
  - `totalAmount` (number)
  - `status` (string) - "pending", "active", "expired"
  - `startDate` (timestamp)
  - `expiryDate` (timestamp)
  - `createdAt` (timestamp)

#### `payments`
- Document ID: Auto-generated
- Fields:
  - `paymentId` (string)
  - `userId` (string)
  - `subscriptionId` (string)
  - `amount` (number)
  - `paymentMethod` (string) - "M-PESA"
  - `phoneNumber` (string)
  - `status` (string) - "pending", "completed", "failed"
  - `transactionReference` (string)
  - `paymentDate` (timestamp)
  - `createdAt` (timestamp)

## Payment Processing

**Note:** The current implementation uses **simulated payment processing**. Payments are automatically marked as "completed" without real M-Pesa integration.

To implement real M-Pesa integration:
1. Set up M-Pesa API credentials
2. Create a backend service (Firebase Cloud Functions or separate server)
3. Replace the simulated payment logic in `NewClientActivity` and `MonthlyRenewActivity`
4. Add webhook handlers for payment callbacks

## Testing

1. Build and run the app
2. Create a new account (Sign Up)
3. Login with the created account
4. Test subscription creation (New Client Plan)
5. Test monthly renewal
6. Check payment history
7. Update profile
8. Change password

## Troubleshooting

### Build Errors
- Ensure `google-services.json` is in the `app/` directory
- Sync Gradle files after adding dependencies
- Clean and rebuild the project

### Authentication Errors
- Verify Email/Password is enabled in Firebase Console
- Check that the package name matches in Firebase Console

### Firestore Errors
- Verify Firestore is created and enabled
- Check security rules are properly configured
- Ensure the database is in the correct mode (test mode for development)

### Data Not Loading
- Check internet connection
- Verify Firestore security rules allow the operation
- Check Logcat for error messages

## Next Steps (Optional Enhancements)

1. **Real M-Pesa Integration** - Replace simulated payments
2. **Push Notifications** - Add Firebase Cloud Messaging
3. **Analytics** - Add Firebase Analytics
4. **Cloud Functions** - For automated tasks (subscription expiry checks, etc.)
5. **Offline Support** - Enable Firestore offline persistence
6. **Error Handling** - Improve error messages and retry logic


