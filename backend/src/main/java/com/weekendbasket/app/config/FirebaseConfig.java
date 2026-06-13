package com.weekendbasket.app.config;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LogManager.getLogger(FirebaseConfig.class);
    
    @Value("${firebase.credentials.json:}")
    private String firebaseCredentialsJson;
    

    @PostConstruct
    public void initialize() {
    	try {
            InputStream serviceAccount;

            if (firebaseCredentialsJson == null || firebaseCredentialsJson.isEmpty()) {
                // FALLBACK FOR LOCAL DEVELOPMENT: Look for your local file
                serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
            } else {
                // PRODUCTION: Read directly from the cloud environment variable string
                serviceAccount = new ByteArrayInputStream(firebaseCredentialsJson.getBytes(StandardCharsets.UTF_8));
            }

            if (serviceAccount == null) {
                throw new IllegalStateException("Firebase credentials file or environment variable is missing!");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Admin SDK initialized successfully.");
            }
        } catch (Exception e) {
            System.err.println("Firebase Admin SDK initialization failed: " + e.getMessage());
        }
    }
}