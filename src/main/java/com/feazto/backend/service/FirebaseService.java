package com.feazto.backend.service;

import com.feazto.backend.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import org.springframework.beans.factory.annotation.Value;

import java.util.HashMap;
import java.util.Map;

@Service
public class FirebaseService {

    @Value("${feazto.firebase.api-key:}")
    private String apiKey;

    public void saveUserToFirebase(User user) {
        try {
            // Get reference to Cloud Firestore
            Firestore db = FirestoreClient.getFirestore();
            DocumentReference docRef = db.collection("users").document(user.getId().toString());

            Map<String, Object> userData = new HashMap<>();
            userData.put("id", user.getId().toString());
            userData.put("name", user.getName());
            userData.put("phone", user.getPhone());
            userData.put("email", user.getEmail());
            userData.put("city", user.getCity());
            userData.put("foodPref", user.getFoodPref());
            userData.put("password", user.getPassword());
            userData.put("username", user.getUsername());
            userData.put("photo", user.getPhoto());

            // Write asynchronously to Cloud Firestore
            ApiFuture<WriteResult> result = docRef.set(userData);
            
            // Log once the write completes
            result.addListener(() -> {
                try {
                    result.get(); // Check if write succeeded
                    System.out.println("✅ Cloud Firestore Sync complete: users/" + user.getId());
                } catch (Exception e) {
                    System.err.println("❌ Failed to sync to Cloud Firestore: " + e.getMessage());
                }
            }, Runnable::run);

        } catch (Exception e) {
            System.err.println("❌ Firebase Firestore sync error: " + e.getMessage());
        }
    }

    public User findUserByEmailInFirestore(String email) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            var query = db.collection("users").whereEqualTo("email", email).get();
            var documents = query.get().getDocuments();
            if (!documents.isEmpty()) {
                // Find a document that has a non-null password
                com.google.cloud.firestore.QueryDocumentSnapshot doc = null;
                for (var d : documents) {
                    if (d.getString("password") != null) {
                        doc = d;
                        break;
                    }
                }
                // Fallback to the first document if none have a password
                if (doc == null) {
                    doc = documents.get(0);
                }

                User user = new User();
                String idStr = doc.getString("id");
                if (idStr != null) {
                    user.setId(java.util.UUID.fromString(idStr));
                } else {
                    user.setId(java.util.UUID.fromString(doc.getId()));
                }
                user.setName(doc.getString("name"));
                user.setPhone(doc.getString("phone"));
                user.setEmail(doc.getString("email"));
                user.setCity(doc.getString("city"));
                user.setFoodPref(doc.getString("foodPref"));
                user.setPassword(doc.getString("password"));
                return user;
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to query Firestore: " + e.getMessage());
        }
        return null;
    }

    public User findUserByPhoneInFirestore(String phone) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            var query = db.collection("users").whereEqualTo("phone", phone).get();
            var documents = query.get().getDocuments();
            if (!documents.isEmpty()) {
                // Find a document that has a non-null password
                com.google.cloud.firestore.QueryDocumentSnapshot doc = null;
                for (var d : documents) {
                    if (d.getString("password") != null) {
                        doc = d;
                        break;
                    }
                }
                // Fallback to the first document if none have a password
                if (doc == null) {
                    doc = documents.get(0);
                }

                User user = new User();
                String idStr = doc.getString("id");
                if (idStr != null) {
                    user.setId(java.util.UUID.fromString(idStr));
                } else {
                    user.setId(java.util.UUID.fromString(doc.getId()));
                }
                user.setName(doc.getString("name"));
                user.setPhone(doc.getString("phone"));
                user.setEmail(doc.getString("email"));
                user.setCity(doc.getString("city"));
                user.setFoodPref(doc.getString("foodPref"));
                user.setPassword(doc.getString("password"));
                return user;
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to query Firestore by phone: " + e.getMessage());
        }
        return null;
    }

    public void registerUserInFirebaseAuth(User user) {
        try {
            UserRecord.CreateRequest request = new UserRecord.CreateRequest()
                    .setEmail(user.getEmail())
                    .setPassword(user.getPassword())
                    .setDisplayName(user.getName());
            
            // Firebase Auth requires phone numbers to be in E.164 format (e.g. +11234567890).
            // Let's check if the user's phone starts with '+' and set it if it does.
            String phone = user.getPhone();
            if (phone != null && phone.startsWith("+")) {
                request.setPhoneNumber(phone);
            }

            UserRecord userRecord = FirebaseAuth.getInstance().createUser(request);
            System.out.println("✅ Successfully created new user in Firebase Auth: " + userRecord.getUid());
        } catch (Exception e) {
            System.err.println("⚠️ Could not register user in Firebase Auth (might already exist): " + e.getMessage());
        }
    }

    public void updateUserPasswordInFirebaseAuth(String email, String newPassword) {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
            UserRecord.UpdateRequest request = new UserRecord.UpdateRequest(userRecord.getUid())
                    .setPassword(newPassword);
            FirebaseAuth.getInstance().updateUser(request);
            System.out.println("✅ Successfully updated password in Firebase Auth for: " + email);
        } catch (Exception e) {
            System.err.println("⚠️ Could not update user password in Firebase Auth: " + e.getMessage());
        }
    }

    public boolean authenticateWithFirebaseAuthREST(String email, String password) {
        if (apiKey == null || apiKey.isEmpty()) {
            return false;
        }
        try {
            String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + apiKey;
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("email", email);
            requestBody.put("password", password);
            requestBody.put("returnSecureToken", true);

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            var response = restTemplate.postForEntity(url, requestBody, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                System.out.println("✅ Firebase Auth REST authentication successful for email: " + email);
                return true;
            }
        } catch (Exception e) {
            System.err.println("❌ Firebase Auth REST authentication failed for " + email + ": " + e.getMessage());
        }
        return false;
    }
    public void deleteUserInFirebaseAuth(String email) {
        try {
            UserRecord userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
            FirebaseAuth.getInstance().deleteUser(userRecord.getUid());
            System.out.println("✅ Successfully deleted user from Firebase Auth: " + email);
        } catch (Exception e) {
            System.err.println("⚠️ Could not delete user from Firebase Auth: " + e.getMessage());
        }
    }

    public void deleteUserFromFirestore(String userId) {
        try {
            Firestore db = FirestoreClient.getFirestore();
            ApiFuture<WriteResult> result = db.collection("users").document(userId).delete();
            result.addListener(() -> {
                try {
                    result.get();
                    System.out.println("✅ Successfully deleted user from Firestore: " + userId);
                } catch (Exception e) {
                    System.err.println("❌ Failed to delete from Firestore: " + e.getMessage());
                }
            }, Runnable::run);
        } catch (Exception e) {
            System.err.println("❌ Firebase Firestore delete error: " + e.getMessage());
        }
    }
}
