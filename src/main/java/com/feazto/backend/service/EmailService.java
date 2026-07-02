package com.feazto.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailService {

    @Value("${feazto.brevo.sender-name}")
    private String senderName;

    @Value("${feazto.brevo.sender-email}")
    private String senderEmail;

    @Value("${feazto.brevo.api-key}")
    private String brevoApiKey;

    private final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    public void sendOtpEmail(String recipientEmail, String otp) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey); // Authenticate HTTP API

            Map<String, Object> sender = new HashMap<>();
            sender.put("name", senderName);
            sender.put("email", senderEmail);

            Map<String, Object> to = new HashMap<>();
            to.put("email", recipientEmail);

            String htmlContent = "<html>" +
                    "<body style='font-family: Arial, sans-serif; color: #333;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; border: 1px solid #ddd; padding: 20px; border-radius: 8px;'>" +
                    "<h2 style='color: #EAB308; text-align: center;'>Welcome to Feazto Family! 💛</h2>" +
                    "<p>Hello,</p>" +
                    "<p>Thank you for signing up with Feazto! We are excited to have you join our home-cooked food community.</p>" +
                    "<div style='background-color: #FEF9C3; padding: 15px; border-radius: 6px; text-align: center; margin: 20px 0;'>" +
                    "<span style='font-size: 24px; font-weight: bold; letter-spacing: 5px; color: #854D0E;'>" + otp + "</span>" +
                    "</div>" +
                    "<p>Please enter this OTP code into both the Mobile OTP and Email OTP fields in the app to verify your registration.</p>" +
                    "<p>Best regards,<br/>The Feazto Team</p>" +
                    "</div>" +
                    "</body>" +
                    "</html>";

            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", sender);
            payload.put("to", List.of(to));
            payload.put("subject", "Feazto OTP Verification Code: " + otp);
            payload.put("htmlContent", htmlContent);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(BREVO_API_URL, request, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("✅ OTP Email sent successfully to " + recipientEmail + " via Brevo HTTP API");
            } else {
                System.err.println("❌ Brevo HTTP API returned status: " + response.getStatusCode());
            }

        } catch (Exception e) {
            System.err.println("❌ Brevo HTTP API Email Service error for " + recipientEmail + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
}
