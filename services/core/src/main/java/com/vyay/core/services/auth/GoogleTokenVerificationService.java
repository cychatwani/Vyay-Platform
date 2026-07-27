package com.vyay.core.services.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
@Service
public class GoogleTokenVerificationService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerificationService(
            @Value("${google.oauth.client-id}") String clientId
    ) throws GeneralSecurityException, IOException {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JSON_FACTORY
        )
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleIdToken.Payload verifyToken(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new IllegalArgumentException("Invalid ID token.");
            }
            return idToken.getPayload();
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to verify Google token", e);
        }
    }
}
