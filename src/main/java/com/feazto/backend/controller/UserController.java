package com.feazto.backend.controller;

import com.feazto.backend.model.User;
import com.feazto.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import com.feazto.backend.service.FirebaseService;
import com.feazto.backend.service.PushNotificationService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FirebaseService firebaseService;

    private User getAuthenticatedUser() {
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findById(UUID.fromString(userIdStr)).orElseThrow(
                () -> new RuntimeException("Authenticated user not found")
        );
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMe() {
        try {
            User user = getAuthenticatedUser();
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(401).body(error);
        }
    }

    @PutMapping("/me")
    public ResponseEntity<?> updateMe(@RequestBody Map<String, String> updates) {
        try {
            User user = getAuthenticatedUser();
            if (updates.containsKey("name")) {
                user.setName(updates.get("name"));
            }
            if (updates.containsKey("city")) {
                user.setCity(updates.get("city"));
            }
            if (updates.containsKey("username")) {
                user.setUsername(updates.get("username"));
            }
            if (updates.containsKey("photo")) {
                user.setPhoto(updates.get("photo"));
            }
            
            userRepository.save(user);
            firebaseService.saveUserToFirebase(user);
            
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

    @Autowired
    private PushNotificationService pushNotificationService;

    @PutMapping("/me/push-token")
    public ResponseEntity<?> updatePushToken(@RequestBody Map<String, String> body) {
        try {
            User user = getAuthenticatedUser();
            if (body.containsKey("pushToken")) {
                user.setPushToken(body.get("pushToken"));
                userRepository.save(user);
            }
            Map<String, String> response = new HashMap<>();
            response.put("message", "Push token updated successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }

    @PostMapping("/me/test-push")
    public ResponseEntity<?> testPushNotification() {
        try {
            User user = getAuthenticatedUser();
            if (user.getPushToken() == null || user.getPushToken().isEmpty()) {
                throw new RuntimeException("No push token found for user");
            }
            
            Map<String, Object> data = new HashMap<>();
            data.put("screen", "home");
            
            new Thread(() -> {
                try {
                    Thread.sleep(6000);
                    pushNotificationService.sendPushNotification(
                        user.getPushToken(), 
                        "Feazto Notification Test 🚀", 
                        "Your background push notifications are working perfectly!",
                        data
                    );
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Test push sent!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe() {
        try {
            User user = getAuthenticatedUser();
            
            // Delete from Firebase services
            firebaseService.deleteUserInFirebaseAuth(user.getEmail());
            firebaseService.deleteUserFromFirestore(user.getId().toString());
            
            // Delete from local H2 database
            userRepository.delete(user);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "User account deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        }
    }
}
