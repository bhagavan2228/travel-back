package com.travelapp.service.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Map;

@Service
@Slf4j
public class DuffelCarApiClient {

    private final WebClient webClient;
    private final String apiKey;

    public DuffelCarApiClient(
            @Value("${app.duffel.base-url:https://api.duffel.com}") String baseUrl,
            @Value("${app.duffel.cars-api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Duffel-Version", "v2")
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("default-key");
    }

    public String searchCars(String location, String pickupDate, String dropoffDate) {
        if (!isConfigured()) return null;

        // Duffel Car search format (mocking the structure from standard Duffel API conventions)
        Map<String, Object> body = Map.of(
            "data", Map.of(
                "pickup_location", Map.of("type", "airport", "id", "arp_" + location.toLowerCase() + "_gb"),
                "drop_off_location", Map.of("type", "airport", "id", "arp_" + location.toLowerCase() + "_gb"),
                "pickup_time", pickupDate + "T10:00:00Z",
                "drop_off_time", dropoffDate + "T10:00:00Z",
                "driver_age", 30
            )
        );

        try {
            return webClient.post()
                    .uri("/cars/search")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            log.warn("Duffel Cars API Error ({}): {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null; // Let the service fallback to mock data
        } catch (Exception e) {
            log.warn("Failed to connect to Duffel Cars API: {}", e.getMessage());
            return null;
        }
    }
}
