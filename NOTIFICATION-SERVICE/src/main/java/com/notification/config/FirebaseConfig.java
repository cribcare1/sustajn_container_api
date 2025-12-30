package com.notification.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

@Configuration
public class FirebaseConfig {
    public FirebaseConfig() throws IOException {
        // Load the file from resources folder
        InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("container.json");

        if (serviceAccount == null) {
            throw new FileNotFoundException("Firebase configuration file not found");
        }

        FirebaseOptions options = FirebaseOptions.builder().setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
        System.err.println(FirebaseApp.getApps());

        if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options);
        }
    }
}
