package com.feazto.backend.service;

import org.springframework.stereotype.Service;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import java.util.Map;

@Service
public class PushNotificationService {

    public void sendPushNotification(String fcmToken, String title, String body, Map<String, Object> data) {
        if (fcmToken == null || fcmToken.isEmpty()) {
            System.out.println("No push token provided. Skipping push notification.");
            return;
        }

        try {
            Message.Builder messageBuilder = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build());
                    
            if (data != null) {
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    messageBuilder.putData(entry.getKey(), entry.getValue().toString());
                }
            }

            Message message = messageBuilder.build();
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Push notification sent! Firebase response: " + response);
        } catch (Exception e) {
            System.err.println("Failed to send Firebase push notification: " + e.getMessage());
        }
    }
}
