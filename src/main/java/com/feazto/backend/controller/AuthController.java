package com.feazto.backend.controller;

import com.feazto.backend.model.User;
import com.feazto.backend.repository.UserRepository;
import com.feazto.backend.security.JwtUtil;
import com.feazto.backend.service.FirebaseService;
import com.feazto.backend.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;

@RestController
@RequestMapping("/api/v1/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private FirebaseService firebaseService;

    @Autowired
    private EmailService emailService;

    @org.springframework.beans.factory.annotation.Value("${feazto.firebase.api-key:}")
    private String firebaseApiKey;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String email = body.get("email");

        if (phone == null || phone.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Phone is required");
            return ResponseEntity.badRequest().body(error);
        }

        // 1. Check if phone is already registered in Firestore (fully registered users have a password)
        User firestoreUserByPhone = firebaseService.findUserByPhoneInFirestore(phone);
        if (firestoreUserByPhone != null && firestoreUserByPhone.getPassword() != null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Phone number is already registered. Please login or reset your password.");
            return ResponseEntity.badRequest().body(error);
        }

        // 2. Check if phone is already registered in H2 with a password
        Optional<User> h2UserByPhone = userRepository.findByPhone(phone);
        if (h2UserByPhone.isPresent() && h2UserByPhone.get().getPassword() != null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Phone number is already registered. Please login or reset your password.");
            return ResponseEntity.badRequest().body(error);
        }

        // 3. Check if email is already registered in Firestore (fully registered users have a password)
        if (email != null && !email.isEmpty()) {
            User firestoreUserByEmail = firebaseService.findUserByEmailInFirestore(email);
            if (firestoreUserByEmail != null && firestoreUserByEmail.getPassword() != null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Email address is already registered. Please login or reset your password.");
                return ResponseEntity.badRequest().body(error);
            }

            // 4. Check if email is already registered in H2 with a password
            Optional<User> h2UserByEmail = userRepository.findByEmail(email);
            if (h2UserByEmail.isPresent() && h2UserByEmail.get().getPassword() != null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Email address is already registered. Please login or reset your password.");
                return ResponseEntity.badRequest().body(error);
            }
        }

        // Check if user exists (incomplete registration session) or needs to be loaded/created
        User user;
        if (h2UserByPhone.isPresent()) {
            user = h2UserByPhone.get();
        } else if (firestoreUserByPhone != null) {
            // Restore from Firestore preserving UUID
            user = userRepository.save(firestoreUserByPhone);
        } else {
            user = new User(phone);
        }

        if (email != null && !email.isEmpty()) {
            user.setEmail(email);
        }

        // Generate a random 6-digit OTP code
        String emailOtp = String.format("%06d", new java.util.Random().nextInt(1000000));
        user.setEmailOtp(emailOtp);
        userRepository.save(user);

        System.out.println("🔑 [DEV ONLY] Generated OTP for " + (email != null && !email.isEmpty() ? email : phone) + " is: " + emailOtp);

        // Send OTP Email using Brevo REST API if email is present
        if (email != null && !email.isEmpty()) {
            emailService.sendOtpEmail(email, emailOtp);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP sent");
        response.put("otp", emailOtp);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String phoneOtp = body.get("phoneOtp");
        String emailOtp = body.get("emailOtp");

        // Backward compatibility fallback
        if (phoneOtp == null) {
            phoneOtp = body.get("otp");
        }
        if (emailOtp == null) {
            emailOtp = body.get("otp");
        }

        if (phone == null || phoneOtp == null || emailOtp == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Phone, Phone OTP, and Email OTP are required");
            return ResponseEntity.badRequest().body(error);
        }

        Optional<User> existingUser = userRepository.findByPhone(phone);
        if (existingUser.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        User user = existingUser.get();

        // Verify both inputs against the generated live code stored in database
        String storedOtp = user.getEmailOtp();
        
        // Fallback for H2 reset or mock if storedOtp is null (e.g. mock code 123456)
        if (storedOtp == null) {
            storedOtp = "123456";
        }

        if (!storedOtp.equals(phoneOtp) || !storedOtp.equals(emailOtp)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid verification code. Please check your email.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        String token = jwtUtil.generateToken(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/request-otp")
    public ResponseEntity<?> requestOtp(@RequestBody Map<String, String> body) {
        String contact = body.get("contact");
        String email = body.get("email");

        if (contact == null || contact.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Contact info is required");
            return ResponseEntity.badRequest().body(error);
        }

        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));

        // Find or create a temporary user to store the OTP
        User user = null;
        if (contact.contains("@")) {
            Optional<User> existing = userRepository.findByEmail(contact);
            if (existing.isPresent()) {
                user = existing.get();
            } else {
                user = new User();
                user.setEmail(contact);
                user.setPhone("TEMP_" + contact); // required by DB schema
            }
        } else {
            Optional<User> existing = userRepository.findByPhone(contact);
            if (existing.isPresent()) {
                user = existing.get();
            } else {
                user = new User(contact);
            }
        }
        user.setEmailOtp(otp);
        userRepository.save(user);

        if (contact.contains("@")) {
            emailService.sendOtpEmail(contact, otp);
        } else if (email != null && !email.isEmpty() && email.contains("@")) {
            emailService.sendOtpEmail(email, otp);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP sent");
        // For development fallback if email fails
        response.put("mockOtp", otp); 
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confirm-otp")
    public ResponseEntity<?> confirmOtp(@RequestBody Map<String, String> body) {
        String contact = body.get("contact");
        String otp = body.get("otp");

        if (contact == null || contact.isEmpty() || otp == null || otp.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Contact and OTP are required");
            return ResponseEntity.badRequest().body(error);
        }

        Optional<User> existing = contact.contains("@") ? userRepository.findByEmail(contact) : userRepository.findByPhone(contact);
        if (existing.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        User user = existing.get();
        if (user.getEmailOtp() == null || !user.getEmailOtp().equals(otp)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid OTP");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // Clear OTP after successful verification
        user.setEmailOtp(null);
        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP verified successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String name = body.get("name");
        String email = body.get("email");
        String password = body.get("password");
        String city = body.get("city");
        String foodPref = body.get("foodPref");

        if (phone == null || phone.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Phone is required");
            return ResponseEntity.badRequest().body(error);
        }

        User user;
        Optional<User> existingUser = userRepository.findByPhone(phone);
        if (existingUser.isPresent()) {
            user = existingUser.get();
        } else {
            // Check Firestore by phone to reuse the original ID if they exist there
            User firestoreUser = firebaseService.findUserByPhoneInFirestore(phone);
            if (firestoreUser != null) {
                user = userRepository.save(firestoreUser);
            } else {
                user = new User(phone);
            }
        }

        // Enforce global uniqueness: ensure no OTHER user has this phone number
        User firestoreUserByPhone = firebaseService.findUserByPhoneInFirestore(phone);
        if (firestoreUserByPhone != null && !firestoreUserByPhone.getId().equals(user.getId())) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Phone number is already registered by another account");
            return ResponseEntity.badRequest().body(error);
        }

        // Enforce global uniqueness: ensure no OTHER user has this email
        if (email != null && !email.isEmpty()) {
            // Check Firestore
            User firestoreUserByEmail = firebaseService.findUserByEmailInFirestore(email);
            if (firestoreUserByEmail != null && !firestoreUserByEmail.getId().equals(user.getId())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Email is already registered by another account");
                return ResponseEntity.badRequest().body(error);
            }

            // Check H2
            Optional<User> h2UserByEmail = userRepository.findByEmail(email);
            if (h2UserByEmail.isPresent() && !h2UserByEmail.get().getId().equals(user.getId())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Email is already registered by another account");
                return ResponseEntity.badRequest().body(error);
            }
        }

        user.setName(name);
        user.setEmail(email);
        user.setPassword(password); // Simple storage for MVP, in production use passwordEncoder
        user.setCity(city);
        user.setFoodPref(foodPref);

        User savedUser = userRepository.save(user);

        // Register in Firebase Auth (Admin SDK)
        firebaseService.registerUserInFirebaseAuth(savedUser);

        // Live Firebase Sync
        firebaseService.saveUserToFirebase(savedUser);

        String token = jwtUtil.generateToken(savedUser.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", savedUser);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login-email")
    public ResponseEntity<?> loginEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");

        if (email == null || email.isEmpty() || password == null || password.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email and password are required");
            return ResponseEntity.badRequest().body(error);
        }

        Optional<User> existingUser = userRepository.findByEmail(email);
        User user;
        if (existingUser.isEmpty()) {
            // Fallback to query Firestore
            User firestoreUser = firebaseService.findUserByEmailInFirestore(email);
            if (firestoreUser == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "User not found with this email");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
            // Save user locally in H2 database so we have it
            user = userRepository.save(firestoreUser);
        } else {
            user = existingUser.get();
        }

        // Verify password
        boolean firebaseAuthEnabled = firebaseApiKey != null && !firebaseApiKey.trim().isEmpty();

        if (firebaseAuthEnabled) {
            boolean isFirebaseAuthed = firebaseService.authenticateWithFirebaseAuthREST(email, password);
            if (!isFirebaseAuthed) {
                // If Firebase Auth failed, check if the password matches locally in our database.
                // If it matches locally, it means the user was created before Firebase Auth was enabled,
                // so we should auto-register them in Firebase Auth now!
                if (user.getPassword() == null) {
                    System.err.println("⚠️ Warning: User " + email + " exists in Firestore but has no stored password (created before password storage was implemented). They must register a new account.");
                }

                if (user.getPassword() != null && password.equals(user.getPassword())) {
                    System.out.println("🔄 User exists in database but not Firebase Auth. Auto-registering user: " + email);
                    firebaseService.registerUserInFirebaseAuth(user);
                    // Double check if we can authenticate now
                    isFirebaseAuthed = firebaseService.authenticateWithFirebaseAuthREST(email, password);
                    if (!isFirebaseAuthed) {
                        System.out.println("⚠️ Firebase REST Auth still failed after registration (make sure Email/Password sign-in method is enabled in Firebase Console). Falling back to database verification.");
                    } else {
                        System.out.println("✅ Successfully auto-registered and authenticated user in Firebase Auth!");
                    }
                } else {
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "Firebase Authentication failed. Check your email/password.");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
                }
            }
        } else {
            System.out.println("⚠️ Warning: feazto.firebase.api-key is not configured. Falling back to database password verification.");
            if (user.getPassword() == null || !password.equals(user.getPassword())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid password");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }
        }

        String token = jwtUtil.generateToken(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("user", user);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        String emailOrPhone = body.get("emailOrPhone");
        if (emailOrPhone == null || emailOrPhone.isEmpty()) {
            emailOrPhone = body.get("email"); // fallback
        }

        if (emailOrPhone == null || emailOrPhone.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email or Phone is required");
            return ResponseEntity.badRequest().body(error);
        }

        User user = findUserByEmailOrPhone(emailOrPhone);

        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        String email = user.getEmail();
        if (email == null || email.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No registered email address found for this user");
            return ResponseEntity.badRequest().body(error);
        }

        // Generate random 6-digit OTP code
        String resetOtp = String.format("%06d", new java.util.Random().nextInt(1000000));
        user.setEmailOtp(resetOtp);
        userRepository.save(user);

        // Send OTP email
        emailService.sendOtpEmail(email, resetOtp);

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP sent successfully to " + email);
        response.put("email", email);
        response.put("otp", resetOtp); // Return OTP for dev/testing ease
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<?> verifyResetOtp(@RequestBody Map<String, String> body) {
        String emailOrPhone = body.get("emailOrPhone");
        if (emailOrPhone == null || emailOrPhone.isEmpty()) {
            emailOrPhone = body.get("email");
        }
        String otp = body.get("otp");

        if (emailOrPhone == null || emailOrPhone.isEmpty() || otp == null || otp.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email/Phone and OTP are required");
            return ResponseEntity.badRequest().body(error);
        }

        User user = findUserByEmailOrPhone(emailOrPhone);

        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        String storedOtp = user.getEmailOtp();
        if (storedOtp == null) {
            storedOtp = "123456"; // dev fallback
        }

        if (!storedOtp.equals(otp)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid verification code");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP verified successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String emailOrPhone = body.get("emailOrPhone");
        if (emailOrPhone == null || emailOrPhone.isEmpty()) {
            emailOrPhone = body.get("email");
        }
        String otp = body.get("otp");
        String password = body.get("password");

        if (emailOrPhone == null || emailOrPhone.isEmpty() || otp == null || otp.isEmpty() || password == null || password.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email/Phone, OTP, and password are required");
            return ResponseEntity.badRequest().body(error);
        }

        User user = findUserByEmailOrPhone(emailOrPhone);

        if (user == null) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }

        String storedOtp = user.getEmailOtp();
        if (storedOtp == null) {
            storedOtp = "123456"; // dev fallback
        }

        if (!storedOtp.equals(otp)) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid verification code");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }

        // Update password and clear OTP
        user.setPassword(password);
        user.setEmailOtp(null);
        userRepository.save(user);

        // Sync to Firestore
        firebaseService.saveUserToFirebase(user);

        // Update Firebase Auth if user has email
        if (user.getEmail() != null && !user.getEmail().isEmpty()) {
            firebaseService.updateUserPasswordInFirebaseAuth(user.getEmail(), password);
        }

        Map<String, String> response = new HashMap<>();
        response.put("message", "Password reset successful");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-phone")
    public ResponseEntity<?> checkPhone(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        if (phone == null || phone.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Phone is required");
            return ResponseEntity.badRequest().body(error);
        }

        // Check Firestore
        User firestoreUser = firebaseService.findUserByPhoneInFirestore(phone);
        if (firestoreUser != null && firestoreUser.getPassword() != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("exists", true);
            response.put("error", "An account with this phone number already exists. Please enter another number.");
            return ResponseEntity.ok(response);
        }

        // Check H2
        Optional<User> h2User = userRepository.findByPhone(phone);
        if (h2User.isPresent() && h2User.get().getPassword() != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("exists", true);
            response.put("error", "An account with this phone number already exists. Please enter another number.");
            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("exists", false);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Email is required");
            return ResponseEntity.badRequest().body(error);
        }

        // Check Firestore
        User firestoreUser = firebaseService.findUserByEmailInFirestore(email);
        if (firestoreUser != null && firestoreUser.getPassword() != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("exists", true);
            response.put("error", "An account with this email address already exists. Please enter another email.");
            return ResponseEntity.ok(response);
        }

        // Check H2
        Optional<User> h2User = userRepository.findByEmail(email);
        if (h2User.isPresent() && h2User.get().getPassword() != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("exists", true);
            response.put("error", "An account with this email address already exists. Please enter another email.");
            return ResponseEntity.ok(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("exists", false);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleSignIn(@RequestBody Map<String, String> body) {
        String idTokenString = body.get("idToken");
        if (idTokenString == null || idTokenString.isEmpty()) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "ID Token is required");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList("689731840975-gp7sv7toikbqj76pel72vag4e6vtqaed.apps.googleusercontent.com"))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Invalid ID Token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();


            // Only allow login — do NOT create new accounts via Google
            Optional<User> existingUser = userRepository.findByEmail(email);
            User user;
            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                // Check Firestore by email
                User firestoreUser = firebaseService.findUserByEmailInFirestore(email);
                if (firestoreUser != null) {
                    user = userRepository.save(firestoreUser);
                } else {
                    // Email not registered — reject login
                    Map<String, String> error = new HashMap<>();
                    error.put("error", "No account found with this Google email. Please sign up first.");
                    error.put("code", "NOT_REGISTERED");
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
                }
            }

            String token = jwtUtil.generateToken(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("user", user);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("error", "Token verification failed: " + e.toString());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private User findUserByEmailOrPhone(String emailOrPhone) {
        if (emailOrPhone == null || emailOrPhone.isEmpty()) {
            return null;
        }
        User user = null;
        if (emailOrPhone.contains("@")) {
            Optional<User> existingUser = userRepository.findByEmail(emailOrPhone);
            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                User firestoreUser = firebaseService.findUserByEmailInFirestore(emailOrPhone);
                if (firestoreUser != null) {
                    user = userRepository.save(firestoreUser);
                }
            }
        } else {
            Optional<User> existingUser = userRepository.findByPhone(emailOrPhone);
            if (existingUser.isPresent()) {
                user = existingUser.get();
            } else {
                User firestoreUser = firebaseService.findUserByPhoneInFirestore(emailOrPhone);
                if (firestoreUser != null) {
                    user = userRepository.save(firestoreUser);
                }
            }
        }
        return user;
    }
}
