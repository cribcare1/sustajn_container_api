package com.notification.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.notification.dto.NotificationRequestNew;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class PushNotificationService {

//    public String sendNotificationToMultipleDevices(NotificationRequestNew request) {
//        try {
//            // Create the MulticastMessage builder
//            MulticastMessage.Builder messageBuilder = MulticastMessage.builder().putData("title", request.getTitle())
//                    .putData("body", request.getBody()).addAllTokens(request.getDeviceTokens());
//
//            // Add additional data to the notification
//            Map<String, String> additionalData = request.getData();
//            if (additionalData != null && !additionalData.isEmpty()) {
//                for (Map.Entry<String, String> entry : additionalData.entrySet()) {
//                    messageBuilder.putData(entry.getKey(), entry.getValue());
//                }
//            }
//
//            // Build the message
//            MulticastMessage message = messageBuilder.build();
//
//            // Send the notification to multiple devices
//            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
//
//            // Process the response
//            int successCount = 0;
//            int failureCount = 0;
//            StringBuilder result = new StringBuilder();
//
//            List<SendResponse> responses = response.getResponses(); // Get the responses from BatchResponse
//
//            for (int i = 0; i < responses.size(); i++) {
//                SendResponse sendResponse = responses.get(i);
//                if (sendResponse.isSuccessful()) {
//                    successCount++;
//                } else {
//                    failureCount++;
//                    result.append("\nDevice Token: ").append(request.getDeviceTokens().get(i)).append(" - Error: ")
//                            .append(sendResponse.getException().getMessage());
//                    System.err.println(sendResponse.getException().getMessage());
//                }
//            }
//
//            result.append("\nNotifications sent successfully to ").append(successCount).append(" devices. Failed for ")
//                    .append(failureCount).append(" devices.");
//
//            return result.toString();
//        } catch (Exception e) {
//            e.printStackTrace();
//            return "Failed to send notifications: " + e.getMessage();
//        }
//    }

    public String sendNotificationToMultipleDevices(NotificationRequestNew request) {

        try {

            // 🔹 Create Notification payload (IMPORTANT for background)
            com.google.firebase.messaging.Notification notification =
                    com.google.firebase.messaging.Notification.builder()
                            .setTitle(request.getTitle())
                            .setBody(request.getBody())
                            .build();

            // 🔹 Create message builder
            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .setNotification(notification)   // ✅ THIS IS THE FIX
                    .addAllTokens(request.getDeviceTokens());

            // 🔹 Add custom data
            if (request.getData() != null && !request.getData().isEmpty()) {
                request.getData().forEach(messageBuilder::putData);
            }

            // 🔹 Build message
            MulticastMessage message = messageBuilder.build();

            // 🔹 Send notification
            BatchResponse response =
                    FirebaseMessaging.getInstance().sendEachForMulticast(message);

            int successCount = 0;
            int failureCount = 0;
            StringBuilder result = new StringBuilder();

            List<SendResponse> responses = response.getResponses();

            for (int i = 0; i < responses.size(); i++) {
                SendResponse sendResponse = responses.get(i);

                if (sendResponse.isSuccessful()) {
                    successCount++;
                } else {
                    failureCount++;
                    result.append("\nToken: ")
                            .append(request.getDeviceTokens().get(i))
                            .append(" Error: ")
                            .append(sendResponse.getException().getMessage());
                }
            }

            result.append("\nSuccess: ").append(successCount)
                    .append(", Failed: ").append(failureCount);

            return result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Failed: " + e.getMessage();
        }
    }

}
