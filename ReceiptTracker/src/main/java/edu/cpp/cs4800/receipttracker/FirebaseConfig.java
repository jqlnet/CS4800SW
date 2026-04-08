package edu.cpp.cs4800.receipttracker;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initFirebase() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            String firebaseJson = """
                {
                  "type": "service_account",
                  "project_id": "receipttracker-cs4800",
                  "private_key_id": "placeholder",
                  "private_key": "placeholder",
                  "client_email": "placeholder",
                  "client_id": "placeholder",
                  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                  "token_uri": "https://oauth2.googleapis.com/token"
                }
                """;

            // We use the project ID approach for token verification only
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            new ByteArrayInputStream(firebaseJson.getBytes())))
                    .setProjectId("receipttracker-cs4800")
                    .build();

            FirebaseApp.initializeApp(options);
        }
    }
}