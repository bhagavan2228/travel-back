package com.travelapp.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AmadeusApiClient {

    @Value("${amadeus.api-key:}")
    private String apiKey;

    @Value("${amadeus.api-secret:}")
    private String apiSecret;

    @Value("${amadeus.base-url:https://test.api.amadeus.com}")
    private String baseUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private String accessToken = null;
    private long tokenExpiry = 0;

    public String generateBookingConfirmation() {
        if (apiKey == null || apiKey.isBlank()) {
            return "TA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + " (MOCK)";
        }
        
        try {
            ensureToken();
            // In a real app, we would hit /v1/booking/flight-orders
            // For now, simulating success with the sandbox
            return "AMADEUS-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } catch (Exception e) {
            log.error("Failed to connect to Amadeus sandbox", e);
            return "TA-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() + " (MOCK)";
        }
    }

    private void ensureToken() {
        if (System.currentTimeMillis() < tokenExpiry && accessToken != null) {
            return;
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", apiKey);
        formData.add("client_secret", apiSecret);

        try {
            String response = webClientBuilder.build().post()
                    .uri(baseUrl + "/v1/security/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            this.accessToken = root.path("access_token").asText();
            int expiresIn = root.path("expires_in").asInt(1799);
            this.tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000L) - 5000L;
            log.info("Successfully fetched Amadeus OAuth token.");
        } catch (Exception e) {
            log.error("Failed to fetch Amadeus token", e);
            throw new RuntimeException("Amadeus authentication failed", e);
        }
    }
}
