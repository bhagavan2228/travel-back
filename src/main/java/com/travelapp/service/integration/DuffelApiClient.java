package com.travelapp.service.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class DuffelApiClient {

    @Value("${app.duffel.api-key:}")
    private String apiKey;

    @Value("${app.duffel.base-url:https://api.duffel.com}")
    private String baseUrl;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String authenticatedPost(String path, Object body) {
        if (!isConfigured()) {
            log.warn("Duffel API key not configured — cannot call {}", path);
            return null;
        }

        try {
            return webClientBuilder.build().post()
                    .uri(baseUrl + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .header("Duffel-Version", "v2")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (Exception e) {
            log.error("Duffel POST request failed for path {}: {}", path, e.getMessage());
            return null;
        }
    }
}
