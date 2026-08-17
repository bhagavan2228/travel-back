package com.travelapp.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class HotelbedsApiClient {

    @Value("${app.hotelbeds.api-key:}")
    private String apiKey;

    @Value("${app.hotelbeds.api-secret:}")
    private String apiSecret;

    @Value("${app.hotelbeds.base-url:https://api.test.hotelbeds.com/hotel-api/1.0}")
    private String baseUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && apiSecret != null && !apiSecret.isBlank();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    private String generateSignature() {
        try {
            long timestamp = Instant.now().getEpochSecond();
            String raw = apiKey + apiSecret + timestamp;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Hotelbeds signature", e);
        }
    }

    public String authenticatedPost(String path, Object body) {
        if (!isConfigured()) {
            log.warn("Hotelbeds API keys not configured — cannot call {}", path);
            return null;
        }

        try {
            return webClientBuilder.build().post()
                    .uri(baseUrl + path)
                    .header("Api-key", apiKey)
                    .header("X-Signature", generateSignature())
                    .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            log.error("Hotelbeds POST request failed for path {}: {}", path, e.getMessage());
            log.error("Response body: {}", e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Hotelbeds POST request failed for path {}: {}", path, e.getMessage());
            return null;
        }
    }
}
